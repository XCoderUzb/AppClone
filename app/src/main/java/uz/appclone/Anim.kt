package uz.appclone

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

/** Pastdan yumshoq chiqib kelish. */
fun View.slideUpIn(delay: Long = 0) {
    alpha = 0f
    translationY = 60f
    animate().alpha(1f).translationY(0f)
        .setStartDelay(delay).setDuration(450)
        .setInterpolator(DecelerateInterpolator()).start()
}

/** Doimiy "nafas olish" animatsiyasi (Saqlash tugmasi uchun). */
fun View.pulse(): Animator {
    val a = ObjectAnimator.ofPropertyValuesHolder(
        this,
        PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.04f),
        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.04f)
    )
    a.duration = 800
    a.repeatCount = ValueAnimator.INFINITE
    a.repeatMode = ValueAnimator.REVERSE
    a.start()
    return a
}

/** Bosilganda ozgina kichrayish. */
@SuppressLint("ClickableViewAccessibility")
fun View.pressEffect() {
    setOnTouchListener { v, e ->
        when (e.action) {
            MotionEvent.ACTION_DOWN ->
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }
        false
    }
}
