package com.assistivetouch.custom

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            processAndSaveImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgPreview = findViewById(R.id.imgPreview)
        val btnPickImage: Button = findViewById(R.id.btnPickImage)
        val btnStartService: Button = findViewById(R.id.btnStartService)
        val btnStopService: Button = findViewById(R.id.btnStopService)

        loadSavedPreview()
        checkAndRequestAllPermissions()

        btnPickImage.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnStartService.setOnClickListener {
            if (checkAndRequestAllPermissions()) {
                startService(Intent(this, FloatingService::class.java))
                Toast.makeText(this, "Ez-touch active", Toast.LENGTH_SHORT).show()
            }
        }

        btnStopService.setOnClickListener {
            stopService(Intent(this, FloatingService::class.java))
            Toast.makeText(this, "Ez-touch overlay disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestAllPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Grant Display Over Other Apps permission", Toast.LENGTH_SHORT).show()
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Grant Write System Settings permission", Toast.LENGTH_SHORT).show()
            return false
        }

        if (AssistiveAccessibilityService.instance == null) {
            openAccessibilitySettings(this)
            Toast.makeText(this, "Enable Ez-touch in Accessibility Settings", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    companion object {
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private fun processAndSaveImage(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val targetSize = 128
            var sampleSize = 1
            while (options.outWidth / sampleSize > targetSize || options.outHeight / sampleSize > targetSize) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val streamForDecode = contentResolver.openInputStream(uri)
            val scaledBitmap = BitmapFactory.decodeStream(streamForDecode, null, decodeOptions)
            streamForDecode?.close()

            if (scaledBitmap != null) {
                val finalSquareBitmap = Bitmap.createScaledBitmap(scaledBitmap, targetSize, targetSize, true)
                val circularBitmap = getCircularBitmap(finalSquareBitmap)

                // Save circular version for the floating overlay ball
                val circularFile = File(filesDir, "custom_ball.png")
                FileOutputStream(circularFile).use { out ->
                    circularBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // Save square version for the main app UI preview
                val squareFile = File(filesDir, "preview_square.png")
                FileOutputStream(squareFile).use { out ->
                    finalSquareBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // Display original square image in main activity UI without starting service
                imgPreview.setImageBitmap(finalSquareBitmap)
                Toast.makeText(this, "Floating photo saved!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to process image.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, bitmap.width / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    private fun loadSavedPreview() {
        val file = File(filesDir, "preview_square.png")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imgPreview.setImageBitmap(bitmap)
        }
    }
}