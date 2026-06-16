package com.mobilemuuzaji.app

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView

object TooltipHelper {

    const val TOOLTIP_NEW_ORG       = "tooltip_new_org"
    const val TOOLTIP_SELL_BUTTON   = "tooltip_sell_button"
    const val TOOLTIP_SIDE_PANEL    = "tooltip_side_panel"
    const val TOOLTIP_SELL_DIALOG   = "tooltip_sell_dialog"
    const val TOOLTIP_NEW_INVENTORY = "tooltip_new_inventory"
    const val TOOLTIP_EDIT_BUTTON   = "tooltip_edit_button"

    fun hasBeenShown(context: Context, key: String): Boolean {
        return context.getSharedPreferences("tooltips", Context.MODE_PRIVATE)
            .getBoolean(key, false)
    }

    fun markAsShown(context: Context, key: String) {
        context.getSharedPreferences("tooltips", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, true)
            .apply()
    }

    // Show a single tooltip
    fun show(
        context:        Context,
        anchor:         View,
        message:        String,
        key:            String,
        arrowDirection: ArrowDirection = ArrowDirection.DOWN,
        onDismissed:    (() -> Unit)?  = null   // ← callback for chaining
    ) {
        if (hasBeenShown(context, key)) {
            // Already seen — skip and fire next in chain immediately
            onDismissed?.invoke()
            return
        }

        val inflater    = LayoutInflater.from(context)
        val tooltipView = inflater.inflate(R.layout.tooltip_bubble, null)

        tooltipView.findViewById<TextView>(R.id.tvTooltipText).text = message

        val arrow = tooltipView.findViewById<View>(R.id.tooltipArrow)
        if (arrowDirection == ArrowDirection.UP) {
            arrow.rotation = 180f
        }

        val popup = PopupWindow(
            tooltipView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popup.isOutsideTouchable = true
        popup.elevation          = 8f

        popup.setOnDismissListener {
            markAsShown(context, key)
            onDismissed?.invoke()   // ← trigger next tooltip in chain
        }

        anchor.post {
            tooltipView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            val anchorLocation = IntArray(2)
            anchor.getLocationOnScreen(anchorLocation)

            val tooltipWidth  = tooltipView.measuredWidth
            val tooltipHeight = tooltipView.measuredHeight

            val xOffset = anchorLocation[0] + (anchor.width / 2) - (tooltipWidth / 2)
            val yOffset = if (arrowDirection == ArrowDirection.DOWN) {
                anchorLocation[1] - tooltipHeight - 8
            } else {
                anchorLocation[1] + anchor.height + 8
            }

            popup.showAtLocation(anchor, Gravity.NO_GRAVITY, xOffset, yOffset)
        }
    }

    // Show a chain of tooltips one after another
    fun showChain(context: Context, tooltips: List<TooltipStep>) {
        if (tooltips.isEmpty()) return

        fun showStep(index: Int) {
            if (index >= tooltips.size) return

            val step = tooltips[index]
            show(
                context        = context,
                anchor         = step.anchor,
                message        = step.message,
                key            = step.key,
                arrowDirection = step.arrowDirection,
                onDismissed    = { showStep(index + 1) }   // show next on dismiss
            )
        }

        showStep(0)
    }

    data class TooltipStep(
        val anchor:         View,
        val message:        String,
        val key:            String,
        val arrowDirection: ArrowDirection = ArrowDirection.DOWN
    )

    enum class ArrowDirection { UP, DOWN }
}