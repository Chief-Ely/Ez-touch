package com.assistivetouch.custom

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

        btnPickImage.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnStartService.setOnClickListener {
            checkPermissionsAndStart()
        }

        btnStopService.setOnClickListener {
            stopService(Intent(this, FloatingService::class.java))
            Toast.makeText(this, "Assistive Touch overlay disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Please grant Overlay permission", Toast.LENGTH_LONG).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
            startActivity(intent)
            Toast.makeText(this, "Please grant Write Settings permission for Auto-Rotate control", Toast.LENGTH_LONG).show()
            return
        }

        if (AssistiveAccessibilityService.instance == null) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Please enable Ez-Touch Accessibility Service for Screenshot and Lock actions", Toast.LENGTH_LONG).show()
            return
        }

        startService(Intent(this, FloatingService::class.java))
        Toast.makeText(this, "Assistive Touch active", Toast.LENGTH_SHORT).show()
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
                val finalBitmap = Bitmap.createScaledBitmap(scaledBitmap, targetSize, targetSize, true)
                val circularBitmap = getCircularBitmap(finalBitmap)

                val file = File(filesDir, "custom_ball.png")
                FileOutputStream(file).use { out ->
                    circularBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                imgPreview.setImageBitmap(circularBitmap)
                Toast.makeText(this, "Floating photo saved!", Toast.LENGTH_SHORT).show()

                stopService(Intent(this, FloatingService::class.java))
                startService(Intent(this, FloatingService::class.java))
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
        val file = File(filesDir, "custom_ball.png")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imgPreview.setImageBitmap(bitmap)
        }
    }
}