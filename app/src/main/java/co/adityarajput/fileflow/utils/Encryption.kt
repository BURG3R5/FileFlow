package co.adityarajput.fileflow.utils

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.KeyStore

object Crypto {
    private const val MASTER_KEY_ALIAS = "fileflow_master_key"
    private const val KEYSET_PREFS = "fileflow_secure_prefs"
    private const val KEYSET_NAME = "fileflow_keyset"

    private lateinit var aead: Aead

    fun init(context: Context) {
        TinkConfig.register()
        AeadConfig.register()

        aead = try {
            buildAead(context)
        } catch (e: InvalidKeyException) {
            Logger.w("Crypto", "Failed to load crypto keyset, attempting recovery", e)
            resetKeyMaterial(context)
            buildAead(context)
        }
    }

    private fun buildAead(context: Context) =
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://$MASTER_KEY_ALIAS")
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)

    private fun resetKeyMaterial(context: Context) {
        try {
            context.deleteSharedPreferences(KEYSET_PREFS)
        } catch (e: Exception) {
            Logger.w("Crypto", "Unable to delete crypto shared prefs", e)
        }

        try {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .deleteEntry(MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            Logger.w("Crypto", "Unable to delete stale keystore alias", e)
        }
    }

    fun encrypt(text: String?) =
        text?.let {
            try {
                Base64.encodeToString(
                    aead.encrypt(
                        text.toByteArray(StandardCharsets.UTF_8),
                        null,
                    ),
                    Base64.DEFAULT,
                )
            } catch (e: Exception) {
                Logger.w("Crypto", "Failed to decrypt text", e)
                null
            }
        }

    fun decrypt(text: String?) =
        text?.let {
            try {
                aead.decrypt(
                    Base64.decode(text, Base64.DEFAULT),
                    null,
                ).toString(StandardCharsets.UTF_8)
            } catch (e: Exception) {
                Logger.w("Crypto", "Failed to decrypt text", e)
                null
            }
        }
}
