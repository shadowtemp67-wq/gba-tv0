package com.example.gbatv

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.math.abs

data class Game(val title: String, val file: File)

fun romsDir(c: Context) = (c.getExternalFilesDir("roms") ?: File(c.filesDir, "roms")).apply { mkdirs() }

fun scan(c: Context): List<Game> =
    listOf(romsDir(c), File("/sdcard/Roms/GBA"))
        .flatMap { it.listFiles { f -> f.extension.equals("gba", true) }?.toList() ?: emptyList() }
        .map { Game(it.nameWithoutExtension.replace(Regex("\\s*[(\\[].*?[)\\]]"), "").trim(), it) }
        .sortedBy { it.title.lowercase() }

fun tint(t: String) = Color.hsv((abs(t.hashCode()) % 360).toFloat(), .6f, .6f)

fun importRom(c: Context, uri: Uri) {
    val name = c.contentResolver.query(uri, null, null, null, null)?.use {
        if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
    } ?: "game.gba"
    c.contentResolver.openInputStream(uri)?.use { i -> File(romsDir(c), name).outputStream().use { i.copyTo(it) } }
}

class MainActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT < 33) requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        setContent { Library(tick) { g ->
            val p = getSharedPreferences("p", 0)
            val r = (listOf(g.file.absolutePath) + (p.getString("recent", "")!!.split("|"))).filter { it.isNotBlank() }.distinct().take(10)
            p.edit().putString("recent", r.joinToString("|")).apply()
            startActivity(Intent(this, GameActivity::class.java).putExtra("rom", g.file.absolutePath))
        } }
    }
    override fun onResume() { super.onResume(); tick++ }
}

@Composable
fun Library(tick: Int, onPlay: (Game) -> Unit) {
    val ctx = LocalContext.current
    var local by remember { mutableIntStateOf(0) }
    val games = remember(tick, local) { scan(ctx) }
    val recent = remember(tick, local) {
        ctx.getSharedPreferences("p", 0).getString("recent", "")!!.split("|").mapNotNull { p -> games.find { it.file.absolutePath == p } }
    }
    val picker = rememberLauncherForActivityResult(OpenDocument()) { u -> u?.let { importRom(ctx, it); local++ } }
    var focus by remember { mutableStateOf<Game?>(null) }
    val glow by animateColorAsState((focus?.let { tint(it.title) } ?: Color(0xFF333344)).copy(alpha = .45f), label = "glow")
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(glow, Color.Black), Offset(1400f, 100f), 1500f))) {
        Column(Modifier.padding(horizontal = 48.dp, vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.height(130.dp), verticalArrangement = Arrangement.Bottom) {
                Text(focus?.title ?: "Game Boy Advance", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Text(if (games.isEmpty()) "Add a .gba file to get started" else "${games.size} game(s)  •  OK to play",
                    color = Color(0xFFBBBBCC), fontSize = 16.sp)
            }
            if (recent.isNotEmpty()) {
                Text("Continue", color = Color.White, fontSize = 20.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(8.dp)) {
                    items(recent) { g -> Tile(g.title, tint(g.title), { focus = g }) { onPlay(g) } }
                }
            }
            Text("Games", color = Color.White, fontSize = 20.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(8.dp)) {
                items(games) { g -> Tile(g.title, tint(g.title), { focus = g }) { onPlay(g) } }
                item { Tile("+ Add ROM", Color(0xFF2A2A38), { focus = null }) { picker.launch(arrayOf("*/*")) } }
            }
            Text("Or copy ROMs to ${romsDir(ctx).path}", color = Color(0xFF777788), fontSize = 13.sp)
        }
    }
}

@Composable
fun Tile(title: String, color: Color, onFocus: () -> Unit, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (f) 1.12f else 1f, label = "s")
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.size(190.dp, 150.dp).scale(s).clip(shape).background(color)
        .border(if (f) 3.dp else 0.dp, Color.White, shape)
        .onFocusChanged { f = it.isFocused; if (f) onFocus() }
        .clickable(onClick = onClick).padding(12.dp), contentAlignment = Alignment.BottomStart) {
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 3)
    }
}
