package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveCloudBackup_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertUserProfile(
                userId = aliceUid,
                displayName = "Alice Lumia",
                deviceModel = "Lumia 950 XL",
                tileCount = 12,
                pageCount = 2,
                accentColorHex = "#0078D7"
            )
        }
        assertTrue(profileResult.isSuccess)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCloudBackup(
                userId = aliceUid,
                title = "Bản sao lưu W10M",
                deviceName = "Lumia 950 XL",
                backupPayloadJson = """{"version":2,"tiles":[]}""",
                tileCount = 12,
                pageCount = 2,
                accentColorHex = "#0078D7"
            )
        }
        assertTrue(createResult.isSuccess)
        val backupId = createResult.getOrThrow()
        assertTrue(backupId.isNotEmpty())

        val emittedBackups = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCloudBackups(aliceUid).first { list -> list.any { it.id == backupId } }
        }
        assertTrue(emittedBackups.any { it.id == backupId && it.title == "Bản sao lưu W10M" })
    }

    @Test
    fun getBackupById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudSyncRepository(firestore)
        val backupId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCloudBackup(
                userId = aliceUid,
                title = "Alice Private Backup",
                deviceName = "Lumia",
                backupPayloadJson = """{"version":2}""",
                tileCount = 8,
                pageCount = 2,
                accentColorHex = "#0078D7"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudSyncRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getBackupById(aliceUid, backupId).getOrThrow()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeCloudBackups_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = CloudSyncRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCloudBackups(aliceUid).first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for unauthenticated user")
        } catch (e: Exception) {
            val firestoreEx = generateSequence<Throwable>(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            assertNotNull("Expected FirebaseFirestoreException in cause chain, got: $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
