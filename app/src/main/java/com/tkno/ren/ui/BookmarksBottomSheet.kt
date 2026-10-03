package com.tkno.ren.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.platform.ComposeView
import com.tkno.ren.R
import com.tkno.ren.ui.theme.RenTheme
import com.tkno.ren.ui.theme.ThemeManager

class BookmarksBottomSheet(
    context: Context,
    private val onOpenBookmark: (String) -> Unit,
    private val onOpenInNewTab: (String) -> Unit = {},
    private val initialFolderId: String? = null
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val composeView = ComposeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                RenTheme(isIncognito = ThemeManager.isIncognitoActive) {
                    BookmarksScreen(
                        onOpenBookmark = { url ->
                            onOpenBookmark(url)
                            dismiss()
                        },
                        onOpenInNewTab = { url ->
                            onOpenInNewTab(url)
                            dismiss()
                        },
                        onClose = {
                            dismiss()
                        },
                        initialFolderId = initialFolderId,
                        isIncognito = ThemeManager.isIncognitoActive
                    )
                }
            }
        }

        setContentView(composeView)

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setWindowAnimations(R.style.BottomSheetAnimation)
        }
    }
}
