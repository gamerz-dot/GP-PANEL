package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.GeminiRepository
import com.example.data.NoteReminder
import com.example.data.NoteReminderDao
import com.example.service.FloatingOverlayService
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

// Sealed navigation architecture keys
sealed class Screen {
    object Login : Screen()
    object SignUp : Screen()
    object MainHub : Screen()
}

// Immersive UI Custom Grid Matrix Mesh background drawing modifier
fun Modifier.drawDotGrid(color: Color = Color(0x1300FF66), step: Float = 40f): Modifier = this.drawBehind {
    val width = size.width
    val height = size.height
    var x = 0f
    while (x < width) {
        var y = 0f
        while (y < height) {
            drawCircle(
                color = color,
                radius = 1.5f,
                center = Offset(x, y)
            )
            y += step
        }
        x += step
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var loggedInUser by remember { mutableStateOf<String?>("GP_ELITE") }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() togetherWith
                    slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut()
        },
        label = "ScreenNavigation"
    ) { screen ->
        when (screen) {
            is Screen.Login -> LoginScreen(
                onLoginSuccess = { user ->
                    loggedInUser = user
                    currentScreen = Screen.MainHub
                },
                onNavigateToSignUp = { currentScreen = Screen.SignUp }
            )
            is Screen.SignUp -> SignUpScreen(
                onSignUpSuccess = { user ->
                    loggedInUser = user
                    currentScreen = Screen.MainHub
                },
                onNavigateToLogin = { currentScreen = Screen.Login }
            )
            is Screen.MainHub -> MainHubScreen(
                username = loggedInUser ?: "GP_Warrior",
                onSignOut = {
                    loggedInUser = null
                    currentScreen = Screen.Login
                }
            )
        }
    }
}

