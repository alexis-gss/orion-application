package com.orion.app.books.data

import android.content.Context
import android.net.Uri
import com.orion.app.R
import com.orion.app.core.data.GzipJsonExportImport

/**
 * Books-domain façade over [GzipJsonExportImport], fixing the bundle serializer and
 * file-name prefix.
 */
object BooksExportImportManager {

    fun writeToUri(context: Context, uri: Uri, bundle: BooksExportBundle) =
        GzipJsonExportImport.writeToUri(context, uri, BooksExportBundle.serializer(), bundle)

    fun readFromUri(context: Context, uri: Uri): BooksExportBundle =
        GzipJsonExportImport.readFromUri(
            context, uri, BooksExportBundle.serializer(),
            readErrorMessage = context.getString(R.string.settings_data_export_import_read_error)
        )

    fun defaultFileName(): String = GzipJsonExportImport.defaultFileName("orion-books-export", "json.gz")
}
