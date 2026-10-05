package com.example.routerproxy

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DarkGlassColors = darkColorScheme(
    background = Color.Transparent,
    surface = Color.Transparent,
    onBackground = Color.White,
    onSurface = Color.White,
    primary = Color.White,
    onPrimary = Color(0xFF11131C)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = DarkGlassColors) {
                RouterProxyScreen(preferences)
            }
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "router_proxy_preferences"
    }
}

@Composable
private fun RouterProxyScreen(preferences: android.content.SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(preferences.getBoolean(KEY_PROXY_ENABLED, false)) }
    var enabledAt by remember { mutableStateOf(preferences.getLong(KEY_ENABLED_AT, 0L)) }
    var token by remember { mutableStateOf(preferences.getString(KEY_TOKEN, "").orEmpty()) }
    var routerIp by remember { mutableStateOf(preferences.getString(KEY_ROUTER_IP, DEFAULT_ROUTER_IP).orEmpty()) }
    var elapsed by remember { mutableStateOf(0L) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (enabled) "Проксирование включено" else "Готово к управлению") }
    var statusIsError by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(readLogs(preferences)) }
    var showSettings by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }

    LaunchedEffect(enabled, enabledAt) {
        while (enabled) {
            elapsed = System.currentTimeMillis() - enabledAt
            delay(1000)
        }
        elapsed = 0L
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LiquidBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("OpenWrt Proxy", color = Color.White.copy(alpha = 0.95f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Управление Podkop через роутер",
                            color = Color.White.copy(alpha = 0.52f),
                            fontSize = 7.sp
                        )
                    }
                    GlassIconButton("⚙", "Настройки") { showSettings = true }
                }

                Text(
                    text = routerIp,
                    modifier = Modifier.padding(top = 14.dp),
                    color = Color.White.copy(alpha = 0.46f),
                    fontSize = 7.sp
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val pressedSource = remember { MutableInteractionSource() }
                    val pressed by pressedSource.collectIsPressedAsState()
                    val scale = if (pressed) 0.95f else 1f

                    GlassLens(
                        modifier = Modifier
                            .size(150.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clickable(
                                interactionSource = pressedSource,
                                indication = null
                            ) {
                                if (loading) return@clickable
                                if (token.isBlank()) {
                                    status = "Введите токен в настройках"
                                    statusIsError = true
                                    showSettings = true
                                    return@clickable
                                }

                                val targetState = !enabled
                                loading = true
                                status = "Подключение к роутеру…"
                                statusIsError = false
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        sendProxyRequest(routerIp, token, targetState)
                                    }
                                    loading = false
                                    result.onSuccess {
                                        enabled = targetState
                                        enabledAt = if (targetState) System.currentTimeMillis() else 0L
                                        preferences.edit()
                                            .putBoolean(KEY_PROXY_ENABLED, targetState)
                                            .putLong(KEY_ENABLED_AT, enabledAt)
                                            .apply()
                                        status = if (targetState) "Проксирование включено" else "Проксирование выключено"
                                        statusIsError = false
                                        logs = addLog(
                                            preferences,
                                            "${if (targetState) "Включение" else "Выключение"} проксирования: успешно"
                                        )
                                    }.onFailure { error ->
                                        status = "Ошибка HTTP: ${error.message ?: "неизвестная ошибка"}"
                                        statusIsError = true
                                        logs = addLog(
                                            preferences,
                                            "${if (targetState) "Включение" else "Выключение"} проксирования: ошибка"
                                        )
                                    }
                                }
                            }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (enabled) "ВКЛЮЧЕНО" else "ВЫКЛЮЧЕНО",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                formatDuration(elapsed),
                                modifier = Modifier.padding(top = 5.dp),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White
                            )
                            Text(
                                if (loading) "СИНХРОНИЗАЦИЯ" else "НАЖМИТЕ ДЛЯ ПЕРЕКЛЮЧЕНИЯ",
                                modifier = Modifier.padding(top = 6.dp),
                                color = Color.White.copy(alpha = 0.50f),
                                fontSize = 5.sp,
                                letterSpacing = 0.7.sp
                            )
                        }
                    }
                }

                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    contentPadding = 8.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (statusIsError) Color(0xFFE2BABA) else Color(0xFF4ADE80))
                        )
                        Text(
                            status,
                            modifier = Modifier.padding(start = 7.dp),
                            color = if (statusIsError) Color(0xFFE2BABA) else Color.White.copy(alpha = 0.82f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    GlassActionButton("◉  ЛОГИ", Modifier.weight(1f)) { showLogs = true }
                    GlassActionButton("⚙  НАСТРОЙКИ", Modifier.weight(1f)) { showSettings = true }
                }
            }


        Text(
            text = "⋮",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 46.dp),
            color = Color.White.copy(alpha = 0.70f),
            fontSize = 18.sp
        )
    }
    if (showSettings) {
        SettingsDialog(
            routerIp = routerIp,
            token = token,
            onDismiss = { showSettings = false },
            onSave = { newIp, newToken ->
                routerIp = newIp
                token = newToken
                preferences.edit().putString(KEY_ROUTER_IP, newIp).putString(KEY_TOKEN, newToken).apply()
                logs = addLog(preferences, "Настройки подключения сохранены")
                status = "Настройки сохранены"
                statusIsError = false
                showSettings = false
            }
        )
    }

    if (showLogs) {
        LogsDialog(logs = logs, onDismiss = { showLogs = false }, onClear = {
            preferences.edit().remove(KEY_LOGS).apply()
            logs = emptyList()
        })
    }
}

@Composable
private fun LiquidBackground() {
    val transition = rememberInfiniteTransition(label = "dark-background")
    val drift by transition.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            tween(20_000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "subtle-highlight-drift"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .blur(38.dp)
    ) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0A0A0C), Color(0xFF000000)),
                startY = 0f,
                endY = size.height
            )
        )
        drawOrb(
            this,
            size.width * (0.50f + drift),
            size.height * 0.08f,
            420.dp.toPx(),
            Color(0xFF1A2233).copy(alpha = 0.35f)
        )
    }
}

private fun drawOrb(scope: DrawScope, x: Float, y: Float, radius: Float, color: Color) {
    scope.drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            radius = radius
        ),
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(x, y)
    )
}

fun Modifier.darkLiquidGlass(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    fillAlpha: Float = 0.05f,
    borderAlpha: Float = 0.10f
): Modifier = this
    .clip(shape)
    .background(Color.White.copy(alpha = fillAlpha), shape)
    .border(
        BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = borderAlpha + 0.12f),
                    Color.White.copy(alpha = borderAlpha)
                )
            )
        ),
        shape
    )

@Composable
private fun GlassSurface(
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    contentPadding: Dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .darkLiquidGlass(shape)
            .padding(contentPadding)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(20.dp)
                .background(Color.White.copy(alpha = 0.035f))
        )
        content()
    }
}

@Composable
private fun GlassLens(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .blur(40.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f), CircleShape)
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.50f),
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    ),
                    CircleShape
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .blur(30.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
private fun GlassActionButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = modifier
            .darkLiquidGlass(RoundedCornerShape(50), fillAlpha = 0.06f, borderAlpha = 0.12f)
            .graphicsLayer {
                scaleX = if (pressed) 0.96f else 1f
                scaleY = if (pressed) 0.96f else 1f
            }
            .clickable(source, indication = null, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(20.dp)
                .background(Color.White.copy(alpha = 0.04f))
        )
        Text(
            text,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GlassIconButton(text: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .darkLiquidGlass(CircleShape, fillAlpha = 0.08f, borderAlpha = 0.10f)
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White.copy(alpha = 0.80f), fontSize = 20.sp)
    }
}
@Composable
private fun SettingsDialog(
    routerIp: String,
    token: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var ipValue by remember { mutableStateOf(routerIp) }
    var tokenValue by remember { mutableStateOf(token) }
    DialogSurface(onDismiss = onDismiss) {
        Text("Настройки подключения", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text("Адрес роутера и токен CGI", color = Color.White.copy(0.62f), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
        OutlinedTextField(value = ipValue, onValueChange = { ipValue = it }, label = { Text("IP роутера", color = Color.White.copy(alpha = 0.75f)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        OutlinedTextField(value = tokenValue, onValueChange = { tokenValue = it }, label = { Text("Токен", color = Color.White.copy(alpha = 0.75f)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Отмена", color = Color.White.copy(0.7f)) }
            Button(onClick = { if (ipValue.isNotBlank() && tokenValue.isNotBlank()) onSave(ipValue.trim(), tokenValue) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.20f), contentColor = Color.White)) { Text("Сохранить") }
        }
    }
}

@Composable
private fun LogsDialog(logs: List<String>, onDismiss: () -> Unit, onClear: () -> Unit) {
    DialogSurface(onDismiss = onDismiss) {
        Text("Логи запросов", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        if (logs.isEmpty()) {
            Text("Логов пока нет", color = Color.White.copy(0.62f), modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs) { line -> Text(line, color = Color.White.copy(0.80f), fontSize = 12.sp) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClear) { Text("Очистить", color = Color.White.copy(0.7f)) }
            TextButton(onClick = onDismiss) { Text("Закрыть", color = Color.White) }
        }
    }
}

@Composable
private fun DialogSurface(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .wrapContentHeight()
                .darkLiquidGlass(RoundedCornerShape(30.dp), fillAlpha = 0.14f, borderAlpha = 0.18f)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

private fun sendProxyRequest(routerIp: String, token: String, enabled: Boolean): Result<Unit> {
    val encodedToken = URLEncoder.encode(token, Charsets.UTF_8.name())
    val mode = if (enabled) "on" else "off"
    val url = URL("http://$routerIp/cgi-bin/proxy?mode=$mode&token=$encodedToken")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = REQUEST_TIMEOUT_MS
        readTimeout = REQUEST_TIMEOUT_MS
        instanceFollowRedirects = false
    }
    return try {
        val responseCode = connection.responseCode
        if (responseCode in 200..299) Result.success(Unit) else Result.failure(IOException("HTTP $responseCode"))
    } catch (error: Exception) {
        Result.failure(error)
    } finally {
        connection.disconnect()
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis.coerceAtLeast(0L) / 1000L
    return String.format(Locale.US, "%02d:%02d:%02d", totalSeconds / 3600L, (totalSeconds % 3600L) / 60L, totalSeconds % 60L)
}

private fun readLogs(preferences: android.content.SharedPreferences): List<String> =
    preferences.getString(KEY_LOGS, "").orEmpty().lineSequence().filter { it.isNotBlank() }.toList()

private fun addLog(preferences: android.content.SharedPreferences, message: String): List<String> {
    val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
    val logs = (listOf("$timestamp — $message") + readLogs(preferences)).take(MAX_LOG_LINES)
    preferences.edit().putString(KEY_LOGS, logs.joinToString("\n")).apply()
    return logs
}

private const val DEFAULT_ROUTER_IP = "192.168.1.1"
private const val KEY_TOKEN = "token"
private const val KEY_ROUTER_IP = "router_ip"
private const val KEY_PROXY_ENABLED = "proxy_enabled"
private const val KEY_ENABLED_AT = "enabled_at"
private const val KEY_LOGS = "logs"
private const val MAX_LOG_LINES = 100
private const val REQUEST_TIMEOUT_MS = 10_000