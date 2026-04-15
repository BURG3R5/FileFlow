package co.adityarajput.fileflow.viewmodels.servers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.data.Repository
import co.adityarajput.fileflow.data.models.Server
import co.adityarajput.fileflow.services.SFTP
import co.adityarajput.fileflow.utils.Logger

class CreateServerViewModel(private val repository: Repository) : ViewModel() {
    suspend fun submitForm(
        host: String,
        port: String,
        username: String,
        password: String,
        privateKey: String,
    ) {
        try {
            val server = Server.new(
                host, port.toInt(), username,
                password.takeIf { it.isNotBlank() },
                privateKey.takeIf { it.isNotBlank() },
            )
            if (SFTP.canConnectTo(server)) {
                Logger.d("CreateServerViewModel", "Adding $server")
                repository.upsert(server)
                return
            }
        } catch (_: Exception) {
        }

        error = Error.CANNOT_CONNECT
    }

    var error by mutableStateOf<Error?>(null)

    enum class Error(val message: Int) {
        CANNOT_CONNECT(R.string.cannot_connect);
    }
}
