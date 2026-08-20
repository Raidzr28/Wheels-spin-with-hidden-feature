// Animation stand-ins. animateTo jumps straight to the target: the test cares about the angle
// the spin lands on, not the frames in between.
package androidx.compose.animation.core

class CubicBezierEasing(
    @Suppress("unused") val a: Float,
    @Suppress("unused") val b: Float,
    @Suppress("unused") val c: Float,
    @Suppress("unused") val d: Float,
)

class TweenSpec(val durationMillis: Int, val easing: Any?)

fun tween(durationMillis: Int = 300, delayMillis: Int = 0, easing: Any? = null): TweenSpec {
    @Suppress("UNUSED_EXPRESSION") delayMillis
    return TweenSpec(durationMillis, easing)
}

class Animatable(initialValue: Float) {
    var value: Float = initialValue
        private set

    suspend fun snapTo(targetValue: Float) {
        value = targetValue
    }

    suspend fun animateTo(targetValue: Float, animationSpec: TweenSpec? = null) {
        @Suppress("UNUSED_EXPRESSION") animationSpec
        value = targetValue
    }
}
