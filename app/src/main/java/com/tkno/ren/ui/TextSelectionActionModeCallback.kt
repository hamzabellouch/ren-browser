package com.tkno.ren.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.widget.Toast
import com.tkno.ren.R
import com.tkno.ren.util.SearchEngineManager

class TextSelectionActionModeCallback(
    private val context: Context,
    private val webView: WebView,
    private val originalCallback: ActionMode.Callback? = null,
    private val onWebSearch: ((String) -> Unit)? = null
) : ActionMode.Callback2() {

    companion object {
        const val MENU_ID_COPY = 1001
        const val MENU_ID_SELECT_ALL = 1002
        const val MENU_ID_SHARE = 1003
        const val MENU_ID_SEARCH = 1004
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        originalCallback?.onCreateActionMode(mode, menu)

        if (menu != null) {
            menu.clear()

            // 1. Copy
            val copyItem = menu.add(Menu.NONE, MENU_ID_COPY, 1, R.string.copy)
            copyItem.setIcon(R.drawable.ic_content_copy)
            copyItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

            // 2. Select All
            val selectAllItem = menu.add(Menu.NONE, MENU_ID_SELECT_ALL, 2, R.string.select_all)
            selectAllItem.setIcon(R.drawable.ic_select_all)
            selectAllItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

            // 3. Share
            val shareItem = menu.add(Menu.NONE, MENU_ID_SHARE, 3, R.string.share)
            shareItem.setIcon(R.drawable.ic_share)
            shareItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

            // 4. Web Search
            val searchItem = menu.add(Menu.NONE, MENU_ID_SEARCH, 4, R.string.web_search)
            searchItem.setIcon(R.drawable.ic_search)
            searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return originalCallback?.onPrepareActionMode(mode, menu) ?: false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val itemId = item?.itemId ?: return false

        when (itemId) {
            MENU_ID_COPY, android.R.id.copy -> {
                getSelectedText { text ->
                    if (text.isNotBlank()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Selected Text", text))
                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                    }
                }
                mode?.finish()
                return true
            }

            MENU_ID_SELECT_ALL, android.R.id.selectAll -> {
                webView.evaluateJavascript("(function(){ try { document.execCommand('selectAll'); } catch(e){} })();", null)
                return true
            }

            MENU_ID_SHARE, android.R.id.shareText -> {
                getSelectedText { text ->
                    if (text.isNotBlank()) {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share))
                        context.startActivity(shareIntent)
                    }
                }
                mode?.finish()
                return true
            }

            MENU_ID_SEARCH -> {
                getSelectedText { text ->
                    if (text.isNotBlank()) {
                        val searchUrl = SearchEngineManager.buildSearchUrl(context, text.trim())
                        onWebSearch?.invoke(searchUrl)
                    }
                }
                mode?.finish()
                return true
            }
        }

        return originalCallback?.onActionItemClicked(mode, item) ?: false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        originalCallback?.onDestroyActionMode(mode)
    }

    private fun getSelectedText(callback: (String) -> Unit) {
        val js = "(function(){ try { return window.getSelection().toString(); } catch(e){ return ''; } })();"
        webView.evaluateJavascript(js) { result ->
            val clean = cleanJsString(result)
            callback(clean)
        }
    }

    private fun cleanJsString(raw: String?): String {
        if (raw.isNullOrBlank() || raw == "null" || raw == "\"\"") return ""
        var s = raw.trim()
        if (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) {
            s = s.substring(1, s.length - 1)
        }
        return s.replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}
