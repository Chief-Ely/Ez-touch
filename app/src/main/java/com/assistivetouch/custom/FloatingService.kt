package com.assistivetouch.custom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingBallView: ImageView
    private lateinit var ballLayoutParams: WindowManager.LayoutParams

    private var activeMenuContainer: View? = null
    private var isFlashlightOn = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundService()
        setupFloatingBall()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "assistive_touch_service"
            val channel = NotificationChannel(
                channelId,
                "Ez-touch Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)

            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Ez-touch Active")
                .setContentText("Ez-touch overlay is running")
                .setSmallIcon(R.drawable.ic_app_logo)
                .build()

            startForeground(1, notification)
        }
    }

    private fun setupFloatingBall() {
        floatingBallView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        loadCustomImage()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val ballSizePx = (48 * resources.displayMetrics.density).toInt()

        ballLayoutParams = WindowManager.LayoutParams(
            ballSizePx,
            ballSizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        floatingBallView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = ballLayoutParams.x
                        initialY = ballLayoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        ballLayoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        ballLayoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingBallView, ballLayoutParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX < 10 && diffY < 10) {
                            if (activeMenuContainer != null) {
                                closeActiveMenu()
                            } else {
                                showMainMenu()
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingBallView, ballLayoutParams)
    }

    private fun loadCustomImage() {
        val file = File(filesDir, "custom_ball.png")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            floatingBallView.setImageBitmap(bitmap)
        } else {
            floatingBallView.setImageBitmap(getDownscaledCircularDefaultLogo())
        }
    }

    private fun getDownscaledCircularDefaultLogo(): Bitmap {
        val rawBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_app_logo)
        val targetSize = 128
        val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, targetSize, targetSize, true)

        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }
        val rect = Rect(0, 0, targetSize, targetSize)

        canvas.drawARGB(0, 0, 0, 0)
        paint.color = Color.BLACK
        canvas.drawCircle(targetSize / 2f, targetSize / 2f, targetSize / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaledBitmap, rect, rect, paint)

        return output
    }

    private fun showMainMenu() {
        closeActiveMenu()
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val menuView = inflater.inflate(R.layout.layout_floating_menu_main, null)

        setupOutsideTouchListener(menuView)

        val menuParams = getMenuLayoutParams()
        windowManager.addView(menuView, menuParams)
        activeMenuContainer = menuView

        menuView.apply {
            findViewById<View>(R.id.btnMenuSetting)?.setOnClickListener {
                showSettingsMenu()
            }
            findViewById<View>(R.id.btnMenuLock)?.setOnClickListener {
                closeActiveMenu()
                if (AssistiveAccessibilityService.instance != null) {
                    AssistiveAccessibilityService.instance?.performLock()
                } else {
                    MainActivity.openAccessibilitySettings(applicationContext)
                }
            }
            findViewById<View>(R.id.btnMenuScreenshot)?.setOnClickListener {
                closeActiveMenu()
                if (AssistiveAccessibilityService.instance != null) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        AssistiveAccessibilityService.instance?.performScreenshot()
                    }, 300)
                } else {
                    MainActivity.openAccessibilitySettings(applicationContext)
                }
            }
            findViewById<View>(R.id.btnMenuHome)?.setOnClickListener {
                closeActiveMenu()
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            }
            findViewById<View>(R.id.btnMenuMain)?.setOnClickListener {
                closeActiveMenu()
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }

    private fun showSettingsMenu() {
        closeActiveMenu()
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val menuView = inflater.inflate(R.layout.layout_floating_menu_settings, null)

        setupOutsideTouchListener(menuView)

        val menuParams = getMenuLayoutParams()
        windowManager.addView(menuView, menuParams)
        activeMenuContainer = menuView

        updateAllSettingsUI(menuView)

        menuView.apply {
            findViewById<View>(R.id.btnWifi)?.setOnClickListener {
                launchControlPanel(Settings.Panel.ACTION_WIFI)
                closeActiveMenu()
            }
            findViewById<View>(R.id.btnBluetooth)?.setOnClickListener {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                closeActiveMenu()
            }
            findViewById<View>(R.id.btnAutoRotate)?.setOnClickListener {
                toggleAutoRotate(menuView)
            }
            findViewById<View>(R.id.btnLocation)?.setOnClickListener {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                closeActiveMenu()
            }
            findViewById<View>(R.id.btnVolumeUp)?.setOnClickListener {
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            }
            findViewById<View>(R.id.btnVolumeDown)?.setOnClickListener {
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            }
            findViewById<View>(R.id.btnNormal)?.setOnClickListener {
                cycleRingerMode(menuView)
            }
            findViewById<View>(R.id.btnFlashlight)?.setOnClickListener {
                toggleFlashlight(menuView)
            }
            findViewById<View>(R.id.btnBackToMain)?.setOnClickListener {
                showMainMenu()
            }
        }
    }

    private fun launchControlPanel(action: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } else {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
    }

    private fun updateAllSettingsUI(view: View) {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val isWifiOn = wifiManager.isWifiEnabled
            highlightIcon(view.findViewById(R.id.imgWifiIcon), isWifiOn)
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val isBtOn = bluetoothAdapter?.isEnabled == true
            highlightIcon(view.findViewById(R.id.imgBluetoothIcon), isBtOn)
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            highlightIcon(view.findViewById(R.id.imgLocationIcon), isGpsOn)
        } catch (e: Exception) { e.printStackTrace() }

        updateRotationUI(view)
        updateTorchUI(view)
        updateRingerUI(view)
    }

    private fun updateTorchUI(view: View) {
        val torchIcon = view.findViewById<ImageView>(R.id.imgTorchIcon)
        if (isFlashlightOn) {
            torchIcon?.setImageResource(android.R.drawable.ic_menu_day)
            highlightIcon(torchIcon, true)
        } else {
            torchIcon?.setImageResource(android.R.drawable.ic_menu_compass)
            highlightIcon(torchIcon, false)
        }
    }

    private fun highlightIcon(imageView: ImageView?, active: Boolean) {
        if (active) {
            imageView?.setColorFilter(Color.WHITE)
            imageView?.alpha = 1.0f
        } else {
            imageView?.setColorFilter(Color.GRAY)
            imageView?.alpha = 0.5f
        }
    }

    private fun cycleRingerMode(view: View) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
            else -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
        }
        updateRingerUI(view)
    }

    private fun updateRingerUI(view: View) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val text = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "Normal"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            else -> "Ringer"
        }
        view.findViewById<TextView>(R.id.txtRingerLabel)?.text = text
    }

    private fun toggleAutoRotate(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, android.net.Uri.parse("package:$packageName")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            closeActiveMenu()
            return
        }

        val currentRotation = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
        val newRotation = if (currentRotation == 1) 0 else 1
        Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, newRotation)

        updateRotationUI(view)
    }

    private fun updateRotationUI(view: View) {
        val currentRotation = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
        val isRotateOn = currentRotation == 1
        val rotateIcon = view.findViewById<ImageView>(R.id.imgRotateIcon)
        val rotateLabel = view.findViewById<TextView>(R.id.txtRotationLabel)

        if (isRotateOn) {
            rotateLabel?.text = "Rotate"
            rotateIcon?.setImageResource(android.R.drawable.ic_menu_always_landscape_portrait)
            highlightIcon(rotateIcon, true)
        } else {
            rotateLabel?.text = "Portrait"
            rotateIcon?.setImageResource(android.R.drawable.ic_menu_sort_by_size)
            highlightIcon(rotateIcon, false)
        }
    }

    private fun setupOutsideTouchListener(view: View) {
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                closeActiveMenu()
                true
            } else {
                false
            }
        }
    }

    private fun toggleFlashlight(view: View) {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
            updateTorchUI(view)
        } catch (e: Exception) {
            Toast.makeText(this, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeActiveMenu() {
        activeMenuContainer?.let {
            windowManager.removeView(it)
            activeMenuContainer = null
        }
    }

    private fun getMenuLayoutParams(): WindowManager.LayoutParams {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingBallView.isInitialized) {
            windowManager.removeView(floatingBallView)
        }
        closeActiveMenu()
    }
}