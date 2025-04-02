package com.permissionx.guolindev.dialog

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Surface
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.permissionx.guolindev.R

/**
 * @Author      : wangming
 * @Date        : 2025-03-20
 * @Description : 描述
 */
open class PermissionTipsView @JvmOverloads constructor(
    context: Context,
    private val horizontal: Boolean = false,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val iconImageView: ImageView
    private val titleTextView: TextView
    private val messageTextView: TextView
    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var isShowing = false

    init {
        val layoutRes =
            if (horizontal) R.layout.permissions_tips_dialog else R.layout.port_permissions_tips_dialog
        val contentView = LayoutInflater.from(context).inflate(layoutRes, this, true)
        iconImageView = contentView.findViewById(R.id.iconImageView)
        titleTextView = contentView.findViewById(R.id.titleTextView)
        messageTextView = contentView.findViewById(R.id.messageTextView)
    }

    fun setIcon(resId: Int) {
        iconImageView.visibility = VISIBLE
        iconImageView.setImageResource(resId)
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun setMessage(message: String) {
        messageTextView.text = message
    }

    fun show() {
        isShowing = true
        if (horizontal) showHorizontal() else showVertical()
    }

    private fun showHorizontal() {
        val metrics = DisplayMetrics().apply { windowManager.defaultDisplay.getMetrics(this) }
        val rotation = windowManager.defaultDisplay.rotation
        val screenWidth = when (rotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> (metrics.widthPixels * 0.9).toInt()
            else -> (metrics.widthPixels * 0.7).toInt()
        }
        show(screenWidth, WindowManager.LayoutParams.WRAP_CONTENT, true)
    }

    private fun showVertical() {
        show(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            false
        )
    }

    private fun show(width: Int, height: Int, isHorizontal: Boolean) {
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (isHorizontal) Gravity.TOP else Gravity.START or Gravity.CENTER_VERTICAL
            windowManager.addView(this@PermissionTipsView, this)
        }
    }

    fun dismiss() {
        if (isShowing) {
            windowManager.removeView(this@PermissionTipsView)
            isShowing = false
        }
    }

    class Builder(val context: Context) {
        private var iconResId: Int = 0
        private var title: String = ""
        private var message: String = ""
        private var backgroundDrawable: Drawable? = null
        private var horizontal: Boolean = true

        fun setIcon(iconResId: Int) = apply { this.iconResId = iconResId }
        fun setTitle(title: String) = apply { this.title = title }
        fun setMessage(message: String) = apply { this.message = message }
        fun setBackground(background: Drawable?) = apply { this.backgroundDrawable = background }
        fun setOrientation(horizontal: Boolean) = apply { this.horizontal = horizontal }

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
        fun build(): PermissionTipsView {
            return PermissionTipsView(context, horizontal).apply {
                if (iconResId != 0) {
                    setIcon(iconResId)
                }
                setTitle(title)
                setMessage(message)
                if (backgroundDrawable != null) {
                    background = backgroundDrawable
                }
            }
        }
    }
}