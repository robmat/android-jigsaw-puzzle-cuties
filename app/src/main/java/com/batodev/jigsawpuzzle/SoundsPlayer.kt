package com.batodev.jigsawpuzzlecuties

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity

object SoundsPlayer {
    fun play(res : Int, activity: AppCompatActivity) {
        if (SettingsHelper.load(activity).playSounds) {
            val mp = MediaPlayer.create(activity, res)
            mp.setOnCompletionListener { mp.release() }
            mp.start()
        }
    }
}
