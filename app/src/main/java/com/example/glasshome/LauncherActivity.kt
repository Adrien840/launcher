package com.example.glasshome

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.qmdeve.liquidglass.widget.LiquidGlassView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private data class LaunchApp(val label: String, val packageName: String, val icon: ImageBitmap)
private data class WallpaperPreset(val name: String, val colors: List<Color>)

private val wallpaperPresets = listOf(
    WallpaperPreset("Midnight", listOf(Color(0xFF536D87), Color(0xFF14283E), Color(0xFF060B14))),
    WallpaperPreset("Aurora", listOf(Color(0xFF7C6BC2), Color(0xFF263C75), Color(0xFF091525))),
    WallpaperPreset("Rose", listOf(Color(0xFFDF9DAB), Color(0xFF764C75), Color(0xFF21182F))),
    WallpaperPreset("Ocean", listOf(Color(0xFF5DC4D5), Color(0xFF176B89), Color(0xFF071C35))),
    WallpaperPreset("Sage", listOf(Color(0xFFB5C9A2), Color(0xFF4A746C), Color(0xFF122D35))),
    WallpaperPreset("Sunset", listOf(Color(0xFFFFB46E), Color(0xFFB85E75), Color(0xFF32264F))),
    WallpaperPreset("Glacier", listOf(Color(0xFFD7E8F5), Color(0xFF739AB9), Color(0xFF263B61)))
)

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent { GlassLauncher() }
    }
}

@Composable
private fun GlassLauncher() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("glass_home", Context.MODE_PRIVATE) }
    val apps = remember { mutableStateOf<List<LaunchApp>>(emptyList()) }
    val showDrawer = remember { mutableStateOf(false) }
    val showCustomize = remember { mutableStateOf(false) }
    val query = remember { mutableStateOf("") }
    val presetId = remember { mutableIntStateOf(prefs.getInt("wallpaper_preset", 0)) }
    val photoUri = remember { mutableStateOf(prefs.getString("wallpaper_uri", null)) }
    val columns = remember { mutableIntStateOf(prefs.getInt("columns", 4)) }
    val compact = remember { mutableStateOf(prefs.getBoolean("compact", false)) }
    val pickedBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    var now by remember { mutableStateOf(Date()) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            photoUri.value = uri.toString()
            prefs.edit().putString("wallpaper_uri", uri.toString()).apply()
        }
    }

    LaunchedEffect(Unit) {
        apps.value = withContext(Dispatchers.IO) { loadLauncherApps(context) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(30_000)
        }
    }
    LaunchedEffect(photoUri.value) {
        val uri = photoUri.value?.let(Uri::parse)
        pickedBitmap.value = if (uri == null) null else withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }

    val selectedPreset = wallpaperPresets[presetId.intValue.coerceIn(wallpaperPresets.indices)]
    val dockApps = remember(apps.value) { chooseDockApps(apps.value) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            createLauncherHost(
                context = ctx,
                apps = apps,
                showDrawer = showDrawer,
                query = query,
                showCustomize = showCustomize,
                preset = presetId,
                columns = columns,
                compact = compact,
                photo = pickedBitmap,
                date = { now },
                onLaunch = { launchApp(ctx, it.packageName) },
                onPickPhoto = { photoPicker.launch(arrayOf("image/*")) },
                onPreset = { id ->
                    presetId.intValue = id
                    photoUri.value = null
                    prefs.edit().putInt("wallpaper_preset", id).remove("wallpaper_uri").apply()
                },
                onColumns = { value ->
                    columns.intValue = value
                    prefs.edit().putInt("columns", value).apply()
                },
                onCompact = { value ->
                    compact.value = value
                    prefs.edit().putBoolean("compact", value).apply()
                },
                dockApps = dockApps
            )
        },
        update = { host ->
            host.updateWallpaper(pickedBitmap.value, selectedPreset.colors)
            host.updateDockApps(dockApps, showDrawer.value, onLaunch = { launchApp(context, it.packageName) })
        }
    )
}

private class GlassLauncherHost(context: Context) : FrameLayout(context) {
    val contentLayer = FrameLayout(context)
    val glassDock: LiquidGlassView = LiquidGlassView(context)
    val contentCompose = ComposeView(context)
    val dockCompose = ComposeView(context)

