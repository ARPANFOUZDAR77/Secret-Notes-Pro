package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.security.EncryptionManager

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titleEncrypted: String,
    val contentEncrypted: String,
    val createdDate: Long = System.currentTimeMillis(),
    val modifiedDate: Long = System.currentTimeMillis(),
    val colorCategory: Int = 0,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false
) {
    val title: String get() = EncryptionManager.decrypt(titleEncrypted)
    val content: String get() = EncryptionManager.decrypt(contentEncrypted)

    companion object {
        fun create(
            title: String,
            content: String,
            colorCategory: Int = 0,
            isFavorite: Boolean = false,
            isLocked: Boolean = false
        ): Note {
            return Note(
                titleEncrypted = EncryptionManager.encrypt(title),
                contentEncrypted = EncryptionManager.encrypt(content),
                colorCategory = colorCategory,
                isFavorite = isFavorite,
                isLocked = isLocked
            )
        }
    }
    
    fun update(
        newTitle: String,
        newContent: String,
        newColorCategory: Int = colorCategory,
        newIsFavorite: Boolean = isFavorite,
        newIsLocked: Boolean = isLocked
    ): Note {
        return this.copy(
            titleEncrypted = EncryptionManager.encrypt(newTitle),
            contentEncrypted = EncryptionManager.encrypt(newContent),
            modifiedDate = System.currentTimeMillis(),
            colorCategory = newColorCategory,
            isFavorite = newIsFavorite,
            isLocked = newIsLocked
        )
    }
}
