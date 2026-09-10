package com.m4.red_android.auth

import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EncryptedSessionFileStoreTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private lateinit var sessionFile: File
    private lateinit var keys: InMemoryKeyProvider

    @Before fun setUp() {
        sessionFile = File(temporaryFolder.root, KeystoreSecureTokenStore.SESSION_FILE)
        keys = InMemoryKeyProvider()
    }

    @Test fun `round trip preserves session without storing token plaintext`() = runTest {
        val store = EncryptedSessionFileStore(sessionFile, keys)
        val session = AuthenticatedSession(
            accessToken = "highly-sensitive-token",
            expiresAtEpochSeconds = 9_999,
            user = SessionUser("Maria", "ADMIN", "company-1"),
        )

        store.write(session)

        assertEquals(session, store.read())
        assertFalse(sessionFile.readBytes().toString(Charsets.UTF_8).contains(session.accessToken))
        assertEquals(1, keys.creationCount)
    }

    @Test fun `missing encrypted file returns no session without creating key`() = runTest {
        val store = EncryptedSessionFileStore(sessionFile, keys)

        assertNull(store.read())
        assertEquals(0, keys.creationCount)
    }

    @Test fun `new session atomically replaces previous encrypted session`() = runTest {
        val store = EncryptedSessionFileStore(sessionFile, keys)
        store.write(AuthenticatedSession("old", 2_000))

        store.write(AuthenticatedSession("new", 3_000))

        assertEquals(AuthenticatedSession("new", 3_000), store.read())
        assertFalse(File(sessionFile.parentFile, "${sessionFile.name}.tmp").exists())
    }

    @Test fun `tampered ciphertext fails authentication`() = runTest {
        val store = EncryptedSessionFileStore(sessionFile, keys)
        store.write(AuthenticatedSession("secret", 9_999))
        sessionFile.writeBytes(sessionFile.readBytes().also { bytes ->
            bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        })

        val failure = runCatching { store.read() }.exceptionOrNull()

        assertTrue(failure != null)
    }

    @Test fun `invalidated or replaced key cannot decrypt persisted session`() = runTest {
        val store = EncryptedSessionFileStore(sessionFile, keys)
        store.write(AuthenticatedSession("secret", 9_999))
        keys.replace()

        val failure = runCatching { store.read() }.exceptionOrNull()

        assertTrue(failure != null)
    }

    @Test fun `clear removes encrypted material and its key`() = runTest {
        val store = EncryptedSessionFileStore(sessionFile, keys)
        store.write(AuthenticatedSession("secret", 9_999))

        store.clear()

        assertFalse(sessionFile.exists())
        assertEquals(1, keys.deletionCount)
        assertNull(store.read())
    }
}

private class InMemoryKeyProvider : SessionEncryptionKeyProvider {
    private var key: SecretKey? = null
    var creationCount = 0
        private set
    var deletionCount = 0
        private set

    override fun getOrCreate(): SecretKey = key ?: newKey().also {
        key = it
        creationCount++
    }

    override fun delete() {
        key = null
        deletionCount++
    }

    fun replace() { key = newKey() }

    private fun newKey(): SecretKey = KeyGenerator.getInstance("AES").run {
        init(256)
        generateKey()
    }
}
