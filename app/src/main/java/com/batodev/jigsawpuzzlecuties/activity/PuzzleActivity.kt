package com.batodev.jigsawpuzzlecuties.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.batodev.jigsawpuzzlecuties.R
import com.batodev.jigsawpuzzlecuties.helpers.AdHelper
import com.batodev.jigsawpuzzlecuties.helpers.AppRatingHelper
import com.batodev.jigsawpuzzlecuties.helpers.FirebaseHelper
import com.batodev.jigsawpuzzlecuties.helpers.NeonBtnOnPressChangeLook
import com.batodev.jigsawpuzzlecuties.helpers.Settings
import com.batodev.jigsawpuzzlecuties.helpers.SettingsHelper
import com.batodev.jigsawpuzzlecuties.logic.ImageLoader
import com.batodev.jigsawpuzzlecuties.logic.PuzzleGameManager
import com.batodev.jigsawpuzzlecuties.logic.PuzzleProgressListener
import com.batodev.jigsawpuzzlecuties.logic.Stopwatch
import com.bumptech.glide.Glide
import com.otaliastudios.zoom.ZoomLayout
import com.smb.glowbutton.NeonButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The main activity for the puzzle game.
 */
class PuzzleActivity : AppCompatActivity(), PuzzleProgressListener {
    private var imageFileName: String? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val rateHelper: AppRatingHelper = AppRatingHelper(this)
    private lateinit var stopwatch: Stopwatch
    private lateinit var puzzleGameManager: PuzzleGameManager

    /**
     * Called when the activity is first created.
     * Initializes the UI, loads settings, and sets up event listeners.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_puzzle)
        FirebaseHelper.logScreenView(this, "PuzzleActivity")

        val windowInsetsController =
            WindowCompat.getInsetsController(this.window, this.window.decorView)
        windowInsetsController.let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val stopWatchText = findViewById<TextView>(R.id.stopwatchText)
        stopWatchText.bringToFront()
        stopwatch = Stopwatch(stopWatchText)

        val layout = findViewById<RelativeLayout>(R.id.layout)
        val zoomableLayout = findViewById<ZoomLayout>(R.id.zoomableLayout)
        val imageView = findViewById<ImageView>(R.id.imageView)
        val settings = SettingsHelper.load(this)

        puzzleGameManager =
            PuzzleGameManager(this, layout, imageView, zoomableLayout, settings, this)

        val displayMetrics = resources.displayMetrics
        val params = layout.layoutParams
        params.width = displayMetrics.widthPixels
        params.height = displayMetrics.heightPixels
        layout.layoutParams = params
        layout.x = 0f
        layout.y = 0f

        imageFileName = intent.getStringExtra("assetName")
        val puzzlesWidth = settings.lastSetDifficultyCustomWidth
        val puzzlesHeight = settings.lastSetDifficultyCustomHeight

        imageView.post {
            val imageLoader = ImageLoader(imageView)
            val bitmap = if (imageFileName != null) {
                imageLoader.setPicFromAsset(imageFileName!!, assets)
            } else {
                val photoPath = File(File(filesDir, "camera_images"), "temp.jpg").toString()
                imageLoader.setPicFromPath(photoPath)
            }
            puzzleGameManager.createPuzzle(bitmap, puzzlesWidth, puzzlesHeight)
        }

        findViewById<NeonButton>(R.id.puzzle_activity_play_again).let {
            it.setOnClickListener {
                FirebaseHelper.logButtonClick(this, "play_again")
                finish()
            }
            it.visibility = View.GONE
            it.setOnTouchListener { view, event ->
                NeonBtnOnPressChangeLook.neonBtnOnPressChangeLook(
                    view,
                    event,
                    this@PuzzleActivity
                )
                true
            }
        }
        rateHelper.requestReview()
    }

    /**
     * Callback for puzzle cutting progress updates.
     * Updates the progress bar and hides it when cutting is complete, then starts the stopwatch.
     * @param progress The current progress value.
     * @param max The maximum progress value.
     */
    override fun onProgressUpdate(progress: Int, max: Int) {
        handler.post {
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.max = max
            progressBar.progress = progress
            if (progress == max) {
                progressBar.visibility = View.GONE
                findViewById<TextView>(R.id.progressText).visibility = View.GONE
                stopwatch.start()
            }
        }
    }

    /**
     * Callback indicating that the puzzle cutting process has finished.
     * Triggers the scattering of puzzle pieces on the game board.
     */
    override fun onCuttingFinished() {
        puzzleGameManager.scatterPieces()
    }

