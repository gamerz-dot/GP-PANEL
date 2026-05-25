package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.service.OverlayLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FloatingOverlayService : Service(), ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var baseOverlayView: FrameLayout? = null
    private val overlayScope = CoroutineScope(Dispatchers.Main)
    override val viewModelStore = ViewModelStore()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1105, createNotification())
        showFloatingBubble()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "OverlayChannel",
                "GP Panel Live Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "GP Panel active overlay visualizer"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "OverlayChannel")
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("GP PANEL Active")
            .setContentText("Holographic controller dashboard is active.")
            .setSmallIcon(android.R.drawable.presence_online)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun showFloatingBubble() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val frameLayout = FrameLayout(this)
        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        frameLayout.setViewTreeLifecycleOwner(lifecycleOwner)
        frameLayout.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        frameLayout.setViewTreeViewModelStoreOwner(this)

        val composeView = ComposeView(this).apply {
            setContent {
                var isOpen by remember { mutableStateOf(false) }

                Box {
                    if (!isOpen) {
                        // Floating cyber bubble to open panel
                        FloatingBubbleButton(
                            onClick = {
                                isOpen = true
                                updateLayoutParamsSize(true)
                            }
                        )
                    } else {
                        // Expandable HUD Dashboard
                        InteractiveOverlayDashboard(
                            onClose = {
                                isOpen = false
                                updateLayoutParamsSize(false)
                            }
                        )
                    }
                }
            }
        }

        frameLayout.addView(composeView)
        baseOverlayView = frameLayout
        windowManager.addView(frameLayout, params)
    }

    private fun updateLayoutParamsSize(expanded: Boolean) {
        val frame = baseOverlayView ?: return
        val currentParams = frame.layoutParams as WindowManager.LayoutParams
        if (expanded) {
            currentParams.width = WindowManager.LayoutParams.MATCH_PARENT
            currentParams.height = WindowManager.LayoutParams.MATCH_PARENT
            currentParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        } else {
            currentParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            currentParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            currentParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        windowManager.updateViewLayout(frame, currentParams)
    }

    override fun onDestroy() {
        baseOverlayView?.let { windowManager.removeView(it) }
        viewModelStore.clear()
        super.onDestroy()
    }
}

// Minimal dummy LifecycleOwner for Floating compose context
class OverlayLifecycleOwner : androidx.lifecycle.LifecycleOwner, androidx.savedstate.SavedStateRegistryOwner {
    private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
    private val savedStateRegistryController = androidx.savedstate.SavedStateRegistryController.create(this)