    init {
        clipChildren = false
        clipToPadding = false
        addView(contentLayer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        contentLayer.addView(contentCompose, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        glassDock.setCornerRadius(dp(42f).toFloat())
        glassDock.setRefractionHeight(dp(30f).toFloat())
        glassDock.setRefractionOffset(dp(72f).toFloat())
        glassDock.setDispersion(0.88f)
        glassDock.setBlurRadius(0.45f)
        glassDock.setTintAlpha(0.025f)
        glassDock.setTintColorRed(0.91f)
        glassDock.setTintColorGreen(0.96f)
        glassDock.setTintColorBlue(1.0f)
        glassDock.setTouchEffectEnabled(true)
        glassDock.bind(contentLayer)

        val dockParams = FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            dp(102f),
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply {
            leftMargin = dp(16f)
            rightMargin = dp(16f)
            bottomMargin = dp(38f)
        }
        addView(glassDock, dockParams)
        glassDock.addView(
            dockCompose,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    fun updateWallpaper(bitmap: ImageBitmap?, colors: List<Color>) {
        contentCompose.invalidate()
    }

    fun updateDockApps(apps: List<LaunchApp>, drawer: Boolean, onLaunch: (LaunchApp) -> Unit) {
        dockCompose.invalidate()
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
}

private fun createLauncherHost(
    context: Context,
    apps: MutableState<List<LaunchApp>>,
    showDrawer: MutableState<Boolean>,
    query: MutableState<String>,
    showCustomize: MutableState<Boolean>,
    preset: MutableIntState,
    columns: MutableIntState,
    compact: MutableState<Boolean>,
    photo: MutableState<ImageBitmap?>,
    date: () -> Date,
    onLaunch: (LaunchApp) -> Unit,
    onPickPhoto: () -> Unit,
    onPreset: (Int) -> Unit,
    onColumns: (Int) -> Unit,
    onCompact: (Boolean) -> Unit,
    dockApps: List<LaunchApp>
): GlassLauncherHost {
    val host = GlassLauncherHost(context)
    host.contentCompose.setContent {
        HomeContent(
            apps = apps.value,
            showDrawer = showDrawer.value,
            onDrawerChange = { showDrawer.value = it; query.value = "" },
            query = query.value,
            onQuery = { query.value = it },
            showCustomize = showCustomize.value,
            onCustomize = { showCustomize.value = it },
            preset = preset.intValue,
            columns = columns.intValue,
            compact = compact.value,
            photo = photo.value,
            date = date(),
            onLaunch = onLaunch,
            onPickPhoto = onPickPhoto,
            onPreset = onPreset,
            onColumns = onColumns,
            onCompact = onCompact
        )
    }
    host.dockCompose.setContent {
        DockContent(apps = chooseDockApps(apps.value), onLaunch = onLaunch, onDrawer = {
            showDrawer.value = !showDrawer.value
            query.value = ""
        })
    }
    return host
}

@Composable
private fun HomeContent(
    apps: List<LaunchApp>,
    showDrawer: Boolean,
    onDrawerChange: (Boolean) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    showCustomize: Boolean,
    onCustomize: (Boolean) -> Unit,
    preset: Int,
    columns: Int,
    compact: Boolean,
    photo: ImageBitmap?,
    date: Date,
    onLaunch: (LaunchApp) -> Unit,
    onPickPhoto: () -> Unit,
    onPreset: (Int) -> Unit,
    onColumns: (Int) -> Unit,
    onCompact: (Boolean) -> Unit
) {
    val presetColors = wallpaperPresets[preset.coerceIn(wallpaperPresets.indices)].colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(presetColors, start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset(900f, 1600f)))
            .then(if (!showDrawer) Modifier.pointerInput(Unit) {
                var drag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { change, amount ->
                        drag += amount
                        if (abs(drag) > 76f && drag < 0f) {
                            onDrawerChange(true)
                            drag = 0f
                            change.consume()
                        }
                    },
                    onDragEnd = { drag = 0f },
                    onDragCancel = { drag = 0f }
                )
            } else Modifier)
    ) {
        if (photo != null) {
            Image(bitmap = photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .18f)))
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .32f)))))
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("GLASS HOME", color = Color.White.copy(alpha = .68f), fontSize = 11.sp, letterSpacing = 2.1.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
                Text("✧", color = Color.White, fontSize = 25.sp, modifier = Modifier.clip(CircleShape).clickable { onCustomize(true) }.padding(7.dp))
            }

            Spacer(Modifier.height(31.dp))
            Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(date), color = Color.White.copy(alpha = .9f), fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif)
            Text(SimpleDateFormat("h:mm", Locale.getDefault()).format(date), color = Color.White, fontSize = 76.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-3).sp, fontFamily = FontFamily.SansSerif)

            AnimatedVisibility(
                visible = showDrawer,
                modifier = Modifier.fillMaxWidth().weight(1f),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 12 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 12 })
            ) {
                Column(Modifier.fillMaxSize()) {
                    Text("Your apps", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 13.dp))
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .16f)).padding(horizontal = 15.dp, vertical = 13.dp)) {
                        if (query.isEmpty()) Text("Search your apps", color = Color.White.copy(alpha = .62f), fontSize = 14.sp)
                        BasicTextField(value = query, onValueChange = onQuery, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.SansSerif), modifier = Modifier.fillMaxWidth())
                    }
                    val filtered = remember(apps, query) { apps.filter { it.label.contains(query.trim(), true) } }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app -> AppTile(app, compact, onLaunch) }
                    }
                }
            }

            if (!showDrawer) {
                Spacer(Modifier.weight(1f))
                val shortcuts = remember(apps) { chooseDockApps(apps) }
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    shortcuts.forEach { AppTile(it, compact, onLaunch, showLabel = true) }
                }
                Text("Swipe up to explore", color = Color.White.copy(alpha = .68f), fontSize = 12.sp, letterSpacing = .3.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
        }

        if (showCustomize) {
            CustomizePanel(
                selectedPreset = preset,
                columns = columns,
                compact = compact,
                onClose = { onCustomize(false) },
                onPreset = onPreset,
                onPickPhoto = onPickPhoto,
                onColumns = onColumns,
                onCompact = onCompact
            )
        }
    }
}

