package com.example.glasshome

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private data class LaunchApp(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap
)

private data class WallpaperAssets(
    val bitmap: Bitmap,
    val image: ImageBitmap
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
        setContent { MaterialTheme { GlassHomeScreen() } }
    }
}

@Composable
private fun GlassHomeScreen() {
    val context = LocalContext.current
    val density = LocalDensity.current
    var apps by remember { mutableStateOf<List<LaunchApp>>(emptyList()) }
    var wallpaper by remember { mutableStateOf<WallpaperAssets?>(null) }
    var showDrawer by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            apps = loadLauncherApps(context)
            wallpaper = loadWallpaper(context)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = Date()
        }
    }

    val dockApps = remember(apps) { chooseDockApps(apps) }
    val filteredApps = remember(apps, search) {
        apps.filter { it.label.contains(search.trim(), ignoreCase = true) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF35475D), Color(0xFF172436), Color(0xFF090F1A))
                )
            )
    ) {
        val rootWidthPx = with(density) { maxWidth.toPx() }
        val rootHeightPx = with(density) { maxHeight.toPx() }

        wallpaper?.let { assets ->
            Image(
                bitmap = assets.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color(0x22071320), Color(0x44071320), Color(0x99050A12))
                    )
                )
            )
        } ?: Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Color(0xFF49647C), Color(0xFF192A3D), Color(0xFF080D16)),
                    center = Offset(rootWidthPx * .76f, rootHeightPx * .19f),
                    radius = rootWidthPx * 1.1f
                )
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(17.dp))
            Text(
                text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now),
                color = Color.White.copy(alpha = .9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = SimpleDateFormat("h:mm", Locale.getDefault()).format(now),
                color = Color.White,
                fontSize = 62.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2.4).sp
            )

            AnimatedVisibility(
                visible = showDrawer,
                modifier = Modifier.fillMaxWidth().weight(1f),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 9 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 9 })
            ) {
                Column(Modifier.fillMaxSize()) {
                    Text(
                        "Applications",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 23.dp, end = 23.dp, top = 6.dp, bottom = 12.dp)
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = .16f))
                            .padding(horizontal = 15.dp, vertical = 12.dp)
                    ) {
                        if (search.isEmpty()) {
                            Text("Search apps", color = Color.White.copy(alpha = .55f), fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = search,
                            onValueChange = { search = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (filteredApps.isEmpty()) {
                        Text(
                            "No matching apps",
                            color = Color.White.copy(alpha = .7f),
                            modifier = Modifier.fillMaxWidth().padding(top = 35.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 22.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                AppTile(app) { launch(context, app.packageName) }
                            }
                        }
                    }
                }
            }

            if (!showDrawer) {
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dockApps.take(4).forEach { app ->
                        Box(Modifier.weight(1f)) {
                            AppTile(app) { launch(context, app.packageName) }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            GlassDock(
                apps = dockApps,
                wallpaper = wallpaper?.bitmap,
                rootWidthPx = rootWidthPx,
                rootHeightPx = rootHeightPx,
                onAppClick = { launch(context, it.packageName) },
                onDrawerClick = {
                    showDrawer = !showDrawer
                    search = ""
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (showDrawer) "Tap the grid to return home" else "Tap the grid to view all apps",
                color = Color.White.copy(alpha = .58f),
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun AppTile(app: LaunchApp, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(51.dp).clip(RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = app.label,
            color = Color.White.copy(alpha = .94f),
            fontSize = 10.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GlassDock(
    apps: List<LaunchApp>,
    wallpaper: Bitmap?,
    rootWidthPx: Float,
    rootHeightPx: Float,
    onAppClick: (LaunchApp) -> Unit,
    onDrawerClick: () -> Unit
) {
    val density = LocalDensity.current
    val radius = with(density) { 30.dp.toPx() }
    val edgeWidth = with(density) { 17.dp.toPx() }
    val refraction = with(density) { 17.dp.toPx() }
    var dockOrigin by remember { mutableStateOf(Offset.Zero) }

    val runtimeShader = remember(wallpaper) {
        if (Build.VERSION.SDK_INT >= 33 && wallpaper != null) {
            runCatching {
                RuntimeShader(LIQUID_GLASS_AGSL).apply {
                    setInputShader(
                        "wallpaper",
                        BitmapShader(wallpaper, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    )
                }
            }.getOrNull()
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(91.dp)
            .onGloballyPositioned { dockOrigin = it.positionInRoot() }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val rect = RectF(0f, 0f, size.width, size.height)
            val native = drawContext.canvas.nativeCanvas
            val shader = runtimeShader
            if (shader != null && wallpaper != null && rootWidthPx > 0f && rootHeightPx > 0f) {
                val coverScale = max(
                    rootWidthPx / wallpaper.width.toFloat(),
                    rootHeightPx / wallpaper.height.toFloat()
                )
                val offsetX = (rootWidthPx - wallpaper.width * coverScale) / 2f
                val offsetY = (rootHeightPx - wallpaper.height * coverScale) / 2f
                shader.setFloatUniform("dockOrigin", dockOrigin.x, dockOrigin.y)
                shader.setFloatUniform("dockSize", size.width, size.height)
                shader.setFloatUniform("wallpaperOffset", offsetX, offsetY)
                shader.setFloatUniform("wallpaperScale", coverScale)
                shader.setFloatUniform("cornerRadius", radius)
                shader.setFloatUniform("edgeWidth", edgeWidth)
                shader.setFloatUniform("refraction", refraction)
                shader.setFloatUniform("chroma", 5.5f)
                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
                native.drawRoundRect(rect, radius, radius, paint)
            } else {
                val fallback = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(104, 212, 225, 242)
                }
                native.drawRoundRect(rect, radius, radius, fallback)
            }

            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = .17f), Color.White.copy(alpha = .035f), Color.Black.copy(alpha = .12f))
                ),
                cornerRadius = CornerRadius(radius)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = .46f),
                cornerRadius = CornerRadius(radius),
                style = Stroke(width = 1.dp.toPx())
            )
            drawLine(
                color = Color.White.copy(alpha = .38f),
                start = Offset(size.width * .19f, 1.dp.toPx()),
                end = Offset(size.width * .81f, 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.take(4).forEach { app ->
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(51.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .clickable { onAppClick(app) }
                )
            }
            Box(
                modifier = Modifier
                    .size(49.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = .16f))
                    .clickable(onClick = onDrawerClick),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(21.dp)) {
                    val dot = 2.35.dp.toPx()
                    for (row in 0..2) for (col in 0..2) {
                        drawCircle(
                            color = Color.White,
                            radius = dot,
                            center = Offset(size.width * (.16f + col * .34f), size.height * (.16f + row * .34f))
                        )
                    }
                }
            }
        }
    }
}

private const val LIQUID_GLASS_AGSL = """
uniform shader wallpaper;
uniform float2 dockOrigin;
uniform float2 dockSize;
uniform float2 wallpaperOffset;
uniform float wallpaperScale;
uniform float cornerRadius;
uniform float edgeWidth;
uniform float refraction;
uniform float chroma;

half4 sampleWallpaper(float2 screenPosition) {
    float2 imagePosition = (screenPosition - wallpaperOffset) / wallpaperScale;
    return wallpaper.eval(imagePosition);
}

half4 main(float2 p) {
    float2 center = dockSize * 0.5;
    float2 q = abs(p - center) - (center - float2(cornerRadius));
    float signedDistance = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - cornerRadius;
    float distanceInside = max(-signedDistance, 0.0);
    float rim = 1.0 - smoothstep(0.0, edgeWidth, distanceInside);

    // Neutral-centre / refractive-rim displacement follows the upstream
    // red/blue gradient-map model without requiring a raster displacement map.
    float2 displacementMap = (p / dockSize - float2(0.5)) * 2.0;
    float2 displacement = displacementMap * refraction * rim;
    float2 screenPosition = dockOrigin + p;

    half4 redSample = sampleWallpaper(screenPosition - displacement * (1.0 + chroma * 0.035));
    half4 greenSample = sampleWallpaper(screenPosition - displacement);
    half4 blueSample = sampleWallpaper(screenPosition - displacement * (1.0 - chroma * 0.035));

    half3 prism = half3(redSample.r, greenSample.g, blueSample.b);
    half3 tint = half3(0.78, 0.86, 0.98);
    half3 glassColor = mix(prism, tint, 0.17);
    float topGlint = (1.0 - smoothstep(0.0, 3.0, p.y)) * 0.20;
    glassColor += half3(topGlint);
    return half4(glassColor, 0.94);
}
"""

private fun loadLauncherApps(context: Context): List<LaunchApp> {
    val pm = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        .mapNotNull { resolveInfo ->
            runCatching {
                val activity = resolveInfo.activityInfo
                LaunchApp(
                    label = resolveInfo.loadLabel(pm).toString(),
                    packageName = activity.packageName,
                    icon = drawableToBitmap(resolveInfo.loadIcon(pm)).asImageBitmap()
                )
            }.getOrNull()
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase(Locale.getDefault()) }
}

private fun chooseDockApps(apps: List<LaunchApp>): List<LaunchApp> {
    val patterns = listOf(
        listOf("phone", "dialer"),
        listOf("messages", "messaging", "sms"),
        listOf("chrome", "browser", "internet"),
        listOf("camera")
    )
    val selected = patterns.mapNotNull { words ->
        apps.firstOrNull { app ->
            words.any { word -> app.label.contains(word, true) || app.packageName.contains(word, true) }
        }
    }.distinctBy { it.packageName }.toMutableList()
    apps.forEach { app ->
        if (selected.size < 4 && selected.none { it.packageName == app.packageName }) selected += app
    }
    return selected.take(4)
}

private fun loadWallpaper(context: Context): WallpaperAssets? = runCatching {
    val drawable = WallpaperManager.getInstance(context).drawable ?: return null
    val source = if (drawable is BitmapDrawable && drawable.bitmap != null) {
        drawable.bitmap
    } else {
        drawableToBitmap(drawable)
    }
    val maxDimension = 1800
    val scale = minOf(1f, maxDimension / max(source.width, source.height).toFloat())
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        )
    } else {
        source.copy(Bitmap.Config.ARGB_8888, false)
    }
    WallpaperAssets(bitmap, bitmap.asImageBitmap())
}.getOrNull()

private fun launch(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(256)
    val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(256)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    return bitmap
}
