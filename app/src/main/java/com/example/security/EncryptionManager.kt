package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object EncryptionManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "secret_notes_pro_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private var aadBytes: ByteArray? = null

    init {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    fun init(context: android.content.Context) {
        val signature = getAppSignatureSHA256(context)
        val packageAndSignature = context.packageName + signature
        aadBytes = packageAndSignature.toByteArray(Charsets.UTF_8)
    }

    private fun getAppSignatureSHA256(context: android.content.Context): String {
        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            }
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            if (signatures != null && signatures.isNotEmpty()) {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                val signatureBytes = signatures[0].toByteArray()
                val digest = md.digest(signatureBytes)
                digest.joinToString(":") { String.format("%02X", it) }
            } else {
                "DEFAULT_SIGNATURE"
            }
        } catch (e: Exception) {
            "DEFAULT_SIGNATURE"
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        aadBytes?.let { cipher.updateAAD(it) }
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        val ivString = Base64.encodeToString(iv, Base64.DEFAULT)
        val dataString = Base64.encodeToString(encryptedData, Base64.DEFAULT)
        return "$ivString:$dataString"
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        try {
            val parts = encryptedText.split(":")
            if (parts.size != 2) return encryptedText // not encrypted or corrupted
            
            val iv = Base64.decode(parts[0], Base64.DEFAULT)
            val encryptedData = Base64.decode(parts[1], Base64.DEFAULT)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            aadBytes?.let { cipher.updateAAD(it) }
            
            val decryptedData = cipher.doFinal(encryptedData)
            return String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            return "Decryption Error"
        }
    }

    fun getEncryptedPreview(text: String): String {
        if (text.isEmpty()) return ""
        return try {
            val encrypted = encrypt(text)
            val parts = encrypted.split(":")
            if (parts.size == 2) {
                parts[1].replace("\n", "").trim().take(40) + "..."
            } else {
                "..."
            }
        } catch (e: Exception) {
            "..."
        }
    }
}