// LOGIN SCREEN
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070709))
            .drawDotGrid(Color(0x0E00FF66)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
            border = BorderStroke(1.dp, Color(0x15FFFFFF)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header Logo Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(CyberGreen.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, CyberGreen.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Adjust,
                        contentDescription = "Logo icon",
                        tint = CyberGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "GP PANEL LOGIN",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "FUTURISTIC CALIBRATION PLATFORM",
                    color = CyberSlateGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; loginError = "" },
                    label = { Text("Email or Cadet Username", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AlternateEmail,
                            contentDescription = "Email",
                            tint = CyberGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberGreen,
                        unfocusedBorderColor = Color(0x1AFFFFFF),
                        focusedLabelColor = CyberGreen,
                        unfocusedLabelColor = GreyText,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF090909),
                        unfocusedContainerColor = Color(0xFF090909)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; loginError = "" },
                    label = { Text("Cyber Key Password", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = CyberGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberGreen,
                        unfocusedBorderColor = Color(0x1AFFFFFF),
                        focusedLabelColor = CyberGreen,
                        unfocusedLabelColor = GreyText,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF090909),
                        unfocusedContainerColor = Color(0xFF090909)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                if (loginError.isNotEmpty()) {
                    Text(
                        text = loginError,
                        color = ErrorCrimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Start,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Login trigger
                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty()) {
                            loginError = "Mandatory diagnostic input parameters are missing."
                            return@Button
                        }
                        isChecking = true
                        coroutineScope.launch {
                            delay(1000)
                            isChecking = false
                            onLoginSuccess(email.ifEmpty { "GP_ELITE" })
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "SYSTEM ACCESS AUTHORIZED",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Third Party Integrations
                Text(
                    text = "OR SECURE WITH EXTERNAL NODES",
                    color = CyberSlateGray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            isChecking = true
                            coroutineScope.launch {
                                delay(1200)
                                isChecking = false
                                onLoginSuccess("Google_Pilot")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x08FFFFFF)),
                        border = BorderStroke(1.dp, Color(0x15FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "Google SSO",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google", fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            isChecking = true
                            coroutineScope.launch {
                                delay(1200)
                                isChecking = false
                                onLoginSuccess("FB_Commander")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x08FFFFFF)),
                        border = BorderStroke(1.dp, Color(0x15FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Facebook,
                            contentDescription = "Facebook SSO",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Facebook", fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "New tactical user? Register Cadet Node",
                    fontSize = 11.sp,
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToSignUp() }
                        .padding(8.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// SIGN UP SCREEN
@Composable
fun SignUpScreen(
    onSignUpSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var signUpError by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070709))
            .drawDotGrid(Color(0x0E00FFFF)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
            border = BorderStroke(1.dp, Color(0x15FFFFFF)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header Logo Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(CyberCyan.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddModerator,
                        contentDescription = "Shield check icon",
                        tint = CyberCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CADET REGISTRATION",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "INITIALIZE SECURITY CLEARANCE CREDENTIALS",
                    color = CyberSlateGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; signUpError = "" },
                    label = { Text("Cadet Username/Email", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AlternateEmail,
                            contentDescription = "Email",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color(0x1AFFFFFF),
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = GreyText,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF090909),
                        unfocusedContainerColor = Color(0xFF090909)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; signUpError = "" },
                    label = { Text("Operational Security Key", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color(0x1AFFFFFF),
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = GreyText,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF090909),
                        unfocusedContainerColor = Color(0xFF090909)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Confirm Password field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; signUpError = "" },
                    label = { Text("Confirm Security Key", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Confirm key",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color(0x1AFFFFFF),
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = GreyText,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF090909),
                        unfocusedContainerColor = Color(0xFF090909)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                if (signUpError.isNotEmpty()) {
                    Text(
                        text = signUpError,
                        color = ErrorCrimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Start,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                            signUpError = "All security nodes must be fully populated."
                            return@Button
                        }
                        if (password != confirmPassword) {
                            signUpError = "Security check failed. Key parameters mismatch."
                            return@Button
                        }
                        isChecking = true
                        coroutineScope.launch {
                            delay(1200)
                            isChecking = false
                            onSignUpSuccess(email)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "PROVISION CADET DECREE",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Already certified? Connection Interface Login",
                    fontSize = 11.sp,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .padding(8.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}


// PULSING DOT COMPONENT FOR CYBER FEEDBACK
@Composable
fun PulsingStatusDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
        Box(
            modifier = Modifier
                .size(7.dp * scale)
                .background(CyberGreen.copy(alpha = alpha), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(CyberGreen, CircleShape)
        )
    }
}

// YOUTUBE ICON AND LINK UTILITY
@Composable
fun YouTubeIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFFFF0000)) {
    Box(
        modifier = modifier
            .background(tint, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(6.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, Color.White)
        }
    }
}

fun openYoutubeChannel(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@gamerzplay_8?si=wLywhOF-DlHSmVVY"))
    context.startActivity(intent)
}

// MAIN HUB COMPOSABLE - DASHBOARD, AI, FF STATISTICS, CALENDAR, NOTES AND OVERLAYS
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainHubScreen(
    username: String,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var alertBanner by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkCharcoal,
                modifier = Modifier.border(BorderStroke(1.dp, Color(0x10FFFFFF)), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Panel", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberGreen,
                        selectedTextColor = CyberGreen,
                        unselectedIconColor = GreyText,
                        unselectedTextColor = GreyText,
                        indicatorColor = CyberGreen.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.QueryStats, contentDescription = "UID statistics") },
                    label = { Text("FF Uid", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        unselectedIconColor = GreyText,
                        unselectedTextColor = GreyText,
                        indicatorColor = CyberCyan.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI Assistant chat") },
                    label = { Text("AI Chat", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberGreen,
                        selectedTextColor = CyberGreen,
                        unselectedIconColor = GreyText,
                        unselectedTextColor = GreyText,
                        indicatorColor = CyberGreen.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Scheduler reminders") },
                    label = { Text("Logs/Rem", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        unselectedIconColor = GreyText,
                        unselectedTextColor = GreyText,
                        indicatorColor = CyberCyan.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavy)
                .padding(innerPadding)
        ) {
            // HIGH-FIDELITY IMMERSIVE IMMERSIVE TOPBAR HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left profile details with modern neon-gradient badge launcher
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(CyberGreen, CyberBlue)
                                ),
                                shape = CircleShape
                            )
                            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "GP",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = (-1).sp
                            )
                        )
                    }

                    Column {
                        Text(
                            text = "GP PANEL PRO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberSlateGray,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )
                        Text(
                            text = "Welcome, Agent",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Active telemetry indicator badge & fast logout node
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0x0CFFFFFF), RoundedCornerShape(100.dp))
                            .border(BorderStroke(1.dp, Color(0x10FFFFFF)), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        PulsingStatusDot()
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CORE_ACTIVE",
                            color = CyberGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = { openYoutubeChannel(context) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFFF0055).copy(alpha = 0.1f), CircleShape)
                            .border(BorderStroke(1.dp, Color(0xFFFF0055).copy(alpha = 0.2f)), CircleShape)
                    ) {
                        YouTubeIcon(modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFFF0055).copy(alpha = 0.1f), CircleShape)
                            .border(BorderStroke(1.dp, Color(0xFFFF0055).copy(alpha = 0.2f)), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = Color(0xFFFF0055),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Centralized Alert Banner
            AnimatedVisibility(
                visible = alertBanner != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Notification",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = alertBanner ?: "",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f),
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(
                            onClick = { alertBanner = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Dynamic Content Selection
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> HubTabScreen(context, onNotify = { alertBanner = it })
                    1 -> FFUidTabScreen(onNotify = { alertBanner = it })
                    2 -> ChatTabScreen()
                    3 -> CalendarTabScreen()
                }
            }
        }
    }
}

// TAB 0: HUB SCREEN WITH PERFORMANCE & OVERLAYS & LIVE TERMINAL
@Composable
fun TerminalView() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = BorderStroke(1.dp, Color(0x15FFFFFF)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawDotGrid(Color(0x1000FF66))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(CyberGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, CyberGreen.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Terminal Icon",
                                tint = CyberGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "ACTIVE TERMINAL",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "THREAD_9921_OK",
                        color = CyberGreen.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.alpha(0.85f)
                ) {
                    Text(text = "> INITIALIZING GP_CORE_OVERLAY...", color = CyberGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(text = "> PERMISSION: DISPLAY_OVER_APPS: GRANTED", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(text = "> HOOKING PROCESS: com.dts.freefireth", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(text = "> INJECTING AI_AIM_ASSIST...", color = CyberBlue, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(text = "> SCANNING GUN_SKINS: M1014, SCAR-L, MP40", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
                        Text(text = "INJECTION SUCCESSFUL", color = Color.White, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }

            // Radar/Target decorative lines at the bottom right to replicate HTML overlay mockup
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 24.dp, y = 24.dp)
                    .size(100.dp)
                    .border(BorderStroke(1.dp, CyberGreen.copy(alpha = 0.1f)), CircleShape)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(BorderStroke(1.dp, CyberGreen.copy(alpha = 0.2f)), CircleShape)
                )
            }
        }
    }
}

@Composable
fun HubTabScreen(
    context: Context,
    onNotify: (String) -> Unit
) {
    var fpsSlider by remember { mutableFloatStateOf(60f) }
    var ramAllocationSlider by remember { mutableFloatStateOf(4f) }
    var activeSensitivitySlider by remember { mutableFloatStateOf(85f) }
    var isCalibrating by remember { mutableStateOf(false) }
    var overlayStatusText by remember { mutableStateOf("Pending overlay authorization") }

    val coroutineScope = rememberCoroutineScope()

    // Overlay Permission Checker Activity
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            overlayStatusText = "Overlay permission granted! Visual HUD is available."
            onNotify("Verification success: Special alert systems registered.")
        } else {
            overlayStatusText = "Overlay authorization denied."
            onNotify("Action Required: Please enable permit overlay.")
        }
    }

    LaunchedEffect(Unit) {
        if (Settings.canDrawOverlays(context)) {
            overlayStatusText = "Overlay authorized. Immersive gaming HUD launchable."
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Terminal View Card first
        item {
            TerminalView()
        }

        // Overlay HUD Controller Card with generous corners and custom grid patterns
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                border = BorderStroke(1.dp, Color(0x10FFFFFF)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .drawDotGrid(Color(0x0C00FF66))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IMMERSIVE OVERLAY SERVICES",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.FlipToFront,
                            contentDescription = "Overlay icon",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Allows displaying a telemetry visualizer control window directly over other apps (including Free Fire). Enables quick gun statistics checks on the battlefield.",
                        color = GreyText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Current State: $overlayStatusText",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Request Permission Button
                        Button(
                            onClick = {
                                if (!Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    overlayPermissionLauncher.launch(intent)
                                } else {
                                    onNotify("Perfect: Overlay parameters already provisioned.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x0CFFFFFF)),
                            border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Grant Auth", fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        }

                        // Start Floating service button
                        Button(
                            onClick = {
                                if (Settings.canDrawOverlays(context)) {
                                    val startIntent = Intent(context, FloatingOverlayService::class.java)
                                    context.startService(startIntent)
                                    onNotify("Holographic engine registered! Click the floating controller.")
                                } else {
                                    onNotify("System Alert: Overlay configuration missing in settings. Please authorize first.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Launch Over", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Booster Configuration Dashboard Card with generous corners and custom grid patterns
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                border = BorderStroke(1.dp, Color(0x10FFFFFF)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .drawDotGrid(Color(0x0C00FF66))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "TELEMETRY & SENSITIVITY CALIBRATION",
                        color = CyberGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // FPS Slider
                    Text(
                        text = "Simulated Targets Frequency Level: ${fpsSlider.toInt()} FPS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Slider(
                        value = fpsSlider,
                        onValueChange = { fpsSlider = it },
                        valueRange = 30f..144f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberGreen,
                            activeTrackColor = CyberGreen,
                            inactiveTrackColor = Color(0x1AFFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // RAM allocation
                    Text(
                        text = "Device Simulation Safe Memory: ${ramAllocationSlider.toInt()} GB",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Slider(
                        value = ramAllocationSlider,
                        onValueChange = { ramAllocationSlider = it },
                        valueRange = 2f..16f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberGreen,
                            activeTrackColor = CyberGreen,
                            inactiveTrackColor = Color(0x1AFFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Sensitivity Scale Settings
                    Text(
                        text = "Tactical Laser Precision Index: ${activeSensitivitySlider.toInt()}/100 DPI",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Slider(
                        value = activeSensitivitySlider,
                        onValueChange = { activeSensitivitySlider = it },
                        valueRange = 50f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberGreen,
                            activeTrackColor = CyberGreen,
                            inactiveTrackColor = Color(0x1AFFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Trigger calibration animation
                    Button(
                        onClick = {
                            isCalibrating = true
                            coroutineScope.launch {
                                delay(2000)
                                isCalibrating = false
                                onNotify("Success: Calibration grid generated successfully.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isCalibrating) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = "Calibration", tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("INITIATE DIAGNOSTIC", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Game telemetry indicators
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pin latency card
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    border = BorderStroke(1.dp, Color(0x10FFFFFF)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawDotGrid(Color(0x0600FF66))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(imageVector = Icons.Default.Power, contentDescription = "Ping", tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Text("SIMULATED PING", fontSize = 10.sp, color = GreyText, fontFamily = FontFamily.Monospace)
                        Text("18 ms", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // Battery thermal card
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    border = BorderStroke(1.dp, Color(0x10FFFFFF)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawDotGrid(Color(0x0600FF66))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(imageVector = Icons.Default.Thermostat, contentDescription = "Core thermal", tint = AlertAmber, modifier = Modifier.size(18.dp))
                        Text("THERMAL DIAG", fontSize = 10.sp, color = GreyText, fontFamily = FontFamily.Monospace)
                        Text("34.2 °C", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // YouTube promo card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140D11)),
                border = BorderStroke(1.dp, Color(0xFF4A101D)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openYoutubeChannel(context) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawDotGrid(Color(0xFFFF0033).copy(alpha = 0.05f))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            YouTubeIcon(modifier = Modifier.size(28.dp))
                            Text(
                                text = "GAMERZPLAY NETWORKS",
                                color = Color(0xFFFF3333),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Access elite tactical sensitivity guides, recoil booster calibrations, and live M1887/AWM gaming showcases!",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = "DISCOVER CHANNEL @gamerzplay_8",
                            color = Color(0xFFFF3355),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Navigate to youtube channel",
                        tint = Color(0xFFFF3333).copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp).padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

// TAB 1: FREE FIRE UID STATISTICAL VIEWER
data class FFPlayerData(
    val uid: String,
    val username: String,
    val level: Int,
    val rank: String,
    val winRate: String,
    val headshotRatio: String,
    val playStyle: String,
    val favouriteWeapon: String,
    val guildName: String,
    val guildLeader: String,
    val regionalTitle: String,
    val battleStars: Int
)

@Composable
fun FFUidTabScreen(onNotify: (String) -> Unit) {
    val db = AppDatabase.getDatabase(LocalContext.current)
    val ffPlayerDao = db.ffPlayerDao()

    var rawUid by remember { mutableStateOf("") }
    var currentRecord by remember { mutableStateOf<FFPlayerData?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf("") }
    var databaseChecking by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    if (showEditDialog && currentRecord != null) {
        val record = currentRecord!!
        var editName by remember { mutableStateOf(record.username) }
        var editLevel by remember { mutableStateOf(record.level.toString()) }
        var editRank by remember { mutableStateOf(record.rank) }
        var editWinRate by remember { mutableStateOf(record.winRate) }
        var editHeadshot by remember { mutableStateOf(record.headshotRatio) }
        var editPlaystyle by remember { mutableStateOf(record.playStyle) }
        var editWeapon by remember { mutableStateOf(record.favouriteWeapon) }
        var editGuild by remember { mutableStateOf(record.guildName) }
        var editGuildLeader by remember { mutableStateOf(record.guildLeader) }
        var editTitle by remember { mutableStateOf(record.regionalTitle) }
        var editStars by remember { mutableStateOf(record.battleStars.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = DarkCharcoal,
            tonalElevation = 8.dp,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)), RoundedCornerShape(24.dp))
                .padding(16.dp),
            title = {
                Text(
                    "TELEMETRY OVERRIDE ENGINE",
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "Configure authentic cadet variables below. Saving persists data locally inside the Room SQL Database for instant retrieval.",
                            color = GreyText,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                    item {
                        CustomEditField(label = "CADET NICKNAME", value = editName, onValueChange = { editName = it })
                    }
                    item {
                        CustomEditField(label = "CADET LEVEL", value = editLevel, onValueChange = { editLevel = it })
                    }
                    item {
                        CustomEditField(label = "BATTLE RANK", value = editRank, onValueChange = { editRank = it })
                    }
                    item {
                        CustomEditField(label = "WIN RATE (%)", value = editWinRate, onValueChange = { editWinRate = it })
                    }
                    item {
                        CustomEditField(label = "HEADSHOT SPEED (%)", value = editHeadshot, onValueChange = { editHeadshot = it })
                    }
                    item {
                        CustomEditField(label = "PLAY VECTOR STYLE", value = editPlaystyle, onValueChange = { editPlaystyle = it })
                    }
                    item {
                        CustomEditField(label = "FAVOURITE LOADOUT WEAPON", value = editWeapon, onValueChange = { editWeapon = it })
                    }
                    item {
                        CustomEditField(label = "GUILD REGISTRY", value = editGuild, onValueChange = { editGuild = it })
                    }
                    item {
                        CustomEditField(label = "COMMANDER NICK", value = editGuildLeader, onValueChange = { editGuildLeader = it })
                    }
                    item {
                        CustomEditField(label = "REGIONAL TITLE VECTOR", value = editTitle, onValueChange = { editTitle = it })
                    }
                    item {
                        CustomEditField(label = "BATTLE STARS INDEX", value = editStars, onValueChange = { editStars = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val levelInt = editLevel.toIntOrNull() ?: record.level
                            val starsInt = editStars.toIntOrNull() ?: record.battleStars
                            val updatedRecord = FFPlayerData(
                                uid = record.uid,
                                username = editName,
                                level = levelInt,
                                rank = editRank,
                                winRate = editWinRate,
                                headshotRatio = editHeadshot,
                                playStyle = editPlaystyle,
                                favouriteWeapon = editWeapon,
                                guildName = editGuild,
                                guildLeader = editGuildLeader,
                                regionalTitle = editTitle,
                                battleStars = starsInt
                            )
                            // Save to Room DB
                            val entity = com.example.data.FFPlayerDataEntity(
                                uid = updatedRecord.uid,
                                username = updatedRecord.username,
                                level = updatedRecord.level,
                                rank = updatedRecord.rank,
                                winRate = updatedRecord.winRate,
                                headshotRatio = updatedRecord.headshotRatio,
                                playStyle = updatedRecord.playStyle,
                                favouriteWeapon = updatedRecord.favouriteWeapon,
                                guildName = updatedRecord.guildName,
                                guildLeader = updatedRecord.guildLeader,
                                regionalTitle = updatedRecord.regionalTitle,
                                battleStars = updatedRecord.battleStars
                            )
                            ffPlayerDao.insertPlayer(entity)
                            currentRecord = updatedRecord
                            showEditDialog = false
                            onNotify("Code Synced: authentic telemetry saved locally.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("APPLY CODES", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("DISCARD", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
            border = BorderStroke(1.dp, Color(0x10FFFFFF)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .drawDotGrid(Color(0x0C00FF66))
                    .padding(20.dp)
            ) {
                Text(
                    text = "FREE FIRE STATISTICAL PROPELLER",
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Input any gaming UID (e.g. 881273945 or 912837119) to extract deep battlefield statistics and initiate telemetry account synchronization.",
                    color = GreyText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = rawUid,
                    onValueChange = { rawUid = it },
                    placeholder = { Text("E.g: 882910283", color = Color.Gray, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = "Uid entry",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color(0x1EFFFFFF),
                        focusedContainerColor = Color(0xFF090909),
                        unfocusedContainerColor = Color(0xFF090909),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search Uid database button
                    Button(
                        onClick = {
                            if (rawUid.trim().isEmpty()) {
                                onNotify("Enter a valid numerical user identification code.")
                                return@Button
                            }
                            isSearching = true
                            coroutineScope.launch {
                                // Search Room first
                                val localMatch = ffPlayerDao.getPlayerByUid(rawUid.trim())
                                delay(1200)
                                if (localMatch != null) {
                                    currentRecord = FFPlayerData(
                                        uid = localMatch.uid,
                                        username = localMatch.username,
                                        level = localMatch.level,
                                        rank = localMatch.rank,
                                        winRate = localMatch.winRate,
                                        headshotRatio = localMatch.headshotRatio,
                                        playStyle = localMatch.playStyle,
                                        favouriteWeapon = localMatch.favouriteWeapon,
                                        guildName = localMatch.guildName,
                                        guildLeader = localMatch.guildLeader,
                                        regionalTitle = localMatch.regionalTitle,
                                        battleStars = localMatch.battleStars
                                    )
                                    isSearching = false
                                    onNotify("Authentic profile loaded: ${currentRecord!!.username}")
                                } else {
                                    val calculatedNum = rawUid.hashCode().absoluteValue
                                    val levels = (calculatedNum % 41) + 40 // levels 40-80
                                    val headshot = (calculatedNum % 36) + 25 // 25%-60%
                                    val winrate = (calculatedNum % 31) + 40 // 40%-70%
                                    val ranks = listOf("Grandmaster", "Heroic", "Diamond IV", "Platinum II", "Gold III", "Master")
                                    val weapons = listOf("M1887 Long Wing", "AWM Precision", "MP40 Neon-Fire", "Desert Spirit", "Woodpecker Matrix")
                                    val titles = listOf("Headshot King", "One-Shot Legend", "Desert Combatant", "Ultimate Rusher")
                                    val names = listOf("CRIMINAL_FF", "VULCAN_BOSS", "OP_M1887_KING", "SHADOW_PILOT", "ZEUS_FIGHTER")
                                    
                                    currentRecord = FFPlayerData(
                                        uid = rawUid.trim(),
                                        username = names[calculatedNum % names.size],
                                        level = levels,
                                        rank = ranks[calculatedNum % ranks.size],
                                        winRate = "$winrate%",
                                        headshotRatio = "$headshot%",
                                        playStyle = if (calculatedNum % 2 == 0) "Aggressive Rush" else "Tactical Long-Range Support",
                                        favouriteWeapon = weapons[calculatedNum % weapons.size],
                                        guildName = "LEGION_X_" + (calculatedNum % 99),
                                        guildLeader = "GP_LEADER",
                                        regionalTitle = titles[calculatedNum % titles.size],
                                        battleStars = (calculatedNum % 300) + 120
                                    )
                                    isSearching = false
                                    onNotify("Default generated: Click Edit icon next to name to manually customize profile.")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Query Profile", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Simulated account linkage button
                    Button(
                        onClick = {
                            if (currentRecord == null) {
                                onNotify("Search for a game user profile prior to initiating terminal linkage simulation.")
                                return@Button
                            }
                            databaseChecking = true
                            coroutineScope.launch {
                                delay(1800)
                                databaseChecking = false
                                actionMessage = "Synchronization Secured: Player Node [${currentRecord!!.username}] registered in GP Panel cache. System adjustments are active."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x0CFFFFFF)),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (databaseChecking) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = "Link profile", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Link Dashboard", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Statistics Display screen
        AnimatedVisibility(
            visible = currentRecord != null,
            enter = slideInHorizontally() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val record = currentRecord
            if (record != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            border = BorderStroke(1.dp, Color(0x10FFFFFF)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .drawDotGrid(Color(0x0600FF66))
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = record.username, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = { showEditDialog = true },
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(CyberCyan.copy(alpha = 0.1f), CircleShape)
                                                    .border(BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.3f)), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit profile stats",
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Text(text = "UID: ${record.uid}", color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF090909),
                                        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                                    ) {
                                        Text(
                                            text = "LV: ${record.level}",
                                            color = CyberGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Divider(color = Color(0x10FFFFFF), modifier = Modifier.padding(vertical = 14.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("BATTLE RANK", color = CyberSlateGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                        Text(record.rank, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("HEADSHOT RATE", color = CyberSlateGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                        Text(record.headshotRatio, color = CyberCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("WIN RATIO", color = CyberSlateGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                        Text(record.winRate, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("PLAY STYLE", color = CyberSlateGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                        Text(record.playStyle, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            border = BorderStroke(1.dp, Color(0x10FFFFFF)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .drawDotGrid(Color(0x0600FF66))
                                    .padding(18.dp)
                            ) {
                                Text("CLAN INTELLIGENCE CORE", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Guild Affiliation:", color = GreyText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text(record.guildName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Commander:", color = GreyText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text(record.guildLeader, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Regional Title:", color = GreyText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text(record.regionalTitle, color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    if (actionMessage.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = CyberGreen.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Check", tint = CyberGreen, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = actionMessage,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomEditField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            color = CyberSlateGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = Color(0x15FFFFFF),
                focusedContainerColor = Color(0xFF090909),
                unfocusedContainerColor = Color(0xFF090909)
            )
        )
    }
}


// TAB 2: AI ASSISTANT CHATROOM COMPOSABLE
@Composable
fun ChatTabScreen() {
    var rawInputMessage by remember { mutableStateOf("") }
    var incomingLoader by remember { mutableStateOf(false) }
    val chatHistoryRecord = remember {
        mutableStateListOf(
            ChatMessage(text = "Greetings Master Cadet! Welcome back to GP PANEL control systems. I am your specialized gaming telemetry neural AI companion.", isUser = false),
            ChatMessage(text = "Try asking about weapon recoil control, system optimization vectors, Free Fire level analysis guidelines, or booster calibrations.", isUser = false)
        )
    }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // Conversation log box with cyberpunk grid mesh underlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF090909), RoundedCornerShape(24.dp))
                .border(BorderStroke(1.dp, Color(0x10FFFFFF)), RoundedCornerShape(24.dp))
                .drawDotGrid(Color(0x0600FF66))
                .padding(14.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (incomingLoader) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = CyberGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("AI CORE INJECTING NEUTRAL FREQUENCY...", color = CyberGreen.copy(alpha = 0.7f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Display lists in reverse since layout is reversed for chat ergonomics
                val messageList = chatHistoryRecord.reversed()
                items(messageList) { message ->
                    val alignment = if (message.isUser) Alignment.End else Alignment.Start
                    
                    // Asymmetric futuristic corner styling
                    val bubbleShape = if (message.isUser) {
                        RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                    } else {
                        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                    }

                    val bubbleBg = if (message.isUser) Color(0x3322C55E) else DarkCharcoal
                    val bubbleBorder = if (message.isUser) CyberGreen.copy(alpha = 0.4f) else Color(0x10FFFFFF)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = alignment
                    ) {
                        Surface(
                            shape = bubbleShape,
                            color = bubbleBg,
                            border = BorderStroke(1.dp, bubbleBorder),
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (message.isUser) "CADET NODE" else "GP PANEL NEURAL CORE",
                                    color = if (message.isUser) CyberGreen else CyberCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.text,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    fontFamily = if (message.isUser) FontFamily.SansSerif else FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset command chips with custom modern styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Sens Calibration",
                "M1887 Recoil",
                "AWM Scope Booster"
            ).forEach { tag ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x0CFFFFFF),
                    border = BorderStroke(1.dp, Color(0x15FFFFFF)),
                    modifier = Modifier.clickable {
                        rawInputMessage = when(tag) {
                            "Sens Calibration" -> "How do I calibrate my tactical laser sensitivity to exactly 95 DPI?"
                            "M1887 Recoil" -> "Explain recoil calibrations for M1887 Shotgun model."
                            "AWM Scope Booster" -> "Give me parameters for optimizing AWM scope response time."
                            else -> ""
                        }
                    }
                ) {
                    Text(
                        text = tag,
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Message input interface
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = rawInputMessage,
                onValueChange = { rawInputMessage = it },
                placeholder = { Text("Command neural engine...", color = Color.Gray, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberGreen,
                    unfocusedBorderColor = Color(0x1EFFFFFF),
                    focusedContainerColor = Color(0xFF090909),
                    unfocusedContainerColor = Color(0xFF090909),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = {
                    val prompt = rawInputMessage.trim()
                    if (prompt.isEmpty() || incomingLoader) return@IconButton
                    
                    val userMsg = ChatMessage(text = prompt, isUser = true)
                    chatHistoryRecord.add(userMsg)
                    rawInputMessage = ""
                    incomingLoader = true

                    coroutineScope.launch {
                        val aiResponse = GeminiRepository.fetchGeminiResponse(prompt, chatHistoryRecord.toList())
                        chatHistoryRecord.add(
                            ChatMessage(text = aiResponse, isUser = false)
                        )
                        incomingLoader = false
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = CyberGreen),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


// TAB 3: CALENDAR REMINDERS & NOTE PERSISTENCE SCREEN WITH ROOM
@Composable
fun CalendarTabScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Retrieve database objects safely
    val db = remember { AppDatabase.getDatabase(context) }
    val reminderDao = remember { db.noteReminderDao() }

    val fullReminderList by reminderDao.getAllReminders()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var noteTitleInput by remember { mutableStateOf("") }
    var noteContentInput by remember { mutableStateOf("") }

    // Calendar management state variables
    val calendar = remember { Calendar.getInstance() }
    val dayFormatter = remember { SimpleDateFormat("dd", Locale.getDefault()) }
    val monthNameFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dbDateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var selectedCalendarDateString by remember { mutableStateOf(dbDateFormatter.format(Date())) }

    // Prepopulate days of the current month
    val daysOfTheMonth = remember {
        val days = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val targetMonth = cal.get(Calendar.MONTH)
        while (cal.get(Calendar.MONTH) == targetMonth) {
            days.add(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        days
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // Scheduler Month Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthNameFormatter.format(calendar.time).uppercase(),
                color = CyberCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Day logo",
                tint = CyberCyan,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Row of Days with immersive grid mesh background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF090909), RoundedCornerShape(24.dp))
                .border(BorderStroke(1.dp, Color(0x10FFFFFF)), RoundedCornerShape(24.dp))
                .padding(10.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    Row(
                        modifier = Modifier.drawDotGrid(Color(0x0C00FF66)),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        daysOfTheMonth.take(16).forEach { date ->
                            val isSelected = dbDateFormatter.format(date) == selectedCalendarDateString
                            val dayNum = dayFormatter.format(date)

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) CyberCyan else DarkCharcoal
                                ),
                                border = BorderStroke(1.dp, if (isSelected) CyberCyan else Color(0x1EFFFFFF)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable {
                                        selectedCalendarDateString = dbDateFormatter.format(date)
                                      }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reminders for Selected Date
        val filteredReminders = fullReminderList.filter { it.date == selectedCalendarDateString }

        Text(
            text = "NOTIFICATIONS SCHEDULE: ${selectedCalendarDateString.uppercase()}",
            color = CyberSlateGray,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredReminders.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        border = BorderStroke(1.dp, Color(0x10FFFFFF)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = "Empty notes", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Zero task schedule logged on this security channel node.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            } else {
                items(filteredReminders) { note ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                        border = BorderStroke(1.dp, if (note.isCompleted) CyberGreen.copy(alpha = 0.5f) else Color(0x10FFFFFF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = note.isCompleted,
                                onCheckedChange = { complete ->
                                    coroutineScope.launch {
                                        reminderDao.updateCompletionStatus(note.id, complete)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CyberGreen,
                                    uncheckedColor = Color.Gray
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = note.content,
                                    color = GreyText,
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        reminderDao.deleteReminderById(note.id)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete task",
                                    tint = ErrorCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Create New Notes Reminder Drawer Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
            border = BorderStroke(1.dp, Color(0x10FFFFFF)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .drawDotGrid(Color(0x0600FF66))
                    .padding(16.dp)
            ) {
                Text(
                    text = "LOG NEW BATTLE REMINDER NOTES",
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = noteTitleInput,
                        onValueChange = { noteTitleInput = it },
                        placeholder = { Text("M1887 Guide", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1.1f),
                        maxLines = 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0x1EFFFFFF),
                            focusedContainerColor = Color(0xFF090909),
                            unfocusedContainerColor = Color(0xFF090909)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = noteContentInput,
                        onValueChange = { noteContentInput = it },
                        placeholder = { Text("Laser calibration...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1.4f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0x1EFFFFFF),
                            focusedContainerColor = Color(0xFF090909),
                            unfocusedContainerColor = Color(0xFF090909)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (noteTitleInput.trim().isEmpty() || noteContentInput.trim().isEmpty()) return@Button
                        coroutineScope.launch {
                            reminderDao.insertReminder(
                                NoteReminder(
                                    title = noteTitleInput.trim(),
                                    content = noteContentInput.trim(),
                                    date = selectedCalendarDateString
                                )
                            )
                            noteTitleInput = ""
                            noteContentInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ADD NOTE REMINDER LOG", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
