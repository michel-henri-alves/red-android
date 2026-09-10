package com.m4.red_android.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.gson.Gson
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreSecureTokenStore(
    context: Context,
) : SecureTokenStore by EncryptedSessionFileStore(
    sessionFile = File(context.noBackupFilesDir, SESSION_FILE),
    keyProvider = AndroidKeystoreSessionKeyProvider(),
) {
    companion object {
        internal const val SESSION_FILE = "authenticated_session.v1"
    }
}

internal class EncryptedSessionFileStore(
    private val sessionFile: File,
    private val keyProvider: SessionEncryptionKeyProvider,
    private val gson: Gson = Gson(),
) : SecureTokenStore {
    override suspend fun read(): AuthenticatedSession? {
        if (!sessionFile.exists()) return null
        DataInputStream(FileInputStream(sessionFile)).use { input ->
            require(input.readUnsignedByte() == FORMAT_VERSION) { "Unsupported session format" }
            val ivLength = input.readUnsignedByte()
            require(ivLength in 12..16) { "Invalid session IV" }
            val iv = ByteArray(ivLength).also(input::readFully)
            val ciphertext = input.readBytes()
            require(ciphertext.size >= GCM_TAG_BYTES) { "Invalid session ciphertext" }
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, keyProvider.getOrCreate(), GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            return gson.fromJson(String(cipher.doFinal(ciphertext), Charsets.UTF_8), AuthenticatedSession::class.java)
                ?: error("Missing session payload")
        }
    }

    override suspend fun write(session: AuthenticatedSession) {
        sessionFile.parentFile?.mkdirs()
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, keyProvider.getOrCreate())
        }
        val ciphertext = cipher.doFinal(gson.toJson(session).toByteArray(Charsets.UTF_8))
        val temporary = File(sessionFile.parentFile, "${sessionFile.name}.tmp")
        try {
            FileOutputStream(temporary).use { fileOutput ->
                DataOutputStream(fileOutput).use { output ->
                    output.writeByte(FORMAT_VERSION)
                    output.writeByte(cipher.iv.size)
                    output.write(cipher.iv)
                    output.write(ciphertext)
                    output.flush()
                    fileOutput.fd.sync()
                }
            }
            Files.move(
                temporary.toPath(),
                sessionFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    override suspend fun clear() {
        val fileCleared = !sessionFile.exists() || sessionFile.delete()
        runCatching { keyProvider.delete() }.getOrElse { keyFailure ->
            if (!fileCleared) keyFailure.addSuppressed(
                IllegalStateException("Could not clear encrypted session"),
            )
            throw keyFailure
        }
        check(fileCleared) { "Could not clear encrypted session" }
    }

    companion object {
        private const val FORMAT_VERSION = 1
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

internal interface SessionEncryptionKeyProvider {
    fun getOrCreate(): SecretKey
    fun delete()
}

internal class AndroidKeystoreSessionKeyProvider(
    private val alias: String = KEY_ALIAS,
) : SessionEncryptionKeyProvider {
    override fun getOrCreate(): SecretKey {
        val keyStore = keyStore()
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    override fun delete() {
        keyStore().let { if (it.containsAlias(alias)) it.deleteEntry(alias) }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "red.authenticated-session.v1"
    }
}
