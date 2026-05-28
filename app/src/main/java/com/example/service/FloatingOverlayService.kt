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
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FloatingOverlayService : Service(), ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var baseOverlayView: FrameLayout? = null
    private var crosshairView: FrameLayout? = null
    private var isCrosshairActive = false

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
        val density = resources.displayMetrics.density
        val sizePx = (76 * density).toInt()

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
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
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MyApplicationTheme {
                    var isOpen by remember { mutableStateOf(false) }
                    var crosshairEnabledState by remember { mutableStateOf(isCrosshairActive) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!isOpen) {
                            FloatingBubbleButton(
                                onDrag = { dx, dy ->
                                    params.x += dx.toInt()
                                    params.y += dy.toInt()
                                    try {
                                        windowManager.updateViewLayout(frameLayout, params)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                onClick = {
                                    isOpen = true
                                    updateLayoutParamsSize(true)
                                }
                            )
                        } else {
                            InteractiveOverlayDashboard(
                                onClose = {
                                    isOpen = false
                                    updateLayoutParamsSize(false)
                                },
                                isCrosshairEnabled = crosshairEnabledState,
                                onCrosshairToggle = { enabled ->
                                    crosshairEnabledState = enabled
                                    isCrosshairActive = enabled
                                    if (enabled) {
                                        showCrosshairOverlay()
                                    } else {
                                        hideCrosshairOverlay()
                                    }
                                }
                            )
                        }
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
        val density = resources.displayMetrics.density

        if (expanded) {
            // Expanded HUD takes large area but not whole screen, e.g. 350dp wide, 480dp high
            // to keep game visible and interactive underneath outside the panel
            currentParams.width = (355 * density).toInt()
            currentParams.height = (490 * density).toInt()
            currentParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        } else {
            // Collapsed size
            val sizePx = (76 * density).toInt()
            currentParams.width = sizePx
            currentParams.height = sizePx
            currentParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
        try {
            windowManager.updateViewLayout(frame, currentParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showCrosshairOverlay() {
        if (crosshairView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val frameLayout = FrameLayout(this)
        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        frameLayout.setViewTreeLifecycleOwner(lifecycleOwner)
        frameLayout.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        frameLayout.setViewTreeViewModelStoreOwner(this)

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GamingCrosshairHUD()
                }
            }
        }

        frameLayout.addView(composeView)
        crosshairView = frameLayout
        try {
            windowManager.addView(frameLayout, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideCrosshairOverlay() {
        crosshairView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            crosshairView = null
        }
    }

    override fun onDestroy() {
        baseOverlayView?.let { 
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        hideCrosshairOverlay()
        viewModelStore.clear()
        super.onDestroy()
    }
}

// Minimal LifecycleOwner for Floating compose context
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

@Composable
fun FloatingBubbleButton(
    onDrag: (Float, Float) -> Unit,
    onClick: () -> Unit
) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var dragTotal = Offset.Zero
                detectDragGestures(
                    onDragStart = { dragTotal = Offset.Zero },
                    onDragEnd = {
                        if (dragTotal.getDistance() < 12f) {
                            onClick()
                        }
                    },
                    onDragCancel = { },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragTotal += dragAmount
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070709)),
            border = BorderStroke(2.dp, Color(0xFF22C55E)),
            modifier = Modifier.size(56.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0x3322C55E),
                        radius = size.minDimension / 2.2f * pulseScale,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = "Expand Panel",
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun GamingCrosshairHUD() {
    Canvas(modifier = Modifier.size(60.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)

        // Inner precise neon target dot
        drawCircle(
            color = Color(0xFFFF0055),
            radius = 2.5.dp.toPx()
        )

        // Inner high-visibility circle
        drawCircle(
            color = Color(0xFF00FFCC).copy(alpha = 0.8f),
            radius = 10.dp.toPx(),
            style = Stroke(width = 1.25.dp.toPx())
        )

        // Outer telemetry ring ticks
        drawCircle(
            color = Color(0xFF22C55E).copy(alpha = 0.4f),
            radius = 22.dp.toPx(),
            style = Stroke(width = 1.dp.toPx())
        )

        // Precision horizontal targeting ticks
        drawLine(
            color = Color(0xFF00FFCC),
            start = Offset(center.x - 18.dp.toPx(), center.y),
            end = Offset(center.x - 6.dp.toPx(), center.y),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = Color(0xFF00FFCC),
            start = Offset(center.x + 6.dp.toPx(), center.y),
            end = Offset(center.x + 18.dp.toPx(), center.y),
            strokeWidth = 1.5.dp.toPx()
        )

        // Precision vertical targeting ticks
        drawLine(
            color = Color(0xFF00FFCC),
            start = Offset(center.x, center.y - 18.dp.toPx()),
            end = Offset(center.x, center.y - 6.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = Color(0xFF00FFCC),
            start = Offset(center.x, center.y + 6.dp.toPx()),
            end = Offset(center.x, center.y + 18.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )
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
fun InteractiveOverlayDashboard(
    onClose: () -> Unit,
    isCrosshairEnabled: Boolean,
    onCrosshairToggle: (Boolean) -> Unit
) {
    val weaponList = remember {
        listOf(
            Weapon("M1887", "Shotgun", "100%", "Shot rate: 42%", "Range: Short", "Maximum damage dual-barrel visual execution"),
            Weapon("AWM", "Sniper Rifle", "90%", "Shot rate: 27%", "Range: Extreme", "One-shot diagnostic calibration model"),
            Weapon("MP40", "Submachine", "48%", "Shot rate: 83%", "Range: Short", "Rapid frequency wave calibration optimizer"),
            Weapon("Desert Eagle", "Pistol", "90%", "Shot rate: 33%", "Range: Mid-Range", "High single-impact precise pointer"),
            Weapon("AK47", "Assault Rifle", "61%", "Shot rate: 56%", "Range: Medium-Long", "Stability calibration diagnostic visualizer"),
            Weapon("Woodpecker", "Marksman", "72%", "Shot rate: 44%", "Range: Long", "Precision semi-automatic simulation grid")
        )
    }

    var selectedWeapon by remember { mutableStateOf<Weapon?>(null) }
    val hackLogs = remember { mutableStateListOf<String>() }
    var hackProgress by remember { mutableStateOf(0.0f) }
    var isHacking by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf("Ready to initiate model diagnostics") }

    LaunchedEffect(isHacking, selectedWeapon) {
        if (isHacking && selectedWeapon != null) {
            hackLogs.clear()
            hackProgress = 0.0f

            val logs = listOf(
                "Establishing telemetry contact with dynamic system...",
                "Opening local diagnostic pipelines for ${selectedWeapon?.name}...",
                "[SECURITY] Sandboxed isolation detected. Satisfying TOS compliance.",
                "[SECURITY] Hardware pointer intercept: BLOCKED by Linux OS kernel.",
                "[SYSTEM] Cross-process memory editing prohibited on default device.",
                "[SYSTEM] Emulating elite tactical recoil booster calibration metrics...",
                "[SYSTEM] Calibrating 3D display orientation dimensions...",
                "[SYSTEM] High-contrast center-screen target dot activated: SUCCESS!",
                "Visual telemetry calculations completed successfully."
            )

            for (i in logs.indices) {
                delay(650)
                hackLogs.add(0, "[CALIBRATE] " + logs[i])
                hackProgress = (i + 1).toFloat() / logs.size.toFloat()
                currentStep = logs[i]
            }
            delay(500)
            isHacking = false
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFB0A0B10)),
        border = BorderStroke(1.5.dp, Color(0xFF22C55E)),
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Holographic panel",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GP FLOATING CODES",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x22EF4444))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close overlay",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tactical Overlay Switch Option (Direct implementation for user request)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TACTICAL CENTER SIGHT",
                        color = Color(0xFF22C55E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Centres custom high-contrast crosshair overlay.",
                        color = Color.Gray,
                        fontSize = 8.5.sp,
                        lineHeight = 11.sp
                    )
                }
                Switch(
                    checked = isCrosshairEnabled,
                    onCheckedChange = onCrosshairToggle,
                    modifier = Modifier.height(24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF22C55E),
                        checkedTrackColor = Color(0x3322C55E),
                        uncheckedThumbColor = Color.DarkGray,
                        uncheckedTrackColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main column split
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Weapons List (Left)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WEAPON DIRECTORY",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(weaponList) { weapon ->
                            val isSelected = selectedWeapon == weapon
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0x1822C55E) else Color(0x07FFFFFF)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF22C55E) else Color(0x12FFFFFF)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedWeapon = weapon
                                        isHacking = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = weapon.name,
                                            color = if (isSelected) Color(0xFF22C55E) else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = weapon.type,
                                            color = Color.Gray,
                                            fontSize = 8.5.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Status Monitors (Right)
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = "TELEMETRY MONITOR",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    if (selectedWeapon != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x06FFFFFF)),
                            border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "${selectedWeapon!!.name} CALIBRATION",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Dmg: ${selectedWeapon!!.initialDamage}",
                                    color = Color.LightGray,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Rate: ${selectedWeapon!!.initialRate}",
                                    color = Color.LightGray,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Scope: ${selectedWeapon!!.initialRange}",
                                    color = Color.LightGray,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { isHacking = true },
                                    enabled = !isHacking,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isHacking) "BOOSTING..." else "EXECUTE CALIBRATION",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Terminal Output
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFF030304), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(10.dp))
                                .padding(6.dp)
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
                                        tint = Color(0x2222C55E),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Awaiting manual diagnostic calibration pipeline execution.",
                                        color = Color.DarkGray,
                                        fontSize = 8.sp,
                                        textAlign = TextAlign.Center,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 10.sp
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
                                            color = when {
                                                log.contains("complete") -> Color(0xFF22C55E)
                                                log.contains("SECURITY") -> Color(0xFFFFCC00)
                                                else -> Color(0xFF00FFCC)
                                            },
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.fillMaxWidth(),
                                            lineHeight = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x05FFFFFF), RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0x0AFFFFFF), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Select weapon",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Please select a specific weapon to load telemetry calibration settings.",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress or Security Status
            if (isHacking) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentStep,
                            color = Color(0xFF10B981),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(hackProgress * 100).toInt()}%",
                            color = Color(0xFF10B981),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { hackProgress },
                        color = Color(0xFF22C55E),
                        trackColor = Color(0x1AFFFFFF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x1110B981), RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Secure status",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sandbox Protection Active: Cross-process input injection is automatically restricted for device and account security. Full client-side telemetry visualizer active.",
                        color = Color.LightGray,
                        fontSize = 8.sp,
                        lineHeight = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
