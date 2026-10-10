package co.adityarajput.fileflow.data

import co.adityarajput.fileflow.data.models.Action
import co.adityarajput.fileflow.data.models.RemoteAction
import co.adityarajput.fileflow.data.models.Server
import co.adityarajput.fileflow.utils.FileSuperlative
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    @Test
    fun Converters_Action() {
        val actions = Action.entries + RemoteAction.entries + listOf(
            Action.MOVE(
                "/storage/emulated/0/AntennaPod",
                "AntennaPodBackup-\\d{4}-\\d{2}-\\d{2}\\.db",
                "/storage/emulated/0/Backups",
                "AntennaPod.db",
                scanSubdirectories = true,
                overwriteExisting = true,
                preserveStructure = false,
            ),
            Action.MOVE(
                "/storage/emulated/0/Backups",
                "PipePipeData-\\d{8}_\\d{6}\\.zip",
                "/storage/emulated/0/Backups",
                $$"PipePipe-${uuid}.zip",
                keepOriginal = false,
                superlative = FileSuperlative.LARGEST,
                deleteEmptySrcSubdirectories = true,
            ),
            Action.DELETE_STALE(
                "/storage/emulated/0/Download",
                "(NotiFilter|Alarmetrics|FileFlow|MinCal)_v[\\d\\.]+\\.apk",
                90,
                scanSubdirectories = true,
                deleteEmptySrcSubdirectories = true,
            ),
            Action.ZIP(
                "/storage/emulated/0/Documents/Notes",
                "(.*)\\.md",
                "/storage/emulated/0/Backups",
                "Notes.zip",
                scanSubdirectories = true,
                overwriteExisting = true,
                preserveStructure = false,
            ),
            Action.EMIT_CHANGES(
                "/storage/emulated/0/Movies",
                ".*\\.mp4",
                "org.amoradi.syncopoli.SYNC_PROFILE",
                "org.amoradi.syncopoli",
                """{"profile_name": "Movies Backup"}""",
                true,
                3_600_000L * 3,
            ),
            RemoteAction.MOVE(
                Server(
                    "192.168.1.12",
                    3456,
                    "admin",
                    "hunter2",
                    null,
                    1,
                ),
                "backups/navidrome",
                "navidrome_backup_[\\d_\\.]{19}\\.db",
                Server(
                    "192.168.1.78",
                    9012,
                    "sftp",
                    null,
                    "passkey",
                    2,
                ),
                "backups/glacial",
                "navidrome.db",
                overwriteExisting = true,
            ),
        )

        val serializedActions = listOf(
            """{"type":"co.adityarajput.fileflow.data.models.Action.MOVE","src":"","srcFileNamePattern":"","dest":"","destFileNameTemplate":""}""",
            """{"type":"co.adityarajput.fileflow.data.models.Action.DELETE_STALE","src":"","srcFileNamePattern":""}""",
            """{"type":"co.adityarajput.fileflow.data.models.Action.ZIP","src":"","srcFileNamePattern":"","dest":"","destFileNameTemplate":""}""",
            """{"type":"co.adityarajput.fileflow.data.models.Action.EMIT_CHANGES","src":"","srcFileNamePattern":"","intent":"","packageName":""}""",
            """{"type":"co.adityarajput.fileflow.data.models.RemoteAction.MOVE","srcServer":null,"src":"","srcFileNamePattern":"","destServer":null,"dest":"","destFileNameTemplate":""}""",
            """{"type":"co.adityarajput.fileflow.data.models.RemoteAction.DELETE_STALE","srcServer":{"host":"","port":0,"username":"","encryptedPassword":null,"encryptedPrivateKey":null,"id":0},"src":"","srcFileNamePattern":""}""",
            """{"type":"co.adityarajput.fileflow.data.models.RemoteAction.ZIP","srcServer":null,"src":"","srcFileNamePattern":"","destServer":null,"dest":"","destFileNameTemplate":""}""",
            """{"type":"co.adityarajput.fileflow.data.models.Action.EMIT_CHANGES","src":"","srcFileNamePattern":"","intent":"","packageName":""}""",
            """{"type":"co.adityarajput.fileflow.data.models.Action.MOVE","src":"/storage/emulated/0/AntennaPod","srcFileNamePattern":"AntennaPodBackup-\\d{4}-\\d{2}-\\d{2}\\.db","dest":"/storage/emulated/0/Backups","destFileNameTemplate":"AntennaPod.db","scanSubdirectories":true,"overwriteExisting":true,"preserveStructure":false}""",
            $$"""{"type":"co.adityarajput.fileflow.data.models.Action.MOVE","src":"/storage/emulated/0/Backups","srcFileNamePattern":"PipePipeData-\\d{8}_\\d{6}\\.zip","dest":"/storage/emulated/0/Backups","destFileNameTemplate":"PipePipe-${uuid}.zip","keepOriginal":false,"superlative":"LARGEST","deleteEmptySrcSubdirectories":true}""",
            """{"type":"co.adityarajput.fileflow.data.models.Action.DELETE_STALE","src":"/storage/emulated/0/Download","srcFileNamePattern":"(NotiFilter|Alarmetrics|FileFlow|MinCal)_v[\\d\\.]+\\.apk","retentionDays":90,"scanSubdirectories":true,"deleteEmptySrcSubdirectories":true}""",
            """{"type":"co.adityarajput.fileflow.data.models.Action.ZIP","src":"/storage/emulated/0/Documents/Notes","srcFileNamePattern":"(.*)\\.md","dest":"/storage/emulated/0/Backups","destFileNameTemplate":"Notes.zip","scanSubdirectories":true,"overwriteExisting":true,"preserveStructure":false}""",
            """{"type":"co.adityarajput.fileflow.data.models.Action.EMIT_CHANGES","src":"/storage/emulated/0/Movies","srcFileNamePattern":".*\\.mp4","intent":"org.amoradi.syncopoli.SYNC_PROFILE","packageName":"org.amoradi.syncopoli","extras":"{\"profile_name\": \"Movies Backup\"}","scanSubdirectories":true,"modifiedWithin":10800000}""",
            """{"type":"co.adityarajput.fileflow.data.models.RemoteAction.MOVE","srcServer":{"host":"192.168.1.12","port":3456,"username":"admin","encryptedPassword":"hunter2","encryptedPrivateKey":null,"id":1},"src":"backups/navidrome","srcFileNamePattern":"navidrome_backup_[\\d_\\.]{19}\\.db","destServer":{"host":"192.168.1.78","port":9012,"username":"sftp","encryptedPassword":null,"encryptedPrivateKey":"passkey","id":2},"dest":"backups/glacial","destFileNameTemplate":"navidrome.db","overwriteExisting":true}""",
        )

        val incorrectSerializations =
            listOf("", "{", "}", "{}", serializedActions[0].substring(0, 10))

        Converters().run {
            for (i in actions.indices) {
                assertEquals(serializedActions[i], fromAction(actions[i]))
                assertEquals(actions[i], toAction(serializedActions[i]))
                assertEquals(
                    actions[i],
                    toAction(serializedActions[i].replaceFirst("{", """{"title": "deprecated",""")),
                )
            }
            for (string in incorrectSerializations) {
                assertEquals(Action.MOVE("", "", "", ""), toAction(string))
            }
        }
    }

    @Test
    fun Converters_IntList() {
        val intLists = listOf(
            listOf(1, 2), listOf(2, 1), listOf(2, 4, 6), listOf(2, 3, 5),
            listOf(0, 1, 2, 3, 4, 5, 6),
        )

        val incorrectSerializations =
            listOf("", "null", "null,")

        Converters().run {
            for (list in intLists) {
                assertEquals(list, toIntList(fromIntList(list)))
            }

            for (string in incorrectSerializations) {
                assertEquals(emptyList<Int>(), toIntList(string))
            }

            assertEquals(listOf(1), toIntList("1,null"))
        }
    }
}