    /**
     * Handles the game over state.
     * Displays confetti, adds the uncovered image to the gallery, stops the stopwatch,
     * plays a win sound, shows the play again button, and displays an ad.
     * Also updates and shows high scores.
     * @see AdHelper
     * @see SettingsHelper
     * @see Stopwatch
     * @see PuzzleGameManager
     */
    @SuppressLint("ClickableViewAccessibility")
    fun onGameOver() {
        FirebaseHelper.logEvent(this, "game_over")
        val konfetti = findViewById<ImageView>(R.id.konfettiView)
        Glide.with(konfetti).asGif().load(R.drawable.confetti2).into(konfetti)
        konfetti.visibility = View.VISIBLE
        val settings = SettingsHelper.load(this)
        imageFileName?.let {
            if (!settings.uncoveredPics.contains(it)) {
                settings.uncoveredPics.add(it)
            }
            SettingsHelper.save(this, settings)
            Toast.makeText(this, R.string.image_added_to_gallery, Toast.LENGTH_SHORT).show()
        }
        stopwatch.stop()
        puzzleGameManager.playWinSound()
        findViewById<NeonButton>(R.id.puzzle_activity_play_again).let {
            it.visibility = View.VISIBLE
            it.setOnTouchListener { view, event ->
                NeonBtnOnPressChangeLook.neonBtnOnPressChangeLook(view, event, this@PuzzleActivity)
                true
            }
        }
        AdHelper.showAd(this)

        val elapsedTime = stopwatch.elapsedTime
        val difficultyKey =
            "${settings.lastSetDifficultyCustomWidth}x${settings.lastSetDifficultyCustomHeight}"
        updateAndShowHighScores(elapsedTime, difficultyKey, settings)
    }

    /**
     * Updates the high scores for a given difficulty and displays the high score popup.
     * If the new score is among the top 10, a congratulatory toast is shown.
     * @param newTime The elapsed time for the current puzzle solution in seconds.
     * @param difficultyKey A string representing the puzzle difficulty (e.g., "3x5").
     * @param settings The current {@link Settings} object containing high scores.
     * @see Settings
     * @see SettingsHelper
     */
    private fun updateAndShowHighScores(newTime: Int, difficultyKey: String, settings: Settings) {
        val highScores = settings.highscores.getOrPut(difficultyKey) { mutableListOf() }

        val currentScoreInSeconds = newTime
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val newScoreString = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            currentScoreInSeconds / 60,
            currentScoreInSeconds % 60
        ) +
                " - " + dateFormat.format(Date())

        highScores.add(newScoreString)

        // Sort by time (first part of the string)
        highScores.sortBy {
            val parts = it.split(" - ")
            val timeParts = parts[0].split(":")
            timeParts[0].toInt() * 60 + timeParts[1].toInt()
        }
        while (highScores.size > 10) {
            highScores.removeAt(10)
        }

        SettingsHelper.save(this, settings)

        showHighScorePopup(difficultyKey, highScores, highScores.indexOf(newScoreString))

        if (highScores.indexOf(newScoreString) <= 10 && highScores.indexOf(newScoreString) != -1) {
            FirebaseHelper.logEvent(this, "new_highscore")
            Toast.makeText(this, getString(R.string.congratulations_top_10), Toast.LENGTH_LONG)
                .show()
        }
    }

    /**
     * Displays the high score popup with the top 10 scores for a given difficulty.
     * The new score, if it made it into the top 10, is highlighted in bold.
     * @param difficultyKey A string representing the puzzle difficulty (e.g., "3x5").
     * @param highScores The list of high score strings to display.
     * @param newScoreIndex The index of the newly achieved score in the highScores list, or -1 if not in top 10.
     */
    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun showHighScorePopup(
        difficultyKey: String,
        highScores: MutableList<String>,
        newScoreIndex: Int,
    ) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.high_score_popup, null)

        val highScoreDifficulty = popupView.findViewById<TextView>(R.id.highScoreDifficulty)
        val highScoreListContainer =
            popupView.findViewById<LinearLayout>(R.id.highScoreListContainer)

        highScoreDifficulty.text = difficultyKey

        // Populate high scores
        for ((index, scoreString) in highScores.withIndex()) {
            val scoreTextView = TextView(this)
            scoreTextView.setTextColor(resources.getColor(R.color.white, null))
            scoreTextView.text = "${index + 1}. $scoreString"
            scoreTextView.textSize = 16f // Use 16f for sp
            if (index == newScoreIndex) {
                scoreTextView.setTypeface(null, Typeface.BOLD)
            }
            highScoreListContainer.addView(scoreTextView)
        }

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(popupView)
        builder.setCancelable(false)

        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        alertDialog.show()

        popupView.findViewById<NeonButton>(R.id.highScoreOkButton).let {
            it.setOnClickListener {
                FirebaseHelper.logButtonClick(this, "highscore_ok")
                alertDialog.dismiss()
            }
            it.setOnTouchListener { view, event ->
                NeonBtnOnPressChangeLook.neonBtnOnPressChangeLook(view, event, this@PuzzleActivity)
                true
            }
        }
    }

    /**
     * Posts a {@link Runnable} to the main thread's handler.
     * This is used for UI updates that need to be performed on the main thread.
     * @param r The {@link Runnable} to be executed.
     */
    override fun postToHandler(r: Runnable) {
        handler.post(r)
    }
}
