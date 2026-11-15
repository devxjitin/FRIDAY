package com.nandivex.friday.data

import android.content.Context
import io.objectbox.BoxStore

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
    fun get(appContext: Context): BoxStore =
        sharedStore?.takeUnless { it.isClosed } ?: MyObjectBox.builder()
            .androidContext(appContext)
            .name("knowledge-vector-store")
            .build()
            .also { sharedStore = it }
}
