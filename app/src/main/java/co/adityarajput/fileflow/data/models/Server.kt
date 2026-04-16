package co.adityarajput.fileflow.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import co.adityarajput.fileflow.utils.Crypto
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "servers")
data class Server(
    val host: String,

    val port: Int,

    val username: String,

    val encryptedPassword: String?,

    val encryptedPrivateKey: String?,

    @PrimaryKey(autoGenerate = true)
    val id: Int,
) {
    companion object {
        fun new(
            host: String,
            port: Int,
            username: String,
            password: String? = null,
            privateKey: String? = null,
        ) = Server(
            host, port, username, Crypto.encrypt(password),
            Crypto.encrypt(privateKey?.trim()),
            id = 0,
        )
    }

    val password get() = Crypto.decrypt(encryptedPassword)

    val privateKey get() = Crypto.decrypt(encryptedPrivateKey)
}
