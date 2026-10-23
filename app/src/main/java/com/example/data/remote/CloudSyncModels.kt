package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val deviceModel: String = "",
    val activeBackupId: String? = null,
    val tileCount: Long = 0L,
    val pageCount: Long = 1L,
    val accentColorHex: String = "#0078D7",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudBackup(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val deviceName: String = "",
    val backupPayloadJson: String = "{}",
    val tileCount: Long = 0L,
    val pageCount: Long = 1L,
    val accentColorHex: String = "#0078D7",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
