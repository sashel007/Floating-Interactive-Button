package ru.ikar.floatingbutton_ikar.util

import android.content.Context

private fun convertDpToPixel(dp: Int, context: Context): Int {
    return (dp * context.resources.displayMetrics.density).toInt()
}