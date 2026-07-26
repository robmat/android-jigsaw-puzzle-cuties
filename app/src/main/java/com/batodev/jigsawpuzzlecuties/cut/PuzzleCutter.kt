package com.batodev.jigsawpuzzlecuties.cut

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import com.batodev.jigsawpuzzlecuties.helpers.FirebaseHelper
import com.batodev.jigsawpuzzlecuties.logic.PuzzleProgressListener
import com.batodev.jigsawpuzzlecuties.view.PuzzlePiece
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

/**
 * The parameters required to cut a source image into puzzle pieces.
 * @param sourceImage The {@link Bitmap} of the original image to be cut.
 * @param rows The number of rows for the puzzle grid.
 * @param cols The number of columns for the puzzle grid.
 * @param svgString The SVG string defining the puzzle piece shapes.
 * @param imageView The {@link ImageView} where the puzzle pieces will be displayed.
 * @param puzzleProgressListener A listener to report progress updates and completion.
 * @param pieces A list of {@link PuzzlePiece} objects to populate with the cut bitmaps.
 */
data class PuzzleCutRequest(
    val sourceImage: Bitmap,
    val rows: Int,
    val cols: Int,
    val svgString: String?,
    val imageView: ImageView,
    val puzzleProgressListener: PuzzleProgressListener,
    val pieces: List<PuzzlePiece>,
)

/**
 * An object for cutting the puzzle pieces from the source image.
 */
object PuzzleCutter {
    private const val AWAIT_TERMINATION_HOURS = 1L
    private val numProcessors = Runtime.getRuntime().availableProcessors()

