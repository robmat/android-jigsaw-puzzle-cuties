package com.batodev.jigsawpuzzlecuties.helpers

/**
 * A data class for storing the application settings.
 */
class Settings {
    /**
     * A mutable list of strings representing the file names of uncovered pictures.
     */
    var uncoveredPics: MutableList<String> = ArrayList()
    /**
     * The index of the last seen picture in the gallery.
     */
    var lastSeenPic = 0
    /**
     * A counter for tracking ad display frequency.
     */
    var addCounter = 0
    /**
     * The frequency at which ads should be displayed (e.g., every X picture views).
     */
    var displayAddEveryXPicView = 2
    /**
     * A boolean indicating whether the puzzle grid should be shown in the background.
     */
    var showGridInBackgroundOfThePuzzle = true
    /**
     * The last set custom height for the puzzle difficulty.
     */
    var lastSetDifficultyCustomHeight = 5
    /**
     * The last set custom width for the puzzle difficulty.
     */
    var lastSetDifficultyCustomWidth = 3
    /**
     * A boolean indicating whether the image should be shown in the background of the puzzle.
     */
    var showImageInBackgroundOfThePuzzle = true
    /**
     * A boolean indicating whether sounds should be played in the application.
     */
    var playSounds = true
    /**
     * A mutable map storing high scores for different puzzle difficulties.
     * The key is the difficulty (e.g., "3x5") and the value is a list of high score strings (e.g., "MM:SS - YYYY-MM-DD HH:MM").
     */
    var highscores: MutableMap<String, MutableList<String>> = mutableMapOf()
}
