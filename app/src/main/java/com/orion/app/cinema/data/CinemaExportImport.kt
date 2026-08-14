package com.orion.app.cinema.data

import android.content.Context
import android.net.Uri
import com.orion.app.R
import com.orion.app.core.data.GzipJsonExportImport
import kotlinx.serialization.Serializable

/** Cinema-domain façade over [GzipJsonExportImport], fixing the bundle serializer and file-name prefix. */
object CinemaExportImportManager {

    fun writeToUri(context: Context, uri: Uri, bundle: CinemaExportBundle) =
        GzipJsonExportImport.writeToUri(context, uri, CinemaExportBundle.serializer(), bundle)

    fun readFromUri(context: Context, uri: Uri): CinemaExportBundle =
        GzipJsonExportImport.readFromUri(
            context, uri, CinemaExportBundle.serializer(),
            readErrorMessage = context.getString(R.string.settings_data_export_import_read_error)
        )

    fun defaultFileName(): String = GzipJsonExportImport.defaultFileName("orion-cinema-export", "json.gz")
}
