package com.karirjepang.dailymonitoringkj.ui.main

import com.karirjepang.dailymonitoringkj.R

/**
 * Central place to configure slide transition animation.
 *
 * Change [current] to switch between animation styles.
 *
 * Available styles:
 *  - [Style.FADE_SEQUENTIAL]  → Fade out content → pause → fade in content (1.5s each, header stays)
 *  - [Style.FADE_CROSSFADE]   → Simultaneous crossfade (1.5s)
 *  - [Style.SLIDE_RIGHT]      → Slide in from right + fade (0.6s)
 */
object SlideAnimationConfig {

    /** ← Change this value to switch animation across the whole app */
    val current: Style = Style.FADE_SEQUENTIAL

    enum class Style(
        /** Animation for the entering fragment (0 = no XML anim, handled manually) */
        val enter: Int,
        /** Animation for the exiting fragment (0 = no XML anim, handled manually) */
        val exit: Int,
        /** If true: fade out first → pause → then fade in (no overlap) */
        val sequential: Boolean,
        /** Duration of fade out in ms (only used when sequential = true) */
        val fadeOutMs: Long,
        /** Duration of fade in in ms (only used when sequential = true) */
        val fadeInMs: Long,
        /** Pause between fade out and fade in in ms (only used when sequential = true) */
        val pauseMs: Long
    ) {
        FADE_SEQUENTIAL(
            enter       = 0,
            exit        = 0,
            sequential  = true,
            fadeOutMs    = 1500L,
            fadeInMs     = 1500L,
            pauseMs     = 300L
        ),
        FADE_CROSSFADE(
            enter       = R.anim.fade_in_slow,
            exit        = R.anim.fade_out_slow,
            sequential  = false,
            fadeOutMs    = 0,
            fadeInMs     = 0,
            pauseMs     = 0
        ),
        SLIDE_RIGHT(
            enter       = R.anim.slide_in_right,
            exit        = R.anim.slide_out_left,
            sequential  = false,
            fadeOutMs    = 0,
            fadeInMs     = 0,
            pauseMs     = 0
        );
    }
}

