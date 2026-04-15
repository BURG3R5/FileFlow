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

object Crypto {
    private lateinit var aead: Aead

    fun init(context: Context) {
        TinkConfig.register()
        AeadConfig.register()

        aead = AndroidKeysetManager.Builder()
            .withSharedPref(context, "fileflow_keyset", "fileflow_secure_prefs")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://fileflow_master_key")
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    fun encrypt(text: String?) =
        text?.let {
            Base64.encodeToString(
                aead.encrypt(
                    text.toByteArray(StandardCharsets.UTF_8),
                    null,
                ),
                Base64.DEFAULT,
            )
        }

    fun decrypt(text: String?) =
        text?.let {
            aead.decrypt(
                Base64.decode(text, Base64.DEFAULT),
                null,
            ).toString(StandardCharsets.UTF_8)
        }
}
