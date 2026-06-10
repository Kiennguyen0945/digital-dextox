package com.example.digitaldextox

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.AppCompatEditText

class NoPasteEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    init {
        // Vô hiệu hóa menu khi bôi đen / nhấn giữ
        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
            override fun onDestroyActionMode(mode: ActionMode?) {}
        }
        isLongClickable = false
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        // Chặn action Dán (Paste)
        if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
            return false
        }
        return super.onTextContextMenuItem(id)
    }
}