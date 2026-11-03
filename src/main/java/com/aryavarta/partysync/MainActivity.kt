package com.aryavarta.partysync

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.provider.OpenableColumns
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.*
import java.io.*
import java.net.*
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import kotlin.math.*

class PartyForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NaadSangam::PartyWakeLock").apply {
            setReferenceCounted(false)
            acquire()
        }
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiLock = wm.createWifiLock(lockMode, "NaadSangam::PartyWifiLock").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "naad_sangam_party_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NaadSangam Background Party Sync",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle("NaadSangam Party Sync Active")
            .setContentText("Zero-Echo Local Hotspot/Wi-Fi Audio Running in Background")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1001, notification)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class AryavartaSplashView(context: Context, private val onFinished: () -> Unit) : View(context) {
    private var progress = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private val ayodhyaX = 0.58f
    private val ayodhyaY = 0.44f

    init {
        textPaint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }

    fun startZoomAnimation() {
        progress = 0f
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000L
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    postDelayed({ onFinished() }, 280L)
                }
            })
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        canvas.drawColor(Color.parseColor("#140D06"))

        val currentScale = 4.5f - (3.5f * progress)
        val focusX = w * ayodhyaX
        val focusY = h * ayodhyaY

        canvas.save()
        val panFactor = 1f - progress
        canvas.translate((w / 2f - focusX) * panFactor, (h / 2f - focusY) * panFactor)
        canvas.scale(currentScale, currentScale, focusX, focusY)

        paint.shader = RadialGradient(
            w * 0.5f, h * 0.48f, max(w, h) * 0.55f,
            intArrayOf(Color.parseColor("#4A2F13"), Color.parseColor("#261607"), Color.parseColor("#120A04")),
            floatArrayOf(0f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        path.reset()
        path.moveTo(w * 0.20f, h * 0.30f)
        path.quadTo(w * 0.38f, h * 0.21f, w * 0.52f, h * 0.26f)
        path.quadTo(w * 0.70f, h * 0.31f, w * 0.84f, h * 0.40f)
        path.quadTo(w * 0.76f, h * 0.54f, w * 0.66f, h * 0.62f)
        path.quadTo(w * 0.56f, h * 0.78f, w * 0.49f, h * 0.86f)
        path.quadTo(w * 0.39f, h * 0.73f, w * 0.31f, h * 0.56f)
        path.quadTo(w * 0.20f, h * 0.52f, w * 0.24f, h * 0.44f)
        path.close()

        paint.color = Color.parseColor("#6E471B")
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)

        paint.color = Color.parseColor("#F5B041")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f / currentScale.coerceAtLeast(1f)
        canvas.drawPath(path, paint)

        paint.color = Color.parseColor("#5DADE2")
        paint.strokeWidth = 2.4f / currentScale.coerceAtLeast(1f)
        val riverPath = Path().apply {
            moveTo(w * 0.47f, h * 0.31f)
            quadTo(w * 0.54f, h * 0.41f, w * ayodhyaX, h * ayodhyaY)
            quadTo(w * 0.68f, h * 0.48f, w * 0.75f, h * 0.55f)
            moveTo(w * 0.44f, h * 0.27f)
            quadTo(w * 0.30f, h * 0.36f, w * 0.24f, h * 0.47f)
        }
        canvas.drawPath(riverPath, paint)

        val janapadas = listOf(
            Triple("तक्षशिला (Takshashila)", 0.31f, 0.31f),
            Triple("इंद्रप्रस्थ (Indraprastha)", 0.46f, 0.39f),
            Triple("मथुरा (Mathura)", 0.48f, 0.44f),
            Triple("काशी (Kashi)", 0.62f, 0.48f),
            Triple("पाटलिपुत्र (Magadha)", 0.68f, 0.47f),
            Triple("उज्जयिनी (Avanti)", 0.43f, 0.55f),
            Triple("द्वारका (Dwarka)", 0.25f, 0.51f),
            Triple("हिमवत् (Himalaya)", 0.56f, 0.28f)
        )

        val cityAlpha = ((progress - 0.15f) * 1.4f).coerceIn(0f, 1f)
        if (cityAlpha > 0f) {
            paint.style = Paint.Style.FILL
            textPaint.color = Color.argb((cityAlpha * 230).toInt(), 255, 235, 190)
            textPaint.textSize = 24f
            textPaint.textAlign = Paint.Align.CENTER
            for ((name, cx, cy) in janapadas) {
                paint.color = Color.argb((cityAlpha * 255).toInt(), 245, 176, 65)
                canvas.drawCircle(w * cx, h * cy, 6f, paint)
                canvas.drawText(name, w * cx, h * cy - 12f, textPaint)
            }
        }

        val pulseRadius = (18f + 14f * sin(progress * PI * 4).toFloat()) / currentScale.coerceAtLeast(1f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f / currentScale.coerceAtLeast(1f)
        paint.color = Color.parseColor("#FFD700")
        canvas.drawCircle(focusX, focusY, pulseRadius * 1.8f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FF6F00")
        canvas.drawCircle(focusX, focusY, 9f / currentScale.coerceAtLeast(1f), paint)
        paint.color = Color.parseColor("#FFF59D")
        canvas.drawCircle(focusX, focusY, 4.5f / currentScale.coerceAtLeast(1f), paint)

        textPaint.color = Color.parseColor("#FFD700")
        textPaint.textSize = (30f / currentScale.coerceAtLeast(1f)).coerceAtLeast(14f)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("★ श्री अयोध्या धाम (कोशल)", focusX, focusY - (18f / currentScale.coerceAtLeast(1f)), textPaint)

        canvas.restore()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = Color.parseColor("#D4AC0D")
        canvas.drawRect(24f, 24f, w - 24f, h - 24f, paint)

        textPaint.color = Color.parseColor("#FDEBD0")
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 46f
        val titleAlpha = (progress * 1.5f).coerceIn(0.2f, 1f)
        textPaint.alpha = (titleAlpha * 255).toInt()
        canvas.drawText("संपूर्ण आर्यावर्त • नाद-संगम", w * 0.5f, h * 0.11f, textPaint)

        textPaint.textSize = 28f
        textPaint.color = Color.parseColor("#F5B041")
        val subtitle = if (progress < 0.45f) {
            "केंद्र: अयोध्या नगरी (सरयू तट)..."
        } else {
            "अयोध्या से संपूर्ण आर्यावर्त • Zero-Echo Party Sync"
        }
        canvas.drawText(subtitle, w * 0.5f, h * 0.91f, textPaint)
    }
}

class MainActivity : Activity() {

    companion object {
        private const val REQ_PICK_AUDIO = 2001
        private const val HTTP_PORT = 8080
        private const val SYNC_TCP_PORT = 8081
        private const val DISCOVERY_UDP_PORT = 8889
    }

    private lateinit var rootFrame: FrameLayout
    private lateinit var mainContainer: LinearLayout
    private lateinit var strobeOverlay: View
    private lateinit var statusBadge: TextView
    private lateinit var modeCapacityBadge: TextView
    private lateinit var ipInfoText: TextView
    private lateinit var trackTitleText: TextView
    private lateinit var connectedClientsText: TextView
    private lateinit var syncMetricText: TextView
    private lateinit var hostCodeInput: EditText
    private lateinit var discoveredHostBtn: Button
    private lateinit var playPauseBtn: Button
    private lateinit var calibrationLabel: TextView

    private val uiHandler = Handler(Looper.getMainLooper())
    private val threadPool = Executors.newCachedThreadPool()

    private var mediaPlayer: MediaPlayer? = null
    private var isPlayerPrepared = false
    private var isHostMode = false
    private var isClientConnected = false
    private var sharedAudioFile: File? = null
    private var currentTrackName = "No Track Selected"
    private var trackSessionId = System.currentTimeMillis()

    @Volatile private var clockOffsetMs: Long = 0L
    @Volatile private var minRttMs: Long = 999L
    @Volatile private var btHardwareDelayMs: Int = 0
    @Volatile private var audioChannelMode: Int = 0

    private var httpServerSocket: ServerSocket? = null
    private var syncServerSocket: ServerSocket? = null
    private var discoverySocket: DatagramSocket? = null
    private val connectedClients = CopyOnWriteArrayList<ClientSession>()
    private var clientSocket: Socket? = null
    private var clientWriter: PrintWriter? = null
    private var discoveredHostIp: String? = null

    inner class ClientSession(val socket: Socket, val writer: PrintWriter, val ip: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 101)
        }

        rootFrame = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0B0F19"))
        }

        buildDashboardUi()
        setContentView(rootFrame)

        playAryavartaSplashIntro()
        startForegroundPartyService()
        startUdpDiscoveryListener()
    }

    private fun startForegroundPartyService() {
        try {
            val serviceIntent = Intent(this, PartyForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (_: Exception) {}
    }

    private fun playAryavartaSplashIntro() {
        val splash = AryavartaSplashView(this) {
            rootFrame.removeViewAt(rootFrame.childCount - 1)
        }
        rootFrame.addView(
            splash,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        splash.post { splash.startZoomAnimation() }
    }

    private fun buildDashboardUi() {
        val scroll = ScrollView(this).apply { isFillViewport = true }
        mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(28))
        }
        scroll.addView(mainContainer)
        rootFrame.addView(scroll)

        strobeOverlay = View(this).apply {
            setBackgroundColor(Color.parseColor("#66FF6F00"))
            alpha = 0f
            isClickable = false
        }
        rootFrame.addView(strobeOverlay)

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = "नाद-संगम • Party Sync"
            setTextColor(Color.parseColor("#F5B041"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.DEFAULT_BOLD
        })
        titleCol.addView(TextView(this).apply {
            text = "Zero-Echo Hostel Hotspot & Router Audio System"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })
        headerRow.addView(titleCol)

        val replayIntroBtn = createStyledButton("🗺️ आर्यावर्त Map", "#33220B", "#F5B041").apply {
            setOnClickListener { playAryavartaSplashIntro() }
        }
        headerRow.addView(replayIntroBtn)
        mainContainer.addView(headerRow)

        val netCard = createCard()
        val localIp = getLocalLanIpAddress()
        val isHotspotSubnet = localIp.startsWith("192.168.43.") || localIp.startsWith("192.168.137.")
        modeCapacityBadge = TextView(this).apply {
            text = if (isHotspotSubnet) {
                "📡 Mobile Hotspot Active • Optimized for 7 Phones (Zero Echo)"
            } else {
                "🚀 Wi-Fi Router LAN Active • High-Capacity Mode (Up to 32 Phones)"
            }
            setTextColor(Color.parseColor("#38BDF8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
        }
        netCard.addView(modeCapacityBadge)

        ipInfoText = TextView(this).apply {
            text = "Your Local IP: $localIp  |  Room Code: ${ipToQuickCode(localIp)}  |  0 MB Internet Used"
            setTextColor(Color.parseColor("#E2E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setPadding(0, dp(4), 0, 0)
        }
        netCard.addView(ipInfoText)

        statusBadge = TextView(this).apply {
            text = "Status: Ready — Choose DJ Host or Friend Join Mode below"
            setTextColor(Color.parseColor("#4ADE80"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setPadding(0, dp(6), 0, 0)
        }
        netCard.addView(statusBadge)
        mainContainer.addView(netCard)

        val hostCard = createCard()
        hostCard.addView(sectionHeader("👑 1. DJ HOST MODE (Gaana Bajane Wala Phone)"))

        trackTitleText = TextView(this).apply {
            text = "🎵 Selected Song: $currentTrackName"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(6), 0, dp(8))
        }
        hostCard.addView(trackTitleText)

        val hostBtnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val pickSongBtn = createStyledButton("📂 1. Select MP3 Song", "#1E293B", "#38BDF8").apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(6) }
            setOnClickListener { pickAudioFile() }
        }
        playPauseBtn = createStyledButton("▶️ 2. Sync & Play All", "#065F46", "#6EE7B7").apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { toggleHostPlayPause() }
        }
        hostBtnRow.addView(pickSongBtn)
        hostBtnRow.addView(playPauseBtn)
        hostCard.addView(hostBtnRow)

        connectedClientsText = TextView(this).apply {
            text = "Connected Speakers/Phones: 0 (Waiting for friends to join...)"
            setTextColor(Color.parseColor("#FBBF24"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setPadding(0, dp(8), 0, 0)
        }
        hostCard.addView(connectedClientsText)
        mainContainer.addView(hostCard)

        val clientCard = createCard()
        clientCard.addView(sectionHeader("🎧 2. FRIEND SPEAKER MODE (Baaki Doston Ke Phone)"))

        discoveredHostBtn = createStyledButton("🔍 Auto-Searching DJ on Wi-Fi/Hotspot...", "#1E1B4B", "#A5B4FC").apply {
            setOnClickListener {
                val ip = discoveredHostIp
                if (ip != null) {
                    hostCodeInput.setText(ip)
                    connectToHost(ip)
                } else {
                    Toast.makeText(this@MainActivity, "Enter DJ IP or Room Code below!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        clientCard.addView(discoveredHostBtn)

        val joinRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        hostCodeInput = EditText(this).apply {
            hint = "Enter DJ IP (e.g. 192.168.43.1)"
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            inputType = InputType.TYPE_CLASS_TEXT
            background = roundedBg("#0F172A", "#334155")
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f).apply { rightMargin = dp(8) }
        }
        val joinBtn = createStyledButton("⚡ Join Party", "#7C2D12", "#FDBA74").apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f)
            setOnClickListener {
                val raw = hostCodeInput.text.toString().trim()
                if (raw.isNotEmpty()) connectToHost(resolveIpInput(raw))
            }
        }
        joinRow.addView(hostCodeInput)
        joinRow.addView(joinBtn)
        clientCard.addView(joinRow)
        mainContainer.addView(clientCard)

        val calibCard = createCard()
        calibCard.addView(sectionHeader("🎛️ 3. ZERO-ECHO BLUETOOTH SPEAKER CALIBRATION"))

        syncMetricText = TextView(this).apply {
            text = "Clock Offset: 0 ms  |  Network Ping: -- ms  |  PLL Phase: Locked"
            setTextColor(Color.parseColor("#34D399"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setPadding(0, dp(4), 0, dp(6))
        }
        calibCard.addView(syncMetricText)

        calibrationLabel = TextView(this).apply {
            text = "Bluetooth Hardware Offset: 0 ms (Use presets if using BT Speaker/Buds)"
            setTextColor(Color.parseColor("#E2E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        }
        calibCard.addView(calibrationLabel)

        val seekBar = SeekBar(this).apply {
            max = 400
            progress = 200
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    btHardwareDelayMs = p - 200
                    calibrationLabel.text = "Bluetooth Hardware Offset: ${btHardwareDelayMs} ms"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        calibCard.addView(seekBar)

        val presetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
        }
        val pPhone = createStyledButton("📱 Phone (0ms)", "#0F172A", "#94A3B8").apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(4) }
            setOnClickListener { seekBar.progress = 200 }
        }
        val pBuds = createStyledButton("🎧 Buds (+65ms)", "#0F172A", "#38BDF8").apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(4) }
            setOnClickListener { seekBar.progress = 265 }
        }
        val pSpeaker = createStyledButton("🔊 BT Speaker (+125ms)", "#0F172A", "#F59E0B").apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { seekBar.progress = 325 }
        }
        presetRow.addView(pPhone)
        presetRow.addView(pBuds)
        presetRow.addView(pSpeaker)
        calibCard.addView(presetRow)

        val extraRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        val channelBtn = createStyledButton("🔊 Mode: Full Stereo", "#1E293B", "#E2E8F0").apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(6) }
            setOnClickListener {
                audioChannelMode = (audioChannelMode + 1) % 3
                when (audioChannelMode) {
                    0 -> { text = "🔊 Mode: Full Stereo"; mediaPlayer?.setVolume(1f, 1f) }
                    1 -> { text = "⬅️ Mode: Left Speaker"; mediaPlayer?.setVolume(1f, 0f) }
                    2 -> { text = "➡️ Mode: Right Speaker"; mediaPlayer?.setVolume(0f, 1f) }
                }
            }
        }
        val hypeBtn = createStyledButton("🔥 BASS DROP FLASH", "#4C0519", "#FB7185").apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { triggerPartyStrobeAndBroadcast() }
        }
        extraRow.addView(channelBtn)
        extraRow.addView(hypeBtn)
        calibCard.addView(extraRow)

        mainContainer.addView(calibCard)
    }

    private fun pickAudioFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }
        startActivityForResult(intent, REQ_PICK_AUDIO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_AUDIO && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            prepareHostWithSelectedSong(uri)
        }
    }

    private fun prepareHostWithSelectedSong(uri: Uri) {
        statusBadge.text = "Status: Loading track into high-speed local RAM/Cache..."
        threadPool.execute {
            try {
                val fileName = queryFileName(uri)
                val destFile = File(cacheDir, "party_stream_track.mp3")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output, 64 * 1024)
                    }
                }
                sharedAudioFile = destFile
                currentTrackName = fileName
                trackSessionId = System.currentTimeMillis()
                isHostMode = true

                startHostServersIfNeeded()

                uiHandler.post {
                    val ip = getLocalLanIpAddress()
                    ipInfoText.text = "DJ Host IP: $ip  |  Quick Code: ${ipToQuickCode(ip)}  |  Zero Internet Used"
                    trackTitleText.text = "🎵 Ready Track: $currentTrackName"
                    statusBadge.text = "Status: DJ Server Live! Friends can tap Join & Sync now."
                    prepareLocalMediaPlayer(destFile.absolutePath, isHost = true)
                    broadcastCommandToClients("LOAD|$trackSessionId|$currentTrackName")
                }
            } catch (e: Exception) {
                uiHandler.post { statusBadge.text = "Error loading audio: ${e.message}" }
            }
        }
    }

    private fun startHostServersIfNeeded() {
        if (httpServerSocket == null) {
            threadPool.execute {
                try {
                    httpServerSocket = ServerSocket(HTTP_PORT, 50)
                    while (!httpServerSocket!!.isClosed) {
                        val client = httpServerSocket!!.accept()
                        threadPool.execute { handleHttpAudioRequest(client) }
                    }
                } catch (_: Exception) {}
            }
        }

        if (syncServerSocket == null) {
            threadPool.execute {
                try {
                    syncServerSocket = ServerSocket(SYNC_TCP_PORT, 50)
                    while (!syncServerSocket!!.isClosed) {
                        val client = syncServerSocket!!.accept()
                        client.tcpNoDelay = true
                        threadPool.execute { handleSyncClientConnection(client) }
                    }
                } catch (_: Exception) {}
            }
        }

        threadPool.execute {
            val udpSocket = DatagramSocket().apply { broadcast = true }
            while (isHostMode) {
                try {
                    val localIp = getLocalLanIpAddress()
                    val msg = "ARYAVARTA_DJ|$localIp|$currentTrackName|${connectedClients.size}"
                    val bytes = msg.toByteArray()
                    val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), DISCOVERY_UDP_PORT)
                    udpSocket.send(packet)

                    val mp = mediaPlayer
                    if (mp != null && isPlayerPrepared && mp.isPlaying) {
                        val now = System.currentTimeMillis()
                        val pos = mp.currentPosition
                        broadcastCommandToClients("BEAT|$pos|$now|$trackSessionId")
                    }
                    Thread.sleep(1800L)
                } catch (_: Exception) {
                    Thread.sleep(1800L)
                }
            }
        }
    }

    private fun handleHttpAudioRequest(socket: Socket) {
        try {
            val file = sharedAudioFile ?: return socket.close()
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            var rangeHeader: String? = null
            var line: String?
            while (reader.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                if (line!!.startsWith("Range:", ignoreCase = true)) {
                    rangeHeader = line!!.substringAfter("=").trim()
                }
            }

            val fileLen = file.length()
            var startByte = 0L
            var endByte = fileLen - 1
            var isPartial = false

            if (rangeHeader != null) {
                isPartial = true
                val parts = rangeHeader.split("-")
                startByte = parts[0].toLongOrNull() ?: 0L
                if (parts.size > 1 && parts[1].isNotEmpty()) {
                    endByte = parts[1].toLongOrNull() ?: (fileLen - 1)
                }
            }

            val contentLen = (endByte - startByte) + 1
            val out = BufferedOutputStream(socket.getOutputStream(), 64 * 1024)
            val headers = StringBuilder()
            if (isPartial) {
                headers.append("HTTP/1.1 206 Partial Content\r\n")
                headers.append("Content-Range: bytes $startByte-$endByte/$fileLen\r\n")
            } else {
                headers.append("HTTP/1.1 200 OK\r\n")
            }
            headers.append("Content-Type: audio/mpeg\r\n")
            headers.append("Accept-Ranges: bytes\r\n")
            headers.append("Content-Length: $contentLen\r\n")
            headers.append("Connection: close\r\n\r\n")
            out.write(headers.toString().toByteArray())

            RandomAccessFile(file, "r").use { raf ->
                raf.seek(startByte)
                val buf = ByteArray(32 * 1024)
                var remaining = contentLen
                while (remaining > 0) {
                    val toRead = min(buf.size.toLong(), remaining).toInt()
                    val read = raf.read(buf, 0, toRead)
                    if (read <= 0) break
                    out.write(buf, 0, read)
                    remaining -= read
                }
                out.flush()
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun handleSyncClientConnection(socket: Socket) {
        val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
        val session = ClientSession(socket, writer, socket.inetAddress.hostAddress ?: "Client")
        connectedClients.add(session)
        updateConnectedCountUi()

        try {
            if (sharedAudioFile != null) {
                writer.println("LOAD|$trackSessionId|$currentTrackName")
            }
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split("|")
                when (parts[0]) {
                    "PING" -> {
                        val clientSendTime = parts.getOrNull(1) ?: "0"
                        val hostNow = System.currentTimeMillis()
                        writer.println("PONG|$clientSendTime|$hostNow")
                    }
                    "READY_FOR_STATE" -> {
                        val mp = mediaPlayer
                        if (mp != null && isPlayerPrepared && mp.isPlaying) {
                            val fireAt = System.currentTimeMillis() + 550L
                            val targetPos = mp.currentPosition + 550
                            writer.println("PLAY_AT|$targetPos|$fireAt|$trackSessionId")
                        }
                    }
                    "STROBE" -> {
                        broadcastCommandToClients("STROBE")
                        uiHandler.post { flashScreenStrobe() }
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            connectedClients.remove(session)
            updateConnectedCountUi()
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun updateConnectedCountUi() {
        uiHandler.post {
            val count = connectedClients.size
            val modeText = if (count <= 7) "Hotspot Zero-Echo Optimal" else "Router High-Capacity Cluster"
            connectedClientsText.text = "Connected Friends: $count Phones ($modeText)"
        }
    }

    private fun toggleHostPlayPause() {
        val mp = mediaPlayer
        if (!isHostMode || mp == null || !isPlayerPrepared) {
            Toast.makeText(this, "First select an MP3 song!", Toast.LENGTH_SHORT).show()
            return
        }

        if (mp.isPlaying) {
            mp.pause()
            val pos = mp.currentPosition
            broadcastCommandToClients("PAUSE|$pos")
            playPauseBtn.text = "▶️ Resume All"
            statusBadge.text = "Status: Paused across all ${connectedClients.size + 1} phones."
        } else {
            val fireAtHostMs = System.currentTimeMillis() + 650L
            val startPosMs = mp.currentPosition
            broadcastCommandToClients("PLAY_AT|$startPosMs|$fireAtHostMs|$trackSessionId")
            scheduleExactLocalPlay(startPosMs, fireAtHostMs, isHost = true)
            playPauseBtn.text = "⏸️ Pause All"
        }
    }

    private fun broadcastCommandToClients(cmd: String) {
        for (client in connectedClients) {
            threadPool.execute {
                try { client.writer.println(cmd) } catch (_: Exception) {}
            }
        }
    }

    private fun connectToHost(hostIp: String) {
        isHostMode = false
        statusBadge.text = "Status: Connecting to DJ ($hostIp)..."
        threadPool.execute {
            try {
                clientSocket?.close()
                val socket = Socket()
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(hostIp, SYNC_TCP_PORT), 4000)
                clientSocket = socket
                val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                clientWriter = writer
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                isClientConnected = true

                uiHandler.post {
                    statusBadge.text = "Status: Connected to DJ $hostIp! Calibrating Zero-Echo Clock..."
                }

                threadPool.execute {
                    while (isClientConnected && !socket.isClosed) {
                        for (i in 1..8) {
                            writer.println("PING|${System.currentTimeMillis()}")
                            Thread.sleep(90L)
                        }
                        Thread.sleep(6000L)
                    }
                }

                val rttSamples = Collections.synchronizedList(mutableListOf<Pair<Long, Long>>())
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split("|")
                    when (parts[0]) {
                        "PONG" -> {
                            val tSend = parts[1].toLong()
                            val tHost = parts[2].toLong()
                            val tRecv = System.currentTimeMillis()
                            val rtt = (tRecv - tSend).coerceAtLeast(1L)
                            val offset = tHost - (tSend + rtt / 2L)
                            rttSamples.add(Pair(rtt, offset))
                            if (rttSamples.size > 8) rttSamples.removeAt(0)
                            val best = rttSamples.minByOrNull { it.first }!!
                            minRttMs = best.first
                            clockOffsetMs = best.second
                            uiHandler.post {
                                syncMetricText.text = "Clock Offset: $clockOffsetMs ms  |  Ping: $minRttMs ms  |  Zero-Echo Locked"
                            }
                        }
                        "LOAD" -> {
                            val sessId = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                            val title = parts.getOrNull(2) ?: "Party Track"
                            trackSessionId = sessId
                            currentTrackName = title
                            val streamUrl = "http://$hostIp:$HTTP_PORT/track.mp3?sid=$sessId"
                            uiHandler.post {
                                trackTitleText.text = "🎵 DJ Song: $currentTrackName"
                                prepareLocalMediaPlayer(streamUrl, isHost = false)
                            }
                        }
                        "PLAY_AT" -> {
                            val startPos = parts[1].toInt()
                            val fireAtHostMs = parts[2].toLong()
                            scheduleExactLocalPlay(startPos, fireAtHostMs, isHost = false)
                        }
                        "PAUSE" -> {
                            val pausePos = parts[1].toInt()
                            uiHandler.post {
                                mediaPlayer?.let {
                                    if (it.isPlaying) it.pause()
                                    it.seekTo(pausePos)
                                }
                                statusBadge.text = "Status: Paused by DJ"
                            }
                        }
                        "BEAT" -> {
                            val hostPos = parts[1].toInt()
                            val hostBeatTime = parts[2].toLong()
                            applyPllDriftCorrection(hostPos, hostBeatTime)
                        }
                        "STROBE" -> {
                            uiHandler.post { flashScreenStrobe() }
                        }
                    }
                }
            } catch (e: Exception) {
                isClientConnected = false
                uiHandler.post { statusBadge.text = "Disconnected from DJ: ${e.message}" }
            }
        }
    }

    private fun prepareLocalMediaPlayer(source: String, isHost: Boolean) {
        try {
            mediaPlayer?.release()
            isPlayerPrepared = false
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setDataSource(source)
            mp.setOnPreparedListener {
                isPlayerPrepared = true
                statusBadge.text = if (isHost) {
                    "Status: Track Buffered! Press 'Sync & Play All' to start party."
                } else {
                    "Status: Track Pre-Buffered (0 Internet Data)! Waiting for DJ beat..."
                }
                if (!isHost) {
                    threadPool.execute { clientWriter?.println("READY_FOR_STATE") }
                }
            }
            mp.prepareAsync()
            mediaPlayer = mp
        } catch (e: Exception) {
            statusBadge.text = "Audio Prepare Error: ${e.message}"
        }
    }

    private fun scheduleExactLocalPlay(startPosMs: Int, fireAtHostMs: Long, isHost: Boolean) {
        threadPool.execute {
            val mp = mediaPlayer ?: return@execute
            while (!isPlayerPrepared) {
                Thread.sleep(15L)
            }

            val nowHostMs = if (isHost) System.currentTimeMillis() else (System.currentTimeMillis() + clockOffsetMs)
            val waitMs = fireAtHostMs - nowHostMs - btHardwareDelayMs

            if (waitMs > 0) {
                uiHandler.post {
                    mp.seekTo(startPosMs)
                    statusBadge.text = "Status: Synchronizing Beat in ${waitMs}ms..."
                }
                if (waitMs > 12) Thread.sleep(waitMs - 8)
                while (true) {
                    val currentHostTime = if (isHost) System.currentTimeMillis() else (System.currentTimeMillis() + clockOffsetMs)
                    if (currentHostTime + btHardwareDelayMs >= fireAtHostMs) break
                }
                uiHandler.post {
                    mp.start()
                    statusBadge.text = "Status: 🔊 Playing in Zero-Echo Lock!"
                }
            } else {
                val catchUpPos = (startPosMs + (-waitMs).toInt()).coerceAtLeast(0)
                uiHandler.post {
                    mp.seekTo(catchUpPos)
                    mp.start()
                    statusBadge.text = "Status: 🔊 Live Synced with DJ!"
                }
            }
        }
    }

    private fun applyPllDriftCorrection(hostPosMs: Int, hostBeatTimeMs: Long) {
        val mp = mediaPlayer ?: return
        if (!isPlayerPrepared || !mp.isPlaying) return

        val estimatedHostNow = System.currentTimeMillis() + clockOffsetMs
        val elapsedSinceBeat = (estimatedHostNow - hostBeatTimeMs).toInt()
        val targetPosNow = hostPosMs + elapsedSinceBeat + btHardwareDelayMs
        val actualPosNow = mp.currentPosition
        val driftMs = actualPosNow - targetPosNow

        uiHandler.post {
            try {
                when {
                    abs(driftMs) <= 8 -> {
                        mp.playbackParams = PlaybackParams().setSpeed(1.0f)
                        syncMetricText.text = "Drift: ${driftMs}ms (Perfect Zero-Echo) | Ping: ${minRttMs}ms"
                    }
                    abs(driftMs) in 9..70 -> {
                        val nudgeSpeed = if (driftMs > 0) 0.985f else 1.015f
                        mp.playbackParams = PlaybackParams().setSpeed(nudgeSpeed)
                        syncMetricText.text = "Drift: ${driftMs}ms (Auto-Gliding Phase Lock) | Ping: ${minRttMs}ms"
                        uiHandler.postDelayed({
                            try {
                                if (mp.isPlaying) mp.playbackParams = PlaybackParams().setSpeed(1.0f)
                            } catch (_: Exception) {}
                        }, 550L)
                    }
                    else -> {
                        mp.seekTo(targetPosNow.coerceAtLeast(0))
                        mp.playbackParams = PlaybackParams().setSpeed(1.0f)
                        syncMetricText.text = "Drift Resynced to 0ms | Ping: ${minRttMs}ms"
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun startUdpDiscoveryListener() {
        threadPool.execute {
            try {
                discoverySocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(DISCOVERY_UDP_PORT))
                }
                val buf = ByteArray(1024)
                while (discoverySocket != null && !discoverySocket!!.isClosed) {
                    val packet = DatagramPacket(buf, buf.size)
                    discoverySocket!!.receive(packet)
                    val msg = String(packet.data, 0, packet.length)
                    if (msg.startsWith("ARYAVARTA_DJ|") && !isHostMode) {
                        val parts = msg.split("|")
                        val djIp = parts.getOrNull(1) ?: continue
                        val song = parts.getOrNull(2) ?: "Party Track"
                        val count = parts.getOrNull(3) ?: "0"
                        discoveredHostIp = djIp
                        uiHandler.post {
                            discoveredHostBtn.text = "⚡ Tap to Join DJ ($djIp) • $song ($count Joined)"
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun triggerPartyStrobeAndBroadcast() {
        flashScreenStrobe()
        if (isHostMode) {
            broadcastCommandToClients("STROBE")
        } else {
            threadPool.execute { clientWriter?.println("STROBE") }
        }
    }

    private fun flashScreenStrobe() {
        val colors = intArrayOf(
            Color.parseColor("#88FF6F00"),
            Color.parseColor("#8800E5FF"),
            Color.parseColor("#88F50057")
        )
        strobeOverlay.setBackgroundColor(colors.random())
        strobeOverlay.alpha = 0.9f
        strobeOverlay.animate().alpha(0f).setDuration(420L).start()
    }

    private fun getLocalLanIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in Collections.list(interfaces)) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in Collections.list(intf.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "192.168.43.1"
    }

    private fun ipToQuickCode(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size == 4) "${parts[2]}-${parts[3]}" else ip
    }

    private fun resolveIpInput(input: String): String {
        if (input.contains(".")) return input
        if (input.contains("-")) {
            val parts = input.split("-")
            if (parts.size == 2) return "192.168.${parts[0]}.${parts[1]}"
        }
        return input
    }

    private fun queryFileName(uri: Uri): String {
        var name = "Hostel_Party_Track.mp3"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) {
                name = cursor.getString(idx)
            }
        }
        return name
    }

    private fun createCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#151C2C", "#26324B")
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }
    }

    private fun sectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            setTextColor(Color.parseColor("#F5B041"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun createStyledButton(label: String, bgHex: String, textHex: String): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.parseColor(textHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg(bgHex, textHex)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
    }

    private fun roundedBg(fillHex: String, strokeHex: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(Color.parseColor(fillHex))
            setStroke(dp(1), Color.parseColor(strokeHex))
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        isHostMode = false
        isClientConnected = false
        try {
            mediaPlayer?.release()
            httpServerSocket?.close()
            syncServerSocket?.close()
            discoverySocket?.close()
            clientSocket?.close()
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
