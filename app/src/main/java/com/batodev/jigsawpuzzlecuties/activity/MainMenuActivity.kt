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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.batodev.jigsawpuzzlecuties.R
import com.batodev.jigsawpuzzlecuties.helpers.AdHelper
import com.batodev.jigsawpuzzlecuties.helpers.FirebaseHelper
import com.batodev.jigsawpuzzlecuties.helpers.NeonBtnOnPressChangeLook
import com.batodev.jigsawpuzzlecuties.helpers.RemoveBars
import com.batodev.jigsawpuzzlecuties.helpers.SettingsHelper
import com.smb.glowbutton.NeonButton

/**
 * The main menu activity of the application.
 */
class MainMenuActivity : AppCompatActivity() {
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
        val moreAppsButton = findViewById<NeonButton>(R.id.main_menu_activity_more_apps)
        val emberfoxLogo = findViewById<ImageView>(R.id.main_menu_activity_emberfox_logo)

        playButton.visibility = View.INVISIBLE
        galleryButton.visibility = View.INVISIBLE
        moreAppsButton.visibility = View.INVISIBLE
        emberfoxLogo.visibility = View.INVISIBLE

        playButton.setOnClickListener { play() }
        galleryButton.setOnClickListener { gallery() }
        moreAppsButton.setOnClickListener { moreApps() }

        NeonBtnOnPressChangeLook.setupNeonButtonTouchListeners(this, playButton, galleryButton, moreAppsButton)

        // Delay the menu button animations
        Handler(Looper.getMainLooper()).postDelayed({
            animateMenuButtons(playButton, galleryButton, moreAppsButton, emberfoxLogo)
        }, 500) // Corrected to 9.5 seconds delay

    }


    private fun animateMenuButtons(vararg views: View) {
        for ((index, view) in views.withIndex()) {
            // Make view visible just before animation starts
            view.visibility = View.VISIBLE

            view.alpha = 0f
            view.scaleX = 0.5f
            view.scaleY = 0.5f

            val animator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1f),
                    ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1f)
                )
                duration = 500
                interpolator = AccelerateDecelerateInterpolator()
                startDelay = (index * 200).toLong()
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
            Toast.makeText(this, R.string.main_menu_activity_play_to_uncover, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the Google Play Store to show more applications from the developer.
     */
    fun moreApps() {
        FirebaseHelper.logButtonClick(this, "more_apps")
        startActivity(Intent(Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/dev?id=8228670503574649511".toUri()))
    }

    /**
     * Opens the Google Play Store to navigate to the second part of the game.
     */
    fun playPart2() {
        FirebaseHelper.logButtonClick(this, "play_part_2")
        startActivity(Intent(Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=com.batodev.jigsawpuzzlecuties3".toUri()))
    }
}
