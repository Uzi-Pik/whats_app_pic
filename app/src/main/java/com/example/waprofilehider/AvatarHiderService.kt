package com.example.waprofilehider

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Hides WhatsApp profile avatars using an accessibility overlay.
 * Tuned for modern Android phones, including Redmi Note 14 class displays.
 */
class AvatarHiderService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: FrameLayout? = null
    private val masks = mutableListOf<View>()
    private var lastSignature = ""
    private var density = 1f
    private var screenWidth = 0
    private var screenHeight = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        isEnabled = true
        density = resources.displayMetrics.density
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels
        createOverlay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()
        if (pkg != WHATSAPP) {
            clearMasks()
            return
        }
        handler.removeCallbacksAndMessages(null)
        // WhatsApp frequently rebuilds rows in several passes; wait briefly for the
        // final layout before measuring nodes.
        handler.postDelayed({ refreshMasks() }, 90)
    }

    override fun onInterrupt() = clearMasks()

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        clearMasks()
        isEnabled = false
        super.onDestroy()
    }

    private fun dp(value: Float): Int = (value * density + 0.5f).toInt()

    private fun createOverlay() {
        if (overlay != null) return
        overlay = FrameLayout(this).also { frame ->
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            windowManager.addView(frame, lp)
        }
    }

    private fun refreshMasks() {
        val root = rootInActiveWindow ?: run { clearMasks(); return }
        if (root.packageName?.toString() != WHATSAPP) {
            clearMasks()
            return
        }

        val found = mutableListOf<Rect>()
        collect(root, found)

        // Prevent unnecessary redraws, which also reduces flicker on Redmi/MIUI.
        val unique = found.distinctBy { "${it.left},${it.top},${it.right},${it.bottom}" }
        val signature = unique.joinToString("|") { "${it.left},${it.top},${it.right},${it.bottom}" }
        if (signature == lastSignature) return
        lastSignature = signature

        overlay?.let { frame ->
            frame.removeAllViews()
            masks.clear()
            unique.forEach { r -> addMask(frame, r) }
        }
    }

    private fun addMask(frame: FrameLayout, r: Rect) {
        val size = min(r.width(), r.height())
        val v = View(this).apply {
            // A circular mask matches WhatsApp avatars and avoids ugly square blocks.
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(232, 232, 232))
            }
            elevation = 50f
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val lp = FrameLayout.LayoutParams(size, size).apply {
            leftMargin = r.left + (r.width() - size) / 2
            topMargin = r.top + (r.height() - size) / 2
            gravity = Gravity.TOP or Gravity.START
        }
        frame.addView(v, lp)
        masks.add(v)
    }

    private fun collect(node: AccessibilityNodeInfo, out: MutableList<Rect>) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (isLikelyProfileAvatar(node, bounds)) out.add(bounds)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                collect(child, out)
            } finally {
                child.recycle()
            }
        }
    }

    private fun isLikelyProfileAvatar(node: AccessibilityNodeInfo, b: Rect): Boolean {
        val w = b.width()
        val h = b.height()
        if (w <= 0 || h <= 0) return false

        // WhatsApp profile pictures are normally circular/square and roughly
        // 36–64dp. Work in dp so this is stable on Redmi/HyperOS densities.
        val minSize = dp(30f)
        val maxSize = dp(72f)
        val tolerance = max(dp(6f), (min(w, h) * 0.20f).toInt())
        if (w !in minSize..maxSize || h !in minSize..maxSize || abs(w - h) > tolerance) return false

        val cls = node.className?.toString().orEmpty()
        if (!cls.contains("ImageView", ignoreCase = true) &&
            !cls.contains("ImageButton", ignoreCase = true)) return false

        val id = node.viewIdResourceName?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        val text = node.text?.toString()?.lowercase().orEmpty()

        // Strong semantic signals: if WhatsApp exposes these, trust them.
        val semanticAvatar = listOf("avatar", "profile", "photo", "picture", "contact_photo").any {
            id.contains(it) || desc.contains(it) || text.contains(it)
        }
        if (semanticAvatar) return true

        // Both LTR and RTL WhatsApp layouts are supported. Avatars in the chat
        // list sit close to either screen edge; the conversation header does too.
        val edgeLeft = b.left <= dp(92f)
        val edgeRight = b.right >= screenWidth - dp(92f)
        val nearEdge = edgeLeft || edgeRight
        if (!nearEdge) return false

        // Header avatar: upper area, close to an edge.
        val header = b.top <= dp(112f) &&
            w in dp(34f)..dp(62f) && h in dp(34f)..dp(62f)
        if (header) return true

        // Chat-list avatar: avoid the status bar and bottom navigation, but allow
        // the full list. This is deliberately broad because WhatsApp changes row
        // layouts between releases.
        val listArea = b.top >= dp(48f) && b.bottom <= screenHeight - dp(40f)
        val rowSize = w in dp(38f)..dp(68f) && h in dp(38f)..dp(68f)
        if (listArea && rowSize) return true

        // Clickable contact images are a useful fallback in newer WhatsApp builds.
        return listArea && (node.isClickable || node.isLongClickable)
    }

    private fun clearMasks() {
        lastSignature = ""
        overlay?.removeAllViews()
        masks.clear()
    }

    companion object {
        const val WHATSAPP = "com.whatsapp"
        @Volatile var isEnabled = false
    }
}
