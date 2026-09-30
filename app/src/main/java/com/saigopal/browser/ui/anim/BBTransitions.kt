package com.saigopal.browser.ui.anim

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset

/*
 * BBTransitions — the app's page-to-page motion language.
 *
 * Two moves, both spring-driven ("butter"):
 *  · MODAL (browser → settings/games/feedback and back): the destination rises a short distance
 *    while fading in — reads as "opening a panel", not "navigating away".
 *  · STACK (settings home ↔ inner screens): direction-aware horizontal slide — forward pushes
 *    in from the trailing edge, back slides the screen back out the same edge.
 *
 * Springs are gentle (no bounce, low stiffness) so they feel smooth on low-end devices too;
 * distances stay small (1/8–1/5 of the container) so nothing teleports or crawls.
 */

/** Root-route moves: browser ↔ settings/games/feedback. */
object BBTransitions {

    private const val MODAL_SLIDE_DIVISOR = 8
    private const val STACK_SLIDE_DIVISOR = 5

    private val modalSpring = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    )

    private val stackSpring = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 380f,
    )

    private val fadeTween = tween<Float>(durationMillis = 90)

    /** Entering a root destination (settings/games/feedback opened from the browser). */
    fun enterModal(): EnterTransition =
        slideInVertically(animationSpec = modalSpring) { height -> height / MODAL_SLIDE_DIVISOR } +
            fadeIn(animationSpec = fadeTween)

    /** Leaving a root destination when going back toward the browser. */
    fun exitModalToBrowser(): ExitTransition =
        slideOutVertically(animationSpec = modalSpring) { height -> height / MODAL_SLIDE_DIVISOR } +
            fadeOut(animationSpec = fadeTween)

    /** Leaving a root destination because a NEW one is opened from it. */
    fun exitModalForward(): ExitTransition =
        slideOutVertically(animationSpec = modalSpring) { height -> -height / MODAL_SLIDE_DIVISOR } +
            fadeOut(animationSpec = fadeTween)

    /** Inner screen pushing in from the right (settings home → route). */
    fun enterStackForward(): EnterTransition =
        slideInHorizontally(animationSpec = stackSpring) { width -> width / STACK_SLIDE_DIVISOR } +
            fadeIn(animationSpec = fadeTween)

    /** Inner screen sliding back out to the right (route → back to settings home). */
    fun exitStackBackward(): ExitTransition =
        slideOutHorizontally(animationSpec = stackSpring) { width -> width / STACK_SLIDE_DIVISOR } +
            fadeOut(animationSpec = fadeTween)

    /** Settings home re-appearing from the left after a back pop. */
    fun enterStackBackward(): EnterTransition =
        slideInHorizontally(animationSpec = stackSpring) { width -> -width / STACK_SLIDE_DIVISOR } +
            fadeIn(animationSpec = fadeTween)

    /** Settings home receding left as a route pushes over it. */
    fun exitStackForward(): ExitTransition =
        slideOutHorizontally(animationSpec = stackSpring) { width -> -width / STACK_SLIDE_DIVISOR } +
            fadeOut(animationSpec = fadeTween)
}