@Composable
private fun CustomizePanel(
    selectedPreset: Int,
    columns: Int,
    compact: Boolean,
    onClose: () -> Unit,
    onPreset: (Int) -> Unit,
    onPickPhoto: () -> Unit,
    onColumns: (Int) -> Unit,
    onCompact: (Boolean) -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .48f)), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Color(0xF21A2331)).padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 30.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Personalize", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("Make this home screen yours", color = Color.White.copy(alpha = .62f), fontSize = 13.sp)
                }
                Text("Done", color = Color(0xFFB9D9FF), fontSize = 14.sp, modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("WALLPAPER", color = Color.White.copy(alpha = .6f), fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                wallpaperPresets.forEachIndexed { index, item ->
                    Column(Modifier.weight(1f).clickable { onPreset(index) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(13.dp))
                                .background(Brush.verticalGradient(item.colors)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedPreset == index) Text("✓", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(item.name, color = Color.White.copy(alpha = .8f), fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                Text("Choose a photo from gallery")
            }
            Spacer(Modifier.height(19.dp))
            Text("APP DRAWER", color = Color.White.copy(alpha = .6f), fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                listOf(4, 5).forEach { count ->
                    FilterChip(selected = columns == count, onClick = { onColumns(count) }, label = { Text("$count columns") })
                }
                FilterChip(selected = compact, onClick = { onCompact(!compact) }, label = { Text("Compact icons") })
            }
        }
    }
}

@Composable
private fun DockContent(apps: List<LaunchApp>, onLaunch: (LaunchApp) -> Unit, onDrawer: () -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        apps.take(4).forEach { app ->
            Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(55.dp).clip(RoundedCornerShape(17.dp)).clickable { onLaunch(app) })
        }
        Box(
            Modifier.size(53.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .13f)).clickable(onClick = onDrawer),
            contentAlignment = Alignment.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) { Box(Modifier.size(4.dp).background(Color.White, CircleShape)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTile(app: LaunchApp, compact: Boolean, onLaunch: (LaunchApp) -> Unit, showLabel: Boolean = true) {
    Column(
        Modifier.widthIn(min = 55.dp).clickable { onLaunch(app) }.padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(if (compact) 43.dp else 51.dp).clip(RoundedCornerShape(15.dp)))
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(app.label, color = Color.White.copy(alpha = .95f), fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center, fontFamily = FontFamily.SansSerif)
        }
    }
}

private fun loadLauncherApps(context: Context): List<LaunchApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0).mapNotNull { info ->
        runCatching {
            LaunchApp(info.loadLabel(pm).toString(), info.activityInfo.packageName, drawableToBitmap(info.loadIcon(pm)).asImageBitmap())
        }.getOrNull()
    }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
}

private fun chooseDockApps(apps: List<LaunchApp>): List<LaunchApp> {
    val patterns = listOf(
        listOf("phone", "dialer"),
        listOf("messages", "messaging", "sms"),
        listOf("chrome", "browser", "internet"),
        listOf("camera")
    )
    val result = patterns.mapNotNull { words -> apps.firstOrNull { app -> words.any { app.label.contains(it, true) || app.packageName.contains(it, true) } } }.distinctBy { it.packageName }.toMutableList()
    apps.forEach { if (result.size < 4 && result.none { item -> item.packageName == it.packageName }) result += it }
    return result.take(4)
}

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap {
    val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(256)
    val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(256)
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    return bitmap
}
