package com.example.cs442katie

import android.app.AlertDialog
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import android.util.Pair

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
        private const val faceThreshold = 1.24f

        private var assetManager: AssetManager? = null
        private var appContext: Context? = null

        private var faceFeat: HashMap<String, FloatArray> = HashMap()
        private val mapKey = "Face Feature"

        lateinit var faceDetector: com.google.android.gms.vision.face.FaceDetector

        // options for model interpreter
        private val tfliteOptions = Interpreter.Options()
        // tflite graph
        private var tflite: Interpreter? = null

        private var modelName: String? = null

        fun setup(context: Context, manager: AssetManager, modelFileName: String) {
            if (modelName != null && modelFileName == modelName) return
            assetManager = manager
            appContext = context
            //initialize graph and labels
            try {
                tflite = Interpreter(loadModelFile(modelFileName), tfliteOptions)
                modelName = modelFileName
                Log.e("tflite", "Model loaded.")
                loadMap(context)
            } catch (ex: Exception) {
                ex.printStackTrace()
                Log.e("tflite", "Fail to load model.")
            }
            faceDetector = com.google.android.gms.vision.face.FaceDetector.Builder(context).setTrackingEnabled(false).build()
            if (!faceDetector.isOperational) {
                AlertDialog.Builder(context).setMessage("Could not set up the face detector!").show()
                return
            }
        }

        fun addFaceBitmap(faceBitmap: Bitmap, name: String): FloatArray {
            val resizedBitmap = getResizedBitmap(faceBitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y)
            val feat = Array(1) { FloatArray(512) }
            val startTime = System.nanoTime()
            tflite!!.run(convertBitmapToByteBuffer(resizedBitmap), feat)
            val endTime = System.nanoTime()
            Log.e("inference done", (endTime - startTime).toString() + "")
            Log.e("tflite", "face added for $name")
            // for(int i=0; i<feat[0].length; i++) Log.e("arr: ", feat[0][i] + "");
            faceFeat[name] = normalize(feat[0])
            saveMap(appContext!!)
            return faceFeat[name]!!
        }

        fun compareFaceBitmap(faceBitmap: Bitmap, baseFaceFeat: FloatArray): Boolean {
            val resizedBitmap = getResizedBitmap(faceBitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y)
            val curFeat = Array(1) { FloatArray(512) }
            val startTime = System.nanoTime()
            tflite!!.run(convertBitmapToByteBuffer(resizedBitmap), curFeat)
            val endTime = System.nanoTime()
            Log.e("inference done", (endTime - startTime).toString() + "")
            curFeat[0] = normalize(curFeat[0])

            val diff = getFaceDiff(curFeat[0], baseFaceFeat)
            return diff < faceThreshold
        }

        fun recognizeFaceBitmap(faceBitmap: Bitmap): Pair<String, Float>? {
            val resizedBitmap = getResizedBitmap(faceBitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y)
            val curFeat = Array(1) { FloatArray(512) }
            val startTime = System.nanoTime()
            tflite!!.run(convertBitmapToByteBuffer(resizedBitmap), curFeat)
            val endTime = System.nanoTime()
            Log.e("inference done", (endTime - startTime).toString() + "")
            curFeat[0] = normalize(curFeat[0])

            var best: Pair<String, Float>? = null
            for ((key, value) in faceFeat) {
                val diff = getFaceDiff(curFeat[0], value)
                if (best == null || best.second > diff) best = Pair.create(key, diff)
            }
            return best
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
        fun getResizedBitmap(bm: Bitmap, newWidth: Int = DIM_IMG_SIZE_X, newHeight: Int = DIM_IMG_SIZE_Y): Bitmap {
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

        // converts bitmap to byte array which is passed in the tflite graph
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

        /**
         * Save and get HashMap in SharedPreference
         */

        fun saveMap(context: Context) {
            val pSharedPref = context.getSharedPreferences(
                "MyVariables",
                Context.MODE_PRIVATE
            )
            if (pSharedPref != null) {
                val jsonObject = JSONObject(faceFeat as Map<*, *>)
                val jsonString = jsonObject.toString()
                val editor = pSharedPref.edit()
                editor.remove(mapKey).apply()
                editor.putString(mapKey, jsonString)
                editor.commit()
                Log.e("tflite", "face feat saved")
            }
        }

        fun loadMap(context: Context) {
            val pSharedPref = context.getSharedPreferences(
                "MyVariables",
                Context.MODE_PRIVATE
            )
            try {
                faceFeat = HashMap()
                if (pSharedPref != null) {
                    val jsonString =
                        pSharedPref.getString(mapKey, JSONObject().toString()) ?: return
                    val jsonObject = JSONObject(jsonString)
                    val keysItr = jsonObject.keys()
                    while (keysItr.hasNext()) {
                        val key = keysItr.next()
                        faceFeat[key] = JSONArrayToFloatArray(jsonObject.getJSONArray(key))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        private fun JSONArrayToFloatArray(arr: JSONArray): FloatArray {
            val ans = FloatArray(arr.length())
            try {
                for (i in 0 until arr.length()) ans[i] = arr.getDouble(i).toFloat()
            } catch (e: Exception) {
            }

            return ans
        }

        fun clearMap() {
            faceFeat.clear()
            saveMap(appContext!!)
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
