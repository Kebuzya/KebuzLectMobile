package com.kebuz.kebuzlect.data.storage

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.OutputStream

object StorageCompat {

    fun requiredReadPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    sealed interface DeleteResult {
        data object Deleted : DeleteResult
        data class NeedsConfirmation(val intentSender: IntentSender) : DeleteResult
    }

    fun deletePhotos(context: Context, uris: List<Uri>): DeleteResult {
        if (uris.isEmpty()) return DeleteResult.Deleted
        val resolver = context.contentResolver
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val pendingIntent = MediaStore.createDeleteRequest(resolver, uris)
                DeleteResult.NeedsConfirmation(pendingIntent.intentSender)
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> deleteOnApi29(resolver, uris)

            else -> {
                uris.forEach { resolver.delete(it, null, null) }
                DeleteResult.Deleted
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteOnApi29(resolver: ContentResolver, uris: List<Uri>): DeleteResult =
        try {
            uris.forEach { resolver.delete(it, null, null) }
            DeleteResult.Deleted
        } catch (security: RecoverableSecurityException) {
            DeleteResult.NeedsConfirmation(security.userAction.actionIntent.intentSender)
        }

    fun finalizeDeletion(context: Context, uris: List<Uri>) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            uris.forEach { runCatching { resolver.delete(it, null, null) } }
        }
    }

    fun createPdfInTree(context: Context, treeUriString: String, filename: String): OutputStream? {
        if (treeUriString.isBlank()) return null
        val resolver = context.contentResolver
        val treeUri = Uri.parse(treeUriString)
        val parentDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
        val fileUri = DocumentsContract.createDocument(resolver, parentDocUri, "application/pdf", filename)
            ?: return null
        return resolver.openOutputStream(fileUri)
    }
}
