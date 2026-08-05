package io.github.ultralan.lsposed.core.update

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File

class UpdateApkContentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = APK_MIME_TYPE

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(uri == URI && mode == "r") { "仅允许读取 LSPosed 更新包" }
        val apkFile = File(requireNotNull(context).cacheDir, "updates/LSPosed.apk")
        return ParcelFileDescriptor.open(apkFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val apkFile = File(requireNotNull(context).cacheDir, "updates/LSPosed.apk")
        return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)).apply {
            addRow(arrayOf("LSPosed.apk", apkFile.length()))
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val AUTHORITY = "io.github.ultralan.lsposed.updates.apk"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        val URI: Uri = Uri.parse("content://$AUTHORITY/LSPosed.apk")
    }
}
