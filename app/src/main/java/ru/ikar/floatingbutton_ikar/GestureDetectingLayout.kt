package ru.ikar.floatingbutton_ikar

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class GestureDetectingLayout(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    var gestureListener: ThreeFingerGestureListener? = null  // Private member

    // Public setter for gestureListener
    fun setThreeFingerGestureListener(listener: ThreeFingerGestureListener) {
        gestureListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount == 3) {
            val avgX = (event.getX(0) + event.getX(1) + event.getX(2)) / 3
            val avgY = (event.getY(0) + event.getY(1) + event.getY(2)) / 3

            gestureListener?.onThreeFingerTap(avgX, avgY)  // Notify the listener
            return true
        }
        return super.onTouchEvent(event)
    }
}

