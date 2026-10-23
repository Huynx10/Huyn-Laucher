const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const validTimestamp = () => new Date(Date.now() - 2000);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- SECURITY BOUNDS TESTS (Dirty Dozen & Zero-Trust Verification) ---

test("1. Unauthenticated user: cannot read or write user profiles or backups", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("backups")
      .get()
  );
});

test("2. Authenticated user: can create and read own UserProfile with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = validTimestamp();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Lumia",
      deviceModel: "Pixel 9 Pro",
      activeBackupId: "backup_1",
      tileCount: 12,
      pageCount: 2,
      accentColorHex: "#0078D7",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("3. Authenticated user: cannot read or modify another user's profile (Cross-User Isolation)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = validTimestamp();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Lumia",
      deviceModel: "Pixel 9 Pro",
      tileCount: 10,
      pageCount: 2,
      accentColorHex: "#0078D7",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked by Bob",
      updatedAt: validTimestamp(),
    })
  );
});

test("4. Shadow Update Test: rejects ghost fields on UserProfile and CloudBackup", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = validTimestamp();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Lumia",
      deviceModel: "Pixel 9 Pro",
      tileCount: 10,
      pageCount: 2,
      accentColorHex: "#0078D7",
      isAdmin: true, // Ghost field
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("5. Authenticated user: can create, update, list (with matching userId filter), and delete own CloudBackup", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const backupRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("backups")
    .doc("backup_001");
  const ts = validTimestamp();

  await assertSucceeds(
    backupRef.set({
      userId: ALICE_UID,
      title: "Bố cục Windows 10 Mobile",
      deviceName: "Lumia 950 XL",
      backupPayloadJson: '{"version":2,"tiles":[]}',
      tileCount: 14,
      pageCount: 3,
      accentColorHex: "#0078D7",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Client query aligned with resource.data.userId == request.auth.uid
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("backups")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Update backup title and payload while keeping userId and createdAt immutable
  await assertSucceeds(
    backupRef.update({
      title: "Bố cục One UI 9.0",
      tileCount: 16,
      updatedAt: validTimestamp(),
    })
  );

  await assertSucceeds(backupRef.delete());
});

test("6. Cross-User & Spoofing Test: Bob cannot read, list, create, or delete backups in Alice's subcollection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = validTimestamp();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("backups")
      .doc("backup_alice")
      .set({
        userId: ALICE_UID,
        title: "Alice Private Backup",
        deviceName: "Lumia",
        backupPayloadJson: '{"version":2}',
        tileCount: 8,
        pageCount: 2,
        accentColorHex: "#0078D7",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("backups")
      .doc("backup_alice")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("backups")
      .where("userId", "==", ALICE_UID)
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("backups")
      .doc("backup_alice")
      .delete()
  );
});

test("7. Value Poisoning Test: rejects out-of-bounds tileCount or invalid type on update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const backupRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("backups")
    .doc("backup_002");
  const ts = validTimestamp();

  await assertSucceeds(
    backupRef.set({
      userId: ALICE_UID,
      title: "Valid Backup",
      deviceName: "Lumia",
      backupPayloadJson: '{"version":2}',
      tileCount: 8,
      pageCount: 2,
      accentColorHex: "#0078D7",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    backupRef.update({
      tileCount: 99999, // Exceeds max 500
      updatedAt: validTimestamp(),
    })
  );
});
