package com.xeg911.appcontrol.ui.util

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation.NavBackStackEntry

object MotionSpec {
    const val DURATION_SHORT = 150
    const val DURATION_MEDIUM = 250
    const val DURATION_LONG = 350

    /**
     * Fraction of the screen width a page slides during navigation.
     */
    private const val SLIDE_FRACTION = 0.25f

    fun <T> tweenMedium() = tween<T>(DURATION_MEDIUM, easing = FastOutSlowInEasing)
    fun <T> tweenLong() = tween<T>(DURATION_LONG, easing = FastOutSlowInEasing)

    fun slideOffset(fullWidth: Int): Int = (fullWidth * SLIDE_FRACTION).toInt()
}

fun standardFadeTransition(): ContentTransform =
    fadeIn(tween(MotionSpec.DURATION_MEDIUM)) togetherWith fadeOut(tween(MotionSpec.DURATION_SHORT))

fun tabSlideTransition(fromIndex: Int, toIndex: Int): ContentTransform {
    val forward = toIndex >= fromIndex
    val sign = if (forward) 1 else -1
    return (slideInHorizontally(MotionSpec.tweenMedium()) { sign * MotionSpec.slideOffset(it) } +
            fadeIn(MotionSpec.tweenMedium())) togetherWith
            (slideOutHorizontally(MotionSpec.tweenMedium()) { -sign * MotionSpec.slideOffset(it) } +
                    fadeOut(tween(MotionSpec.DURATION_SHORT)))
}

/**
 * Shared-axis style transitions for the navigation graph. Pushing a screen slides it
 * in from the right, popping slides it back out to the right.
 */
object NavTransitions {
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(MotionSpec.tweenLong()) { MotionSpec.slideOffset(it) } +
                fadeIn(MotionSpec.tweenLong())
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(MotionSpec.tweenLong()) { -MotionSpec.slideOffset(it) } +
                fadeOut(MotionSpec.tweenMedium())
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(MotionSpec.tweenLong()) { -MotionSpec.slideOffset(it) } +
                fadeIn(MotionSpec.tweenLong())
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(MotionSpec.tweenLong()) { MotionSpec.slideOffset(it) } +
                fadeOut(MotionSpec.tweenMedium())
    }

    /**
     * Plain fade for auth <-> device list where a directional slide makes no sense.
     */
    val fadeEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(MotionSpec.tweenLong())
    }
    val fadeExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(MotionSpec.tweenMedium())
    }
}
