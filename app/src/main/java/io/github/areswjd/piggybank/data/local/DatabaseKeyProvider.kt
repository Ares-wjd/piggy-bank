package io.github.areswjd.piggybank.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * SQLCipher DB 비밀번호를 관리한다.
 *
 * 비밀번호는 기기마다 무작위로 만들고, Android Keystore의 AES 키로 암호화해서 앱 전용 SharedPreferences에 둔다.
 * Keystore 키는 기기 밖으로 꺼낼 수 없으므로, 앱 데이터 파일만 복사해 가서는 DB를 열 수 없다.
 */
class DatabaseKeyProvider(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 저장된 비밀번호를 돌려준다. 없거나 복호화할 수 없으면 새로 만든다.
     * 새로 만들 때는 기존 DB를 열 방법이 없으므로 [databaseName] 파일을 지운다 — 데이터는 드라이브 백업에서 복원한다.
     */
    fun getOrCreatePassphrase(databaseName: String): ByteArray {
        readPassphrase()?.let { return it }

        context.deleteDatabase(databaseName)
        val passphrase = newPassphrase()
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        }
        val encrypted = cipher.doFinal(passphrase)
        prefs.edit()
            .putString(KEY_PASSPHRASE, encrypted.toBase64())
            .putString(KEY_IV, cipher.iv.toBase64())
            .commit()
        return passphrase
    }

    private fun readPassphrase(): ByteArray? {
        val encrypted = prefs.getString(KEY_PASSPHRASE, null) ?: return null
        val iv = prefs.getString(KEY_IV, null) ?: return null
        return try {
            val key = loadSecretKey() ?: return null
            Cipher.getInstance(TRANSFORMATION).run {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv.fromBase64()))
                doFinal(encrypted.fromBase64())
            }
        } catch (e: GeneralSecurityException) {
            // Keystore 키가 무효화된 경우(드물다). 키를 지우고 새로 시작한다.
            deleteSecretKey()
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** 32바이트 난수를 16진수 문자열로 만든다(SQLCipher에 그대로 넘겨도 안전한 문자만 쓰도록). */
    private fun newPassphrase(): ByteArray {
        val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return random.joinToString("") { "%02x".format(it) }.toByteArray(Charsets.US_ASCII)
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun loadSecretKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun deleteSecretKey() {
        runCatching { keyStore().deleteEntry(KEY_ALIAS) }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        loadSecretKey()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "piggybank_db_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val PREFS_NAME = "db_key"
        const val KEY_PASSPHRASE = "passphrase"
        const val KEY_IV = "iv"
    }
}
