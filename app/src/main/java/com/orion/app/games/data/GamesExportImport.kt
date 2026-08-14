package com.orion.app.games.data

import android.content.Context
import android.net.Uri
import com.orion.app.R
import com.orion.app.core.data.GzipJsonExportImport

/**
 * Games-domain façade over [GzipJsonExportImport], fixing the bundle serializer and
 * file-name prefix.
 */
object GamesExportImportManager {

    fun writeToUri(context: Context, uri: Uri, bundle: GamesExportBundle) =
        GzipJsonExportImport.writeToUri(context, uri, GamesExportBundle.serializer(), bundle)

    fun readFromUri(context: Context, uri: Uri): GamesExportBundle =
        GzipJsonExportImport.readFromUri(
            context, uri, GamesExportBundle.serializer(),
            readErrorMessage = context.getString(R.string.settings_data_export_import_read_error)
        )

    fun defaultFileName(): String = GzipJsonExportImport.defaultFileName("orion-games-export", "json.gz")
}
