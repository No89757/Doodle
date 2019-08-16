package com.horizon.doodle

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import android.annotation.SuppressLint
import android.content.Context


/**
 * Just for get the context
 */
internal class DoodleContentProvider : ContentProvider() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        internal lateinit var ctx: Context
            private set
    }

    override fun onCreate(): Boolean {
        ctx = checkNotNull(context)
        return true
    }

    override fun insert(uri: Uri,
                        values: ContentValues?) = null

    override fun query(uri: Uri,
                       projection: Array<String>?,
                       selection: String?,
                       selectionArgs: Array<String>?,
                       sortOrder: String?) = null

    override fun update(uri: Uri,
                        values: ContentValues?,
                        selection: String?,
                        selectionArgs: Array<String>?) = 0

    override fun delete(uri: Uri,
                        selection: String?,
                        selectionArgs: Array<String>?) = 0

    override fun getType(uri: Uri) = null

}