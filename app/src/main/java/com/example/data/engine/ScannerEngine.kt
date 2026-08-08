package com.example.data.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ScanFilterMode {
    ORIGINAL,
    GRAYSCALE,
    BLACK_WHITE,
    ENHANCE
}

class ScannerEngine {

    suspend fun applyFilter(
        context: Context,
        inputUri: Uri,
        filterMode: ScanFilterMode,
        rotationDegrees: Float = 0f
    ): File? = withContext(Dispatchers.IO) {
        try {
            var bitmap = context.contentResolver.openInputStream(inputUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@withContext null

            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            val processedBitmap = when (filterMode) {
                ScanFilterMode.ORIGINAL -> bitmap
                ScanFilterMode.GRAYSCALE -> applyGrayscaleFilter(bitmap)
                ScanFilterMode.BLACK_WHITE -> applyHighContrastBwFilter(bitmap)
                ScanFilterMode.ENHANCE -> applyMagicEnhanceFilter(bitmap)
            }

            val outputFile = File(context.cacheDir, "scan_filtered_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outputFile).use { out ->
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun applyGrayscaleFilter(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun applyHighContrastBwFilter(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint()
        // Contrast enhancement color matrix
        val contrast = 2.0f
        val scale = contrast
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val cm = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        val grayCm = ColorMatrix()
        grayCm.setSaturation(0f)
        grayCm.postConcat(cm)

        paint.colorFilter = ColorMatrixColorFilter(grayCm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun applyMagicEnhanceFilter(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint()
        val cm = ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, 10f,
            0f, 1.2f, 0f, 0f, 10f,
            0f, 0f, 1.2f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }
}
