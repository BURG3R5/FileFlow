/**
 * Portions of this file are inspired by or adapted from NexusControl's SftpManager.kt, available
 * at https://github.com/iTroy0/NexusControl, licensed under the MIT License. Significant
 * modifications have been made to the code, and additional functionality has been implemented to
 * suit the needs of this application.
 *
 * NexusControl's SftpManager.kt is Copyright (c) 2026 NexusControl Contributors.
 */

package co.adityarajput.fileflow.services

import co.adityarajput.fileflow.BuildConfig
import co.adityarajput.fileflow.data.models.Server
import co.adityarajput.fileflow.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteResourceFilter
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.kex.DHGexSHA256
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import java.nio.file.Files
import kotlin.io.path.pathString
import java.io.File as IOFile

@Suppress("KotlinConstantConditions")
class SFTP {
    companion object {
        suspend fun <T> runOn(server: Server, action: suspend SFTPClient.() -> T?): T? {
            if (!BuildConfig.HAS_NETWORK_FEATURE)
                return null

            var result = null as T?

            withContext(Dispatchers.IO) {
                val sftp = SFTP()
                try {
                    sftp.connect(server)
                    if (sftp.isConnected && sftp.sftpClient != null) {
                        result = sftp.sftpClient!!.action()
                    } else {
                        Logger.e("SFTP", "Unable to connect to $server")
                    }
                } catch (e: Exception) {
                    Logger.e("SFTP", "Failed while connected to $server", e)
                } finally {
                    sftp.disconnect()
                }
            }

            return result
        }

        suspend fun canConnectTo(server: Server): Boolean {
            if (!BuildConfig.HAS_NETWORK_FEATURE)
                return false

            var canConnect = false
            runOn(server) {
                canConnect = true
            }
            return canConnect
        }
    }

    private var sshClient: SSHClient? = null
    private var sftpClient: SFTPClient? = null

    val isConnected get() = sshClient?.isConnected == true

    private fun connect(server: Server) {
        try {
            disconnect()

            sshClient = SSHClient(
                DefaultConfig().apply {
                    keyExchangeFactories = listOf(DHGexSHA256.Factory())
                },
            ).apply {
                addHostKeyVerifier(PromiscuousVerifier())
                connect(server.host, server.port)

                if (server.privateKey != null) {
                    val pemKey = server.privateKey!!
                    val tempKeyFile = IOFile(Files.createTempFile("key", ".pem").pathString)
                    try {
                        tempKeyFile.writeText(pemKey)
                        authPublickey(
                            server.username,
                            OpenSSHKeyFile().apply {
                                init(tempKeyFile)
                            },
                        )
                    } finally {
                        tempKeyFile.delete()
                    }
                } else if (!server.password.isNullOrBlank()) {
                    authPassword(server.username, server.password!!)
                }
            }
            sftpClient = sshClient!!.newSFTPClient()

            Logger.d("SFTP", "Connected to $server")
        } catch (e: Exception) {
            Logger.e("SFTP", "Failed to connect to $server", e)
        }
    }

    private fun disconnect() {
        try {
            sftpClient?.close()
        } catch (_: Exception) {
        }
        try {
            sshClient?.disconnect()
        } catch (_: Exception) {
        }
        sftpClient = null
        sshClient = null

        Logger.d("SFTP", "Disconnected")
    }
}

fun SFTPClient.ls(
    path: String,
    scanSubdirectories: Boolean,
    filter: (RemoteResourceInfo) -> Boolean,
): List<RemoteResourceInfo> {
    val directories = ArrayDeque(listOf(path))
    val files = mutableListOf<RemoteResourceInfo>()

    while (directories.isNotEmpty()) {
        files += ls(
            directories.removeFirst(),
            RemoteResourceFilter {
                if (scanSubdirectories && it.isDirectory) {
                    directories.add(it.path)
                }

                filter(it)
            },
        )
    }

    return files
}