    init {
        savedStateRegistryController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry
}

// UI COMPOSABLES FOR OVERLAY

@Composable
fun FloatingBubbleButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "bubble")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(2.dp, Color(0xFF22C55E)),
        modifier = Modifier
            .size(64.dp)
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Drawing cyber circles
                drawCircle(
                    color = Color(0x3322C55E),
                    radius = size.minDimension / 2.2f * pulseScale,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = "Open GP Panel",
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

data class Weapon(
    val name: String,
    val type: String,
    val initialDamage: String,
    val initialRate: String,
    val initialRange: String,
    val description: String
)

@Composable
fun InteractiveOverlayDashboard(onClose: () -> Unit) {
    val weaponList = remember {
        listOf(
            Weapon("M1887", "Shotgun", "100%", "Shot rate: 42%", "Range: Short", "Maximum damage dual-barrel visual executor"),
            Weapon("AWM", "Sniper Rifle", "90%", "Shot rate: 27%", "Range: Extreme", "One-shot diagnostic calibration model"),
            Weapon("MP40", "Submachine Gun", "48%", "Shot rate: 83%", "Range: Medium-Short", "Rapid frequency wave calibration optimizer"),
            Weapon("Desert Eagle", "Pistol", "90%", "Shot rate: 33%", "Range: Mid-Range", "High single-impact precise pointer"),
            Weapon("AK47", "Assault Rifle", "61%", "Shot rate: 56%", "Range: Medium-Long", "Stability calibration diagnostic visualizer"),
            Weapon("Woodpecker", "Marksman Rifle", "72%", "Shot rate: 44%", "Range: Long", "Precision semi-automatic simulation grid")
        )
    }

    var selectedWeapon by remember { mutableStateOf<Weapon?>(null) }
    var hackLogs = remember { mutableStateListOf<String>() }
    var hackProgress by remember { mutableStateOf(0.0f) }
    var isHacking by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf("Ready to initiate panel Diagnostics") }

    LaunchedEffect(isHacking, selectedWeapon) {
        if (isHacking && selectedWeapon != null) {
            hackLogs.clear()
            hackProgress = 0.0f
            
            val logs = listOf(
                "Establishing telemetry contact with diagnostic interface...",
                "Opening UDP simulation pipeline to GP sandbox channel...",
                "Loading target data profile for weapon: ${selectedWeapon?.name}...",
                "Initializing local pointer calibration visualizer...",
                "Reading system frame buffer parameters...",
                "Parsing latency thresholds (target level: Optimal)...",
                "Analyzing anti-aliasing vectors for smoother output...",
                "Injecting safe mock logs to test holographic response...",
                "Simulating secure parameter calibration checks...",
                "Synchronizing with local UI refresh registers...",
                "HOLOGRAPHIC COMPONENT READY - Visual boost complete!"
            )

            for (i in logs.indices) {
                delay(750)
                hackLogs.add(0, "[SYSTEM] " + logs[i])
                hackProgress = (i + 1).toFloat() / logs.size.toFloat()
                currentStep = logs[i]
            }
            delay(1000)
            isHacking = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9090D16))
            .clickable(enabled = false) {}, // absorb taps
        contentAlignment = Alignment.Center
    ) {
        // Neon Wing diagonal artwork
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Neon diagonal lines
            drawLine(
                color = Color(0xFF22C55E),
                start = Offset(0f, height * 0.25f),
                end = Offset(width, height * 0.75f),
                strokeWidth = 3.dp.toPx()
            )

            // Neon cyan parallel diagonal
            drawLine(
                color = Color(0xFF06B6D4),
                start = Offset(0f, height * 0.28f),
                end = Offset(width, height * 0.78f),
                strokeWidth = 1.dp.toPx()
            )

            // Dynamic futuristic wings drawing (holographic background)
            val wingPathLeft = Path().apply {
                moveTo(width * 0.1f, height * 0.35f)
                quadraticTo(width * 0.05f, height * 0.45f, width * 0.15f, height * 0.55f)
                quadraticTo(width * 0.25f, height * 0.52f, width * 0.32f, height * 0.45f)
                close()
            }
            drawPath(wingPathLeft, color = Color(0x2222C55E))
            
            val wingPathRight = Path().apply {
                moveTo(width * 0.9f, height * 0.65f)
                quadraticTo(width * 0.95f, height * 0.55f, width * 0.85f, height * 0.45f)
                quadraticTo(width * 0.75f, height * 0.48f, width * 0.68f, height * 0.55f)
                close()
            }
            drawPath(wingPathRight, color = Color(0x2222C55E))
        }

        // Inner Card HUD
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFA0F172A)),
            border = BorderStroke(2.dp, Color(0xFF22C55E)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x3322C55E),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = "GP Icon",
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "GP HOLOGRAPHIC PANELS",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x22EF4444))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close HUDOverlay",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode descriptions
                Text(
                    text = "Select weapon layout model below to trigger the secure diagnostic bypass visualization test matrix. This is a visual gaming telemetry simulator.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Core dynamic grid
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Left Weapon Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "WEAPONS DIRECTORY",
                            color = Color(0xFF22C55E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(weaponList) { weapon ->
                                val isSelected = selectedWeapon == weapon
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0x2222C55E) else Color(0x111E293B)
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF22C55E) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedWeapon = weapon
                                            isHacking = false
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = weapon.name,
                                            color = if (isSelected) Color(0xFF22C55E) else Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = weapon.type,
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Right diagnostics/terminal screen
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "TELEMETRY MONITOR",
                            color = Color(0xFF06B6D4),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (selectedWeapon != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0x221E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${selectedWeapon!!.name} CALIBRATION",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Initial Damage: ${selectedWeapon!!.initialDamage}",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Firing Rate: ${selectedWeapon!!.initialRate}",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Optimal Scope: ${selectedWeapon!!.initialRange}",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { isHacking = true },
                                        enabled = !isHacking,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isHacking) "BOOSTING ENGINE..." else "EXECUTE TEST",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Terminal Shell Log
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color(0xFF05070C), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                if (hackLogs.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = "Safe terminal",
                                            tint = Color(0x3322C55E),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Ready to start simulated bypass pipeline",
                                            color = Color.DarkGray,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        reverseLayout = true
                                    ) {
                                        items(hackLogs) { log ->
                                            Text(
                                                text = log,
                                                color = if (log.contains("complete")) Color(0xFF22C55E) else Color(0xFF00FFCC),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.fillMaxWidth(),
                                                lineHeight = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Empty Directory
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x33090D16), RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Select weapon",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Please select a Free Fire gun to load telemetry controls",
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar or Status bar
                if (isHacking) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentStep,
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${(hackProgress * 100).toInt()}%",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { hackProgress },
                            color = Color(0xFF22C55E),
                            trackColor = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x2210B981), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Secure status",
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sandbox Secured: Safe visual simulation parameters active. Telemetry pipeline is sandbox-bound.",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}
