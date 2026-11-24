package com.batodev.jigsawpuzzlecuties.activity

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.batodev.jigsawpuzzlecuties.R
import com.batodev.jigsawpuzzlecuties.helpers.AdHelper
import com.batodev.jigsawpuzzlecuties.helpers.FirebaseHelper
import com.batodev.jigsawpuzzlecuties.helpers.NeonBtnOnPressChangeLook
import com.batodev.jigsawpuzzlecuties.helpers.RemoveBars
import com.batodev.jigsawpuzzlecuties.helpers.SettingsHelper
import com.google.android.material.snackbar.Snackbar
import com.smb.glowbutton.NeonButton

/**
 * The main menu activity of the application.
 */
class MainMenuActivity : AppCompatActivity() {
    companion object {
        private const val MENU_ANIMATION_DELAY_MS = 500L
        private const val MENU_BUTTON_INITIAL_SCALE = 0.5f
        private const val MENU_BUTTON_ANIMATION_DURATION_MS = 500L
        private const val MENU_BUTTON_STAGGER_DELAY_MS = 200L
    }

    /**
     * Called when the activity is first created.
     * Initializes the UI, loads settings, and sets up event listeners for menu buttons.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_menu_activity)
        FirebaseHelper.logScreenView(this, "MainMenuActivity")
        RemoveBars.removeTopBottomAndActionBars(this)
        SettingsHelper.load(this)
        AdHelper.loadAd(this)

        val playButton = findViewById<NeonButton>(R.id.main_menu_activity_play_the_game)
        val galleryButton = findViewById<NeonButton>(R.id.main_menu_activity_unlocked_gallery)
        val emberfoxLogo = findViewById<ImageView>(R.id.main_menu_activity_emberfox_logo)

        playButton.visibility = View.INVISIBLE
        galleryButton.visibility = View.INVISIBLE
        emberfoxLogo.visibility = View.INVISIBLE

        playButton.setOnClickListener { play() }
        galleryButton.setOnClickListener { gallery() }

        NeonBtnOnPressChangeLook.setupNeonButtonTouchListeners(this, playButton, galleryButton)

        // Delay the menu button animations
        Handler(Looper.getMainLooper()).postDelayed({
            animateMenuButtons(playButton, galleryButton, emberfoxLogo)
        }, MENU_ANIMATION_DELAY_MS)
    }

    private fun animateMenuButtons(vararg views: View) {
        for ((index, view) in views.withIndex()) {
            // Make view visible just before animation starts
            view.visibility = View.VISIBLE

            view.alpha = 0f
            view.scaleX = MENU_BUTTON_INITIAL_SCALE
            view.scaleY = MENU_BUTTON_INITIAL_SCALE

            val animator =
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                        ObjectAnimator.ofFloat(view, "scaleX", MENU_BUTTON_INITIAL_SCALE, 1f),
                        ObjectAnimator.ofFloat(view, "scaleY", MENU_BUTTON_INITIAL_SCALE, 1f),
                    )
                    duration = MENU_BUTTON_ANIMATION_DURATION_MS
                    interpolator = AccelerateDecelerateInterpolator()
                    startDelay = index * MENU_BUTTON_STAGGER_DELAY_MS
                }
            animator.start()
        }
    }

    /**
     * Starts the {@link ImagePickActivity} to allow the user to select an image for the puzzle.
     * @see ImagePickActivity
     */
    fun play() {
        FirebaseHelper.logButtonClick(this, "play")
        startActivity(Intent(this, ImagePickActivity::class.java))
    }

    /**
     * Opens the {@link GalleryActivity} if there are unlocked pictures, otherwise shows a toast message.
     * @see GalleryActivity
     * @see SettingsHelper
     */
    fun gallery() {
        FirebaseHelper.logButtonClick(this, "gallery")
        SettingsHelper.load(this)
        if (!SettingsHelper.load(this).uncoveredPics.isEmpty()) {
            startActivity(Intent(this, GalleryActivity::class.java))
        } else {
            Snackbar
                .make(
                    findViewById(android.R.id.content),
                    R.string.main_menu_activity_play_to_uncover,
                    Snackbar.LENGTH_SHORT,
                ).show()
        }
    }

    // No "more apps" / sequel links: this is a Families app, and the developer
    // page lists apps that are not, so linking out got it rejected (2026-11).
}
