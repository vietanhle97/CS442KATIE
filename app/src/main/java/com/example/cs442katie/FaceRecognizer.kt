package com.example.cs442katie

import android.app.AlertDialog
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import android.util.Pair
import android.widget.Toast
import com.google.android.gms.vision.Frame

import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.Interpreter

import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.HashMap

class FaceRecognizer {
    companion object {
        const val DIM_IMG_SIZE_X = 112
        const val DIM_IMG_SIZE_Y = 112
        private const val faceThreshold = 1.05f

        private var assetManager: AssetManager? = null
        private var appContext: Context? = null

        lateinit var faceDetector: com.google.android.gms.vision.face.FaceDetector

        // options for model interpreter
        private val tfliteOptions = Interpreter.Options()
        // tflite graph
        private var tflite: Interpreter? = null

        private var modelName: String? = null

        fun setup(context: Context, manager: AssetManager, modelFileName: String): Boolean {
            if (modelName != null && modelFileName == modelName) return true
            assetManager = manager
            appContext = context
            //initialize graph and labels
            try {
                tflite = Interpreter(loadModelFile(modelFileName), tfliteOptions)
                modelName = modelFileName
                Log.e("tflite", "Model loaded.")
            } catch (ex: Exception) {
                ex.printStackTrace()
                Log.e("tflite", "Fail to load model.")
                return false
            }
            faceDetector = com.google.android.gms.vision.face.FaceDetector.Builder(context).setTrackingEnabled(false).build()
            if (!faceDetector.isOperational) return false
            return true
        }

        fun getFaceBitmap(frameImg: Bitmap): Bitmap? {
            val frame = Frame.Builder().setBitmap(frameImg).build()
            val faces = faceDetector.detect(frame)

            if (faces.size() == 0) return null
            val face = faces.valueAt(0)
            val faceCenter = floatArrayOf(face.position.x + face.width/2, face.position.y + face.height/2)

            val rotatedFaceImg = Bitmap.createBitmap(frameImg.width, frameImg.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(rotatedFaceImg)
            val rotateMatrix = Matrix()
            rotateMatrix.setTranslate(-faceCenter[0], -faceCenter[1])
            rotateMatrix.postRotate(face.eulerZ)
            rotateMatrix.postTranslate(faceCenter[0], faceCenter[1])
            canvas.drawBitmap(frameImg, rotateMatrix, null)

            var capturedFace =
                Bitmap.createBitmap(face.width.toInt(), face.height.toInt(), Bitmap.Config.ARGB_8888)
            val tempCanvas = Canvas(capturedFace)
            tempCanvas.drawBitmap(rotatedFaceImg, -face.position.x, -face.position.y, null)
            capturedFace = getResizedBitmap(capturedFace)
            return capturedFace
        }

        fun getFaceFeat(faceBitmap: Bitmap): FloatArray {
            val resizedBitmap = getResizedBitmap(faceBitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y)
            val feat = Array(1) { FloatArray(512) }
            tflite!!.run(convertBitmapToByteBuffer(resizedBitmap), feat)
            val endTime = System.nanoTime()
            return normalize(feat[0])
        }

        fun compareFace(faceBitmap: Bitmap, baseFaceFeat: FloatArray): Boolean {
            val resizedBitmap = getResizedBitmap(faceBitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y)
            val curFeat = Array(1) { FloatArray(512) }
            val startTime = System.nanoTime()
            tflite!!.run(convertBitmapToByteBuffer(resizedBitmap), curFeat)
            val endTime = System.nanoTime()
            Log.e("inference done", (endTime - startTime).toString() + "")
            curFeat[0] = normalize(curFeat[0])

            val diff = getFaceDiff(curFeat[0], baseFaceFeat)
            Log.e("Face difference", diff.toString())
            return diff < faceThreshold
        }

        private fun getFaceDiff(faceFeat1: FloatArray, faceFeat2: FloatArray): Float {
            var diff = 0f
            for (i in faceFeat1.indices)
                diff += (faceFeat1[i] - faceFeat2[i]) * (faceFeat1[i] - faceFeat2[i])
            return diff
        }

        private fun normalize(feat: FloatArray): FloatArray {
            var sumSquare = 0f
            for (i in feat.indices) sumSquare += feat[i] * feat[i]
            for (i in feat.indices) feat[i] /= Math.sqrt(sumSquare.toDouble()).toFloat()
            return feat
        }

        // loads tflite graph from file
        @Throws(IOException::class)
        fun loadModelFile(modelFileName: String): MappedByteBuffer {
            val fileDescriptor = assetManager!!.openFd(modelFileName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }

        // Resize bitmap to given dimensions
        private fun getResizedBitmap(bm: Bitmap, newWidth: Int = DIM_IMG_SIZE_X, newHeight: Int = DIM_IMG_SIZE_Y): Bitmap {
            val width = bm.width
            val height = bm.height
            if(width == newWidth && height == newHeight) return bm

            val scaleWidth = newWidth.toFloat() / width
            val scaleHeight = newHeight.toFloat() / height
            val matrix = Matrix()
            matrix.postScale(scaleWidth, scaleHeight)
            return Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false
            )
        }

        // Converts bitmap to byte array which is passed in the tflite graph
        private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
            val imgData = ByteBuffer.allocateDirect(4 * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * 3)
            imgData.order(ByteOrder.nativeOrder())
            val intValues = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)
            bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            // loop through all pixels
            var pixel = 0
            for (i in 0 until DIM_IMG_SIZE_X) {
                for (j in 0 until DIM_IMG_SIZE_Y) {
                    val `val` = intValues[pixel++]
                    imgData.putFloat((`val` and 0xFF).toFloat())
                    imgData.putFloat((`val` shr 8 and 0xFF).toFloat())
                    imgData.putFloat((`val` shr 16 and 0xFF).toFloat())
                }
            }
            return imgData
        }

        fun modifyOrientation(bitmap: Bitmap, image_absolute_path: String): Bitmap {
            var ei: ExifInterface? = null
            try {
                ei = ExifInterface(image_absolute_path)
            } catch (e: Exception) {
                return bitmap
            }

            val orientation =
                ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> return rotate(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> return rotate(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> return rotate(bitmap, 270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> return flip(bitmap, true, false)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> return flip(bitmap, false, true)
                else -> return bitmap
            }
        }

        private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(degrees)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        private fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
            val matrix = Matrix()
            matrix.preScale((if (horizontal) -1 else 1).toFloat(), (if (vertical) -1 else 1).toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }
}
