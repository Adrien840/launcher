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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
    val filtered = remember(apps, query) { apps.filter { it.label.contains(query.trim(), true) } }
    val folders = remember(apps) {
        listOf(
            "Samsung" to listOf("samsung", "galaxy", "one ui"),
            "Google" to listOf("google", "gmail", "youtube", "maps", "chrome"),
            "Social" to listOf("instagram", "facebook", "messenger", "snapchat", "discord", "tiktok"),
            "Media" to listOf("spotify", "music", "netflix", "photos", "gallery")
        ).map { (name, keys) -> name to apps.filter { app -> keys.any { key -> app.label.contains(key, true) || app.packageName.contains(key, true) } }.distinctBy { it.packageName }.take(9) }
            .filter { it.second.isNotEmpty() }
    }
    var selectedFolder by remember { mutableStateOf<Pair<String, List<LaunchApp>>?>(null) }
    var activePage by remember { mutableIntStateOf(0) }

    Box(
        Modifier.fillMaxSize().background(Brush.linearGradient(presetColors))
            .then(if (!showDrawer) Modifier.pointerInput(showDrawer) {
                var drag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { change, amount ->
                        drag += amount
                        if (drag < -72f) {
                            onDrawerChange(true)
                            drag = 0f
                            change.consume()
                        }
                    },
                    onDragEnd = { drag = 0f },
                    onDragCancel = { drag = 0f }
                )
            } else Modifier)
            .pointerInput(showDrawer, activePage) {
                if (!showDrawer) {
                    var dragX = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            dragX += amount
                            if (dragX < -72f) { activePage = 1; dragX = 0f; change.consume() }
                            if (dragX > 72f) { activePage = 0; dragX = 0f; change.consume() }
                        },
                        onDragEnd = { dragX = 0f },
                        onDragCancel = { dragX = 0f }
                    )
                }
            }
    ) {
        if (photo != null) {
            Image(photo, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f)))
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.05f), Color.Transparent, Color.Black.copy(.46f)))))
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 9.dp, bottom = 124.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Compact One UI-style greeting and customization controls.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(SimpleDateFormat("EEEE", Locale.getDefault()).format(date).uppercase(Locale.getDefault()),
                        color = Color.White.copy(.68f), fontSize = 10.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.SemiBold)
                    Text(SimpleDateFormat("MMMM d", Locale.getDefault()).format(date),
                        color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Medium, letterSpacing = (-.4).sp)
                }
                GlassCircleButton("✧", "Personalize") { onCustomize(true) }
                Spacer(Modifier.width(8.dp))
                GlassCircleButton("⋮", "Home menu") { onCustomize(true) }
            }

            Spacer(Modifier.height(17.dp))

            // Two large glance cards, following One UI's rounded widget language.
            Row(Modifier.fillMaxWidth().height(142.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                CalendarWidget(Modifier.weight(1.12f), date)
                WeatherWidget(Modifier.weight(.88f), onClick = { onCustomize(true) })
            }

            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("FOLDERS", color = Color.White.copy(.62f), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Edit", color = Color.White.copy(.75f), fontSize = 11.sp, modifier = Modifier.clickable { onCustomize(true) }.padding(5.dp))
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                folders.take(4).forEach { (name, items) ->
                    FolderTile(name, items, Modifier.weight(1f)) { selectedFolder = name to items }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (activePage == 0) "Apps" else "More apps", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("All apps  ›", color = Color.White.copy(.75f), fontSize = 11.sp,
                    modifier = Modifier.clickable { onDrawerChange(true) }.padding(5.dp))
            }
            Spacer(Modifier.height(9.dp))

            if (showDrawer) {
                Column(Modifier.fillMaxWidth().weight(1f)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("All apps", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("Done", color = Color.White.copy(.85f), fontSize = 13.sp,
                            modifier = Modifier.clickable { onDrawerChange(false) }.padding(8.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    GlassSearchField(query, onQuery, "Search apps")
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app -> AppTile(app, compact, onLaunch) }
                    }
                }
            } else {
                val pageApps = if (activePage == 0) apps.filter { app -> folders.none { (_, group) -> group.any { it.packageName == app.packageName } } }.take(10)
                    else apps.filter { app -> folders.none { (_, group) -> group.any { it.packageName == app.packageName } } }.drop(10).take(10)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    userScrollEnabled = false,
                    contentPadding = PaddingValues(top = 2.dp, bottom = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    items(pageApps, key = { it.packageName }) { app -> AppTile(app, compact, onLaunch) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(2) { index ->
                        Box(Modifier.padding(horizontal = 4.dp).size(if (activePage == index) 6.dp else 5.dp)
                            .clip(CircleShape).background(Color.White.copy(if (activePage == index) .95f else .38f))
                            .clickable { activePage = index })
                    }
                }
            }
        }

        if (selectedFolder != null) {
            FolderDialog(selectedFolder!!.first, selectedFolder!!.second, onLaunch) { selectedFolder = null }
        }
        if (showCustomize) {
            CustomizePanel(
                selectedPreset = preset, columns = columns, compact = compact,
                onClose = { onCustomize(false) }, onPreset = onPreset,
                onPickPhoto = onPickPhoto, onColumns = onColumns, onCompact = onCompact
            )
        }
    }
}

@Composable
private fun CalendarWidget(modifier: Modifier, date: Date) {
    val calendar = java.util.Calendar.getInstance().apply { time = date }
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp))
        .background(Color.Black.copy(.24f)).padding(horizontal = 13.dp, vertical = 12.dp)) {
        Text(SimpleDateFormat("MMMM", Locale.getDefault()).format(date).uppercase(Locale.getDefault()),
            color = Color.White.copy(.7f), fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S","M","T","W","T","F","S").forEach {
                Text(it, color = Color.White.copy(.48f), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        val firstDay = java.util.Calendar.getInstance().apply { time = date; set(java.util.Calendar.DAY_OF_MONTH, 1) }
        val offset = firstDay.get(java.util.Calendar.DAY_OF_WEEK) - 1
        val maxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        for (week in 0..4) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (weekday in 0..6) {
                    val number = week * 7 + weekday - offset + 1
                    Box(Modifier.weight(1f).height(17.dp), contentAlignment = Alignment.Center) {
                        if (number in 1..maxDay) {
                            Box(Modifier.size(if (number == day) 17.dp else 15.dp)
                                .clip(CircleShape)
                                .background(if (number == day) Color.White else Color.Transparent),
                                contentAlignment = Alignment.Center) {
                                Text(number.toString(), color = if (number == day) Color(0xFF253244) else Color.White.copy(.86f),
                                    fontSize = 8.sp, fontWeight = if (number == day) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherWidget(modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(Color.White.copy(.18f), Color.White.copy(.07f))))
        .clickable(onClick = onClick).padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text("WEATHER", color = Color.White.copy(.62f), fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
        Text("☀", color = Color.White, fontSize = 31.sp, lineHeight = 34.sp)
        Column {
            Text("Weather", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text("Tap to personalize", color = Color.White.copy(.65f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun FolderTile(name: String, apps: List<LaunchApp>, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(.12f))
        .clickable(onClick = onClick).padding(vertical = 9.dp, horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(35.dp), contentAlignment = Alignment.Center) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                apps.take(3).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        row.forEach { app ->
                            Image(app.icon, app.label, Modifier.size(15.dp).clip(RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(name, color = Color.White.copy(.92f), fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun FolderDialog(name: String, apps: List<LaunchApp>, onLaunch: (LaunchApp) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(27.dp))
            .background(Color(0xF226303C)).padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("Done", color = Color(0xFFBBD8FF), modifier = Modifier.clickable(onClick = onDismiss).padding(6.dp))
            }
            Spacer(Modifier.height(15.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(apps, key = { it.packageName }) { app -> AppTile(app, false, onLaunch) }
            }
        }
    }
}

@Composable
private fun GlassCircleButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(Modifier.size(39.dp).clip(CircleShape).background(Color.White.copy(.16f))
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(symbol, color = Color.White, fontSize = 20.sp)
    }
}

@Composable
private fun GlassInfoCard(modifier: Modifier, symbol: String, title: String, subtitle: String, onClick: () -> Unit) {
    Column(modifier.height(83.dp).clip(RoundedCornerShape(21.dp))
        .background(Color.White.copy(.13f)).clickable(onClick = onClick).padding(13.dp),
        verticalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.width(7.dp))
            Text(title, color = Color.White.copy(.9f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(subtitle, color = Color.White.copy(.72f), fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun GlassSearchField(value: String, onValue: (String) -> Unit, hint: String) {
    Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(18.dp))
        .background(Color.White.copy(.16f)).padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("⌕", color = Color.White.copy(.8f), fontSize = 23.sp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = Color.White.copy(.62f), fontSize = 13.sp)
            BasicTextField(value, onValue, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth())
        }
        if (value.isNotEmpty()) Text("×", color = Color.White, modifier = Modifier.clickable { onValue("") })
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
    var selectedMenu by remember { mutableStateOf("Home screen") }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.48f)), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.82f)
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Color(0xF21B2430)).padding(horizontal = 21.dp, vertical = 17.dp)) {
            Box(Modifier.width(38.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(.28f)).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Home settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("Make your home screen yours", color = Color.White.copy(.58f), fontSize = 12.sp)
                }
                Text("Done", color = Color(0xFFBBD8FF), modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Home screen", "Wallpaper", "Apps", "About").forEach { item ->
                    val active = selectedMenu == item
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                        .background(if (active) Color.White.copy(.19f) else Color.White.copy(.07f))
                        .clickable { selectedMenu = item }.padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center) {
                        Text(item, color = Color.White.copy(if (active) 1f else .65f), fontSize = 10.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(19.dp))
            when (selectedMenu) {
                "Wallpaper" -> {
                    Text("WALLPAPER COLLECTION", color = Color.White.copy(.55f), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(13.dp))
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(wallpaperPresets.chunked(2)) { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                pair.forEach { item ->
                                    val index = wallpaperPresets.indexOf(item)
                                    Column(Modifier.weight(1f).clickable { onPreset(index) }) {
                                        Box(Modifier.fillMaxWidth().height(105.dp).clip(RoundedCornerShape(17.dp))
                                            .background(Brush.verticalGradient(item.colors)), contentAlignment = Alignment.Center) {
                                            if (selectedPreset == index) Text("✓", color = Color.White, fontSize = 25.sp)
                                        }
                                        Text(item.name, color = Color.White.copy(.84f), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("Choose from Gallery") }
                }
                "Home screen" -> {
                    Text("HOME SCREEN GRID", color = Color.White.copy(.55f), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(4, 5).forEach { count ->
                            FilterChip(selected = columns == count, onClick = { onColumns(count) }, label = { Text("$count columns") })
                        }
                    }
                    SettingsRow("Home screen layout", "4 × 5 app grid", "▦")
                    SettingsRow("Swipe up", "Open the app drawer", "↑")
                    SettingsRow("Page indicator", "Show home pages", "••")
                    SettingsRow("Dock background", "Liquid Glass", "◉")
                }
                "Apps" -> {
                    Text("APP DRAWER", color = Color.White.copy(.55f), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    SettingsRow("App drawer columns", "$columns columns", "▦")
                    SettingsRow("Icon labels", "App names below icons", "Aa")
                    SettingsRow("Icon size", if (compact) "Compact" else "Standard", "◫")
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !compact, onClick = { onCompact(false) }, label = { Text("Standard icons") })
                        FilterChip(selected = compact, onClick = { onCompact(true) }, label = { Text("Compact icons") })
                    }
                }
                else -> {
                    Text("GLASS HOME", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("A native Android home experience with refractive Liquid Glass rendering.",
                        color = Color.White.copy(.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    SettingsRow("Rendering", "QmDeve AGSL LiquidGlassView", "✧")
                    SettingsRow("Device", "Optimized for Galaxy S22+", "▣")
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, summary: String, symbol: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(.1f)), contentAlignment = Alignment.Center) {
            Text(symbol, color = Color.White, fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(summary, color = Color.White.copy(.55f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun DockContent(apps: List<LaunchApp>, onLaunch: (LaunchApp) -> Unit, onDrawer: () -> Unit) {
    Row(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        apps.take(4).forEach { app ->
            Image(app.icon, app.label, Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).clickable { onLaunch(app) })
        }
        Box(Modifier.size(53.dp).clip(CircleShape).background(Color.White.copy(.17f)).clickable(onClick = onDrawer),
            contentAlignment = Alignment.Center) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { Box(Modifier.size(4.dp).background(Color.White, CircleShape)) }
                } }
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
