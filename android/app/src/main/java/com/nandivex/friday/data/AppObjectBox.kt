package com.nandivex.friday.data

import android.content.Context
import android.util.Log
import io.objectbox.BoxStore
import java.io.File

/**
 * The app's one ObjectBox store, which now holds the saved conversation history. The store keeps its old name so
 * existing history is still found after the Knowledge base and the picture library were removed.
 */
internal object AppObjectBox {
    private var sharedStore: BoxStore? = null

    /**
     * One store per process: a restarted ViewModel must reuse it, since ObjectBox rejects a second open store on the
     * same directory and the old one is never closed.
     */
    @Synchronized
    fun get(appContext: Context): BoxStore {
        sharedStore?.takeUnless { it.isClosed }?.let { return it }
        val store = try {
            open(appContext)
        } catch (e: IllegalStateException) {
            // A store written by an older build (it still holds the removed Knowledge vector index) cannot be opened
            // by the current model. Keep the old files aside and start a clean store so History works again.
            Log.w("AssistantError", "ObjectBox store could not be opened, starting a new one", e)
            val dir = File(appContext.filesDir, "objectbox/$STORE_NAME")
            if (dir.exists()) dir.renameTo(File(dir.parentFile, "$STORE_NAME.old-${System.currentTimeMillis()}"))
            open(appContext)
        }
        sharedStore = store
        return store
    }

    private fun open(appContext: Context): BoxStore =
        MyObjectBox.builder().androidContext(appContext).name(STORE_NAME).build()

    private const val STORE_NAME = "knowledge-vector-store"
}
