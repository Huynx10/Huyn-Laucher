package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.remote.CloudBackup
import com.example.data.remote.OperationType
import com.example.data.remote.UserProfile
import com.example.data.remote.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CloudSyncRepository(
    private val db: FirebaseFirestore
) {
    // Convenience constructor resolving the named database ID from firebase_applet_config.xml
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
    }

    fun observeCloudBackups(userId: String): Flow<List<CloudBackup>> {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return db.collection("users").document(userId)
            .collection("backups")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    CloudBackup::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }
    }

    suspend fun upsertUserProfile(
        userId: String,
        displayName: String,
        deviceModel: String,
        activeBackupId: String? = null,
        tileCount: Int,
        pageCount: Int,
        accentColorHex: String
    ): Result<Unit> = runCatching {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val docRef = db.collection("users").document(userId)
        val safeName = displayName.trim().ifBlank { "Người dùng Lumia" }.take(100)
        val safeDevice = deviceModel.trim().ifBlank { "Android Lumia" }.take(100)
        val safeTiles = tileCount.coerceIn(0, 500).toLong()
        val safePages = pageCount.coerceIn(1, 50).toLong()
        val safeAccent = accentColorHex.trim().ifBlank { "#0078D7" }.take(16)

        try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to userId,
                    "displayName" to safeName,
                    "deviceModel" to safeDevice,
                    "tileCount" to safeTiles,
                    "pageCount" to safePages,
                    "accentColorHex" to safeAccent,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!activeBackupId.isNullOrBlank()) {
                    createPayload["activeBackupId"] = activeBackupId.take(128)
                }
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "deviceModel" to safeDevice,
                    "tileCount" to safeTiles,
                    "pageCount" to safePages,
                    "accentColorHex" to safeAccent,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!activeBackupId.isNullOrBlank()) {
                    updatePayload["activeBackupId"] = activeBackupId.take(128)
                }
                docRef.update(updatePayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun createCloudBackup(
        userId: String,
        title: String,
        deviceName: String,
        backupPayloadJson: String,
        tileCount: Int,
        pageCount: Int,
        accentColorHex: String
    ): Result<String> = runCatching {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val backupId = "backup_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("users").document(userId)
            .collection("backups").document(backupId)

        val payload = mapOf<String, Any>(
            "userId" to userId,
            "title" to title.trim().ifBlank { "Bản sao lưu Lumia" }.take(120),
            "deviceName" to deviceName.trim().ifBlank { "Thiết bị Android" }.take(100),
            "backupPayloadJson" to backupPayloadJson.ifBlank { "{}" }.take(200000),
            "tileCount" to tileCount.coerceIn(0, 500).toLong(),
            "pageCount" to pageCount.coerceIn(1, 50).toLong(),
            "accentColorHex" to accentColorHex.trim().ifBlank { "#0078D7" }.take(16),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            backupId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getUserBackups(userId: String): Result<List<CloudBackup>> = runCatching {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val colRef = db.collection("users").document(userId).collection("backups")
        try {
            val snapshot = colRef.whereEqualTo("userId", userId).get(Source.SERVER).await()
            snapshot.toObjects(
                CloudBackup::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun getBackupById(userId: String, backupId: String): Result<CloudBackup?> = runCatching {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(backupId.isNotBlank()) { "backupId must not be blank" }
        val docRef = db.collection("users").document(userId)
            .collection("backups").document(backupId)
        try {
            val snap = docRef.get(Source.SERVER).await()
            if (snap.exists()) {
                snap.toObject(CloudBackup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun deleteCloudBackup(userId: String, backupId: String): Result<Unit> = runCatching {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(backupId.isNotBlank()) { "backupId must not be blank" }
        val docRef = db.collection("users").document(userId)
            .collection("backups").document(backupId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