    /**
     * Cuts the source image into puzzle pieces based on the provided SVG string.
     * This operation is performed asynchronously using a fixed thread pool.
     * @param request The {@link PuzzleCutRequest} describing the image, grid and destination pieces.
     * @return A list of {@link Bitmap} objects, each representing a cut puzzle piece.
     * @throws SVGParseException if the provided SVG string is invalid.
     */
    @Throws(SVGParseException::class)
    fun cut(request: PuzzleCutRequest): List<Bitmap> {
        val result: MutableList<Bitmap> = ArrayList()
        val startTime = System.currentTimeMillis()
        val svg = SVG.getFromString(request.svgString)
        val width = request.sourceImage.width
        val height = request.sourceImage.height
        val puzzleGridBitmap = createBitmap(width, height)
        val puzzleGridCanvas = Canvas(puzzleGridBitmap)
        val whiteFill = Paint()
        whiteFill.style = Paint.Style.FILL
        whiteFill.color = Color.WHITE
        puzzleGridCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), whiteFill)
        svg.renderToCanvas(puzzleGridCanvas)
        val executor = Executors.newFixedThreadPool(numProcessors)
        val cuttingContext = CuttingContext(
            sourceImage = request.sourceImage,
            puzzleGridBitmap = puzzleGridBitmap,
            output = PieceOutput(request.imageView, request.puzzleProgressListener),
            tracking = ProgressTracking(result, request.rows * request.cols),
            startTime = startTime,
        )
        val puzzlesCenterPoints = divideImage(puzzleGridBitmap, request.rows, request.cols)
        var puzzleIndex = 0
        for (rowIndex in 0 until request.rows) {
            for (colIndex in 0 until request.cols) {
                val piece = request.pieces[puzzleIndex++]
                val puzzleCenter = puzzlesCenterPoints[rowIndex][colIndex]!!
                executor.submit(cutPieceJob(cuttingContext, puzzleCenter, piece))
                println("Filling target took: " + (System.currentTimeMillis() - startTime) + "ms")
            }
        }
        executor.shutdown()
        awaitCompletionAsync(executor, request.imageView, request.puzzleProgressListener)
        return result
    }

    private class PieceOutput(
        val imageView: ImageView,
        val puzzleProgressListener: PuzzleProgressListener,
    )

    private class ProgressTracking(
        val result: MutableList<Bitmap>,
        val totalPieces: Int,
        val progressCounter: AtomicInteger = AtomicInteger(0),
    )

    private class CuttingContext(
        val sourceImage: Bitmap,
        val puzzleGridBitmap: Bitmap,
        val output: PieceOutput,
        val tracking: ProgressTracking,
        val startTime: Long,
    )

    private fun cutPieceJob(
        context: CuttingContext,
        puzzleCenter: Point,
        piece: PuzzlePiece,
    ) = Runnable {
        val reg = floodFill(context.puzzleGridBitmap, puzzleCenter.x, puzzleCenter.y)
        val regionWidth = reg.width
        val regionHeight = reg.height
        val regionMinX = reg.minX
        val regionMinY = reg.minY
        val puzzleBitmap = createBitmap(regionWidth + 1, regionHeight + 1)
        println("Flood fill took: " + (System.currentTimeMillis() - context.startTime) + "ms")
        reg.points.forEach(
            Consumer { (x1, y1): Point ->
                val rgbSource = context.sourceImage[x1, y1]
                puzzleBitmap[x1 - regionMinX, y1 - regionMinY] = rgbSource
            }
        )
        context.tracking.result.add(puzzleBitmap)
        context.output.puzzleProgressListener.postToHandler {
            piece.setImageBitmap(puzzleBitmap)
            piece.pieceWidth = regionWidth
            piece.pieceHeight = regionHeight
            piece.xCoord = regionMinX + context.output.imageView.left
            piece.yCoord = regionMinY + context.output.imageView.top
        }
        val progress = context.tracking.progressCounter.incrementAndGet()
        context.output.puzzleProgressListener.postToHandler {
            context.output.puzzleProgressListener.onProgressUpdate(progress, context.tracking.totalPieces)
        }
    }

    private fun awaitCompletionAsync(
        executor: java.util.concurrent.ExecutorService,
        imageView: ImageView,
        puzzleProgressListener: PuzzleProgressListener,
    ) {
        Thread {
            try {
                val terminated = executor.awaitTermination(AWAIT_TERMINATION_HOURS, TimeUnit.HOURS)
                println(terminated)
            } catch (e: InterruptedException) {
                FirebaseHelper.logException(imageView.context, "PuzzleCutter.cut", e.message)
                Thread.currentThread().interrupt()
            }
            puzzleProgressListener.postToHandler { puzzleProgressListener.onCuttingFinished() }
        }.start()
    }

    /**
     * Performs a flood fill algorithm to identify a connected region of white pixels in a bitmap.
     * Used to define the shape of a puzzle piece.
     * @param image The {@link Bitmap} to perform the flood fill on. This bitmap will be modified during the process.
     * @param startX The starting X-coordinate for the flood fill.
     * @param startY The starting Y-coordinate for the flood fill.
     * @return A {@link Region} object containing all points within the filled area.
     */
    private fun floodFill(image: Bitmap, startX: Int, startY: Int): Region {
        val reg = Region(ArrayList())
        val queue: Queue<Point> = ArrayDeque()
        val width = image.width
        val height = image.height

        // Bail out if the starting point is out of bounds or not the fill color
        if (isOutOfBounds(startX, startY, width, height) || image[startX, startY] != Color.WHITE) {
            return reg
        }

        // Add starting point to queue
        queue.add(Point(startX, startY))

        // Perform flood fill
        while (!queue.isEmpty()) {
            val point = queue.poll()!!
            val x = point.x
            val y = point.y

            // Check current pixel color
            if (image[x, y] != Color.WHITE) {
                continue
            }

            // Fill current pixel with fill color
            image[x, y] = Color.GREEN
            reg.points.add(Point(x, y))

            // Add neighboring pixels to queue
            if (x > 0) {
                queue.add(Point(x - 1, y))
            }
            if (x < width - 1) {
                queue.add(Point(x + 1, y))
            }
            if (y > 0) {
                queue.add(Point(x, y - 1))
            }
            if (y < height - 1) {
                queue.add(Point(x, y + 1))
            }
        }
        return reg
    }

    private fun isOutOfBounds(x: Int, y: Int, width: Int, height: Int): Boolean =
        x < 0 || y < 0 || x >= width || y >= height

    /**
     * Divides an image into a grid and calculates the center point of each cell.
     * @param image The {@link Bitmap} to divide.
     * @param rows The number of rows in the grid.
     * @param cols The number of columns in the grid.
     * @return A 2D array of {@link Point} objects, where each point represents the center of a grid cell.
     */
    private fun divideImage(image: Bitmap, rows: Int, cols: Int): Array<Array<Point?>> {
        val width = image.width
        val height = image.height

        // Calculate the width and height of each cell
        val cellWidth = width / cols
        val cellHeight = height / rows
        val cellCenters = Array(rows) { arrayOfNulls<Point>(cols) }

        // Loop through each cell and find its center
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                // Calculate the coordinates of the cell center
                val centerX = j * cellWidth + cellWidth / 2
                val centerY = i * cellHeight + cellHeight / 2
                cellCenters[i][j] = Point(centerX, centerY)
            }
        }
        return cellCenters
    }

    /**
     * Represents a point in 2D space with integer coordinates.
     * @param x The X-coordinate of the point.
     * @param y The Y-coordinate of the point.
     */
    internal class Point(var x: Int, var y: Int) {
        /**
         * Returns the X-coordinate of the point.
         * This is a component function for destructuring declarations.
         * @return The X-coordinate.
         */
        operator fun component1(): Int {
            return x
        }

        /**
         * Returns the Y-coordinate of the point.
         * This is a component function for destructuring declarations.
         * @return The Y-coordinate.
         */
        operator fun component2(): Int {
            return y
        }
    }

    /**
     * Represents a region defined by a collection of {@link Point} objects.
     * Provides methods to calculate the bounding box (min/max X/Y, width, height) of the region.
     * @param points The mutable collection of points that define the region.
     */
    internal class Region(val points: MutableCollection<Point>) {
        /**
         * Gets the maximum X-coordinate among all points in the region.
         * @return The maximum X-coordinate.
         */
        private val maxX: Int
            get() = points.stream().map(Point::x).max { obj: Int, anotherInteger: Int? ->
                obj.compareTo(
                    anotherInteger!!
                )
            }.orElse(0)

        /**
         * Gets the minimum X-coordinate among all points in the region.
         * @return The minimum X-coordinate.
         */
        val minX: Int
            get() = points.stream().map(Point::x).min { obj: Int, anotherInteger: Int? ->
                obj.compareTo(
                    anotherInteger!!
                )
            }.orElse(0)

        /**
         * Gets the maximum Y-coordinate among all points in the region.
         * @return The maximum Y-coordinate.
         */
        private val maxY: Int
            get() = points.stream().map(Point::y).max { obj: Int, anotherInteger: Int? ->
                obj.compareTo(
                    anotherInteger!!
                )
            }.orElse(0)

        /**
         * Gets the minimum Y-coordinate among all points in the region.
         * @return The minimum Y-coordinate.
         */
        val minY: Int
            get() = points.stream().map(Point::y).min { obj: Int, anotherInteger: Int? ->
                obj.compareTo(
                    anotherInteger!!
                )
            }.orElse(0)

        /**
         * Calculates the width of the bounding box of the region.
         * @return The width of the region.
         */
        val width: Int
            get() = maxX - minX

        /**
         * Calculates the height of the bounding box of the region.
         * @return The height of the region.
         */
        val height: Int
            get() = maxY - minY
    }
}
