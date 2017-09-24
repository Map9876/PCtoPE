package com.zhufuc.pctope.Adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

import com.zhufuc.pctope.Utils.CompressImage
import com.zhufuc.pctope.Utils.JsonFormatTool
import com.zhufuc.pctope.Utils.PackVersionDecisions

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.UUID

/**
 * Created by zhufu on 7/22/17.
 */

class Textures(path: File) {

    val path: String

    val name: String?
    val description: String?

    private val version: PackVersionDecisions

    init {
        this.path = path.path
        version = PackVersionDecisions(path)
        name = version.name
        description = version.description
    }

    fun IfIsResourcePack(testVersion: String): Boolean? {
        return version.getIfIsResourcePack(testVersion)
    }

    fun getVersion(): String {
        return version.packVersion
    }

    val icon: String?
        get() {
            val icon = File(path + "/pack_icon.png")
            return if (icon.exists()) {
                icon.path
            } else
                null
        }

    //EDITING
    class Edit(private val path: String) {

        private var intro: String? = null

        private val textures: Textures

        private fun makeSpace(i: Int): String {
            var spaces = ""
            for (j in 0..i) spaces += " "
            return spaces
        }

        interface CompressionProgressChangeListener {
            fun OnProgressChangeListener(whatsBeingCompressed: String?, isDone: Boolean)
        }

        private var compressionProgressChangeListener: CompressionProgressChangeListener? = null
        fun setOnCompressionProgressChangeListener(listener: CompressionProgressChangeListener) {
            this.compressionProgressChangeListener = listener
        }

        interface OnCrashListener {
            fun onCrash(e: String)
        }

        private var onCrashListener: OnCrashListener? = null
        fun setOnCrashListener(listener: OnCrashListener) {
            this.onCrashListener = listener
        }

        init {
            textures = Textures(File(path))
        }

        fun changeNameAndDescription(nameIndex: String, descriptionIndex: String) {
            readManifest()

            Log.i("Change Existed Pack", "Name Set=$nameIndex, Description Set=$descriptionIndex, Manifest Read=\n$intro")

            try {
                val `object` = JSONObject(intro)
                if (`object`.has("header")) {
                    val header = `object`.getJSONObject("header")
                    header.put("name", nameIndex)
                    `object`.put("header", header)
                    intro = `object`.toString()

                    header.put("description", descriptionIndex)
                    `object`.put("header", header)
                    intro = `object`.toString()
                } else
                    intro = overwriteManifest(nameIndex, descriptionIndex)
                if (`object`.has("modules")) {
                    val array = `object`.getJSONArray("modules")
                    val descriptionObj = array.getJSONObject(0)
                    descriptionObj.put("description", descriptionIndex)
                    `object`.put("modules", array)
                    intro = `object`.toString()
                } else
                    intro = overwriteManifest(nameIndex, descriptionIndex)
            } catch (e: JSONException) {
                intro = overwriteManifest(nameIndex, descriptionIndex)
            }

            writeResult(JsonFormatTool().formatJson(intro!!))
        }

        @Throws(IOException::class)
        fun iconEdit(icon: String) {
            val baos = ByteArrayOutputStream()
            val bitmap = BitmapFactory.decodeFile(icon)

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)

            var output: FileOutputStream? = null
            output = FileOutputStream(path + "/pack_icon.png")
            output.write(baos.toByteArray())
            output.close()
        }

        @Throws(IOException::class)
        fun iconEdit(bitmap: Bitmap) {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)

            var output: FileOutputStream? = null
            output = FileOutputStream(path + "/pack_icon.png")
            output.write(baos.toByteArray())
            output.close()
        }

        fun compressImages(compressFinalSize: Int) {
            if (compressFinalSize == 0)
                return
            Log.i("Pack Conversion", "Doing image compressions...")
            //get images
            val items = File(path + "/textures/items").listFiles()
            val blocks = File(path + "/textures/blocks").listFiles()
            if (items != null) {
                for (n in items)
                    doMainInCompressing(n, compressFinalSize)
            }
            if (blocks != null) {
                for (n in blocks)
                    doMainInCompressing(n, compressFinalSize)
            }
            compressionProgressChangeListener!!.OnProgressChangeListener(null, true)
        }

        private fun doMainInCompressing(n: File, compressFinalSize: Int) {
            if (n.isFile) {
                //Show progress
                compressionProgressChangeListener!!.OnProgressChangeListener(n.path, false)
                Log.d("compression", "Compressing " + n)

                val str = n.path
                if (str.substring(str.lastIndexOf("."), str.length) != ".png")
                    return

                val image = BitmapFactory.decodeFile(n.path)
                //get compressed bitmap
                var compressHeight = compressFinalSize
                var compressWidth = compressFinalSize
                if (image.width - image.height < -5) {
                    compressHeight = compressHeight * (image.width / compressFinalSize)
                } else if (image.height - image.width > 5) {
                    compressWidth = compressWidth * (image.height / compressFinalSize)
                }
                val compressed = CompressImage.getBitmap(image, compressHeight, compressWidth)

                if (compressed == null) {
                    onCrashListener!!.onCrash("Compressing Resources: could not compress " + n.path)
                }

                val baos = ByteArrayOutputStream()
                compressed!!.compress(Bitmap.CompressFormat.PNG, 100, baos)//png

                try {
                    val outputStream = FileOutputStream(n)
                    outputStream.write(baos.toByteArray())

                    outputStream.flush()
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }

        private fun writeResult(result: String) {
            try {
                val outputStream = FileOutputStream(path + "/manifest.json")
                outputStream.write(result.toByteArray())

                outputStream.flush()
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        private fun readManifest() {
            val manifest = File(path + "/manifest.json")
            if (intro != null)
                return

            if (manifest.exists()) {
                intro = manifest.readText(Charset.defaultCharset())
            }
        }

        fun overwriteManifest(name: String, description: String): String {

            val intro: String
            val out = JSONObject()
            val versionArray = JSONArray()
            try {
                versionArray.put(0)
                versionArray.put(0)
                versionArray.put(1)

                out.put("format_version", 1)
                val header = JSONObject()
                header.put("description", description)
                header.put("name", name)
                header.put("uuid", UUID.randomUUID().toString())
                header.put("version", versionArray)
                out.put("header", header)

                val modules = JSONArray()
                val modulesObjs = JSONObject()
                modulesObjs.put("description", description)
                modulesObjs.put("type", "resources")
                modulesObjs.put("uuid", UUID.randomUUID().toString())
                modulesObjs.put("version", versionArray)
                modules.put(modulesObjs)
                out.put("modules", modules)

            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return JsonFormatTool().formatJson(out.toString())

        }
    }

}