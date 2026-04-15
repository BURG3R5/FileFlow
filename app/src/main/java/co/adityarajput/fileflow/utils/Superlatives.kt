package co.adityarajput.fileflow.utils

import co.adityarajput.fileflow.R
import net.schmizz.sshj.sftp.RemoteResourceInfo

enum class FileSuperlative(
    val displayName: Int,
    val selector: (File) -> Long,
    val remoteSelector: (RemoteResourceInfo) -> Long,
) {
    EARLIEST(R.string.earliest, { -it.lastModified }, { -it.attributes.mtime }),
    LATEST(R.string.latest, { it.lastModified }, { it.attributes.mtime }),
    SMALLEST(R.string.smallest, { -it.length }, { -it.attributes.size }),
    LARGEST(R.string.largest, { it.length }, { it.attributes.size }),
    NONE(R.string.all, { 0L }, { 0L }),
}
