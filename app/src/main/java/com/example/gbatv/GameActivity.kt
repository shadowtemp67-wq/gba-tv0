package com.example.gbatv

import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.*
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.ShaderConfig
import java.io.File

class GameActivity : ComponentActivity() {
    private lateinit var retro: GLRetroView
    private lateinit var rom: File
    private var menuOpen = false
    private val stateFile get() = File(filesDir, "states").apply { mkdirs() }.let { File(it, rom.name + ".state") }

    // Positional (Nintendo) layout: Xbox B/Y -> GBA A, Xbox A/X -> GBA B.
    // If A/B feel swapped in-game, swap KEYCODE_BUTTON_A <-> KEYCODE_BUTTON_B values below.
    private val keymap = mapOf(
        KEYCODE_BUTTON_B to KEYCODE_BUTTON_A, KEYCODE_BUTTON_Y to KEYCODE_BUTTON_A, KEYCODE_DPAD_CENTER to KEYCODE_BUTTON_A,
        KEYCODE_BUTTON_A to KEYCODE_BUTTON_B, KEYCODE_BUTTON_X to KEYCODE_BUTTON_B,
        KEYCODE_BUTTON_L1 to KEYCODE_BUTTON_L1, KEYCODE_BUTTON_R1 to KEYCODE_BUTTON_R1,
        KEYCODE_BUTTON_START to KEYCODE_BUTTON_START, KEYCODE_MENU to KEYCODE_BUTTON_START,
        KEYCODE_BUTTON_SELECT to KEYCODE_BUTTON_SELECT,
        KEYCODE_DPAD_UP to KEYCODE_DPAD_UP, KEYCODE_DPAD_DOWN to KEYCODE_DPAD_DOWN,
        KEYCODE_DPAD_LEFT to KEYCODE_DPAD_LEFT, KEYCODE_DPAD_RIGHT to KEYCODE_DPAD_RIGHT,
    )

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        rom = File(intent.getStringExtra("rom")!!)
        val data = GLRetroViewData(this).apply {
            coreFilePath = "libmgba_libretro_android.so"
            gameFilePath = rom.absolutePath
            systemDirectory = filesDir.absolutePath
            savesDirectory = File(filesDir, "saves").apply { mkdirs() }.absolutePath
            shader = ShaderConfig.Sharp
            if (stateFile.exists()) stateBytes = stateFile.readBytes()   // resume where you left off
        }
        retro = GLRetroView(this, data)
        lifecycle.addObserver(retro)
        setContentView(retro)
    }

    private fun save() = try { stateFile.writeBytes(retro.serializeState()) } catch (_: Exception) {}
    override fun onPause() { save(); super.onPause() }

    private fun openMenu() {
        menuOpen = true; retro.audioEnabled = false
        AlertDialog.Builder(this).setTitle(rom.nameWithoutExtension)
            .setItems(arrayOf("Resume", "Save state", "Load state", "Reset", "Quit to library")) { _, i ->
                when (i) {
                    1 -> save()
                    2 -> if (stateFile.exists()) retro.unserializeState(stateFile.readBytes())
                    3 -> retro.reset()
                    4 -> finish()
                }
            }.setOnDismissListener { menuOpen = false; retro.audioEnabled = true }.show()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode == KEYCODE_BACK || e.keyCode == KEYCODE_BUTTON_MODE) {
            if (e.action == ACTION_UP && !menuOpen) openMenu(); return true
        }
        val k = keymap[e.keyCode] ?: return super.dispatchKeyEvent(e)
        retro.sendKeyEvent(e.action, k, 0)
        return true
    }

    override fun dispatchGenericMotionEvent(e: MotionEvent): Boolean {
        if (e.source and android.view.InputDevice.SOURCE_JOYSTICK != 0 && !menuOpen) {
            retro.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, e.getAxisValue(MotionEvent.AXIS_X), e.getAxisValue(MotionEvent.AXIS_Y), 0)
            return true
        }
        return super.dispatchGenericMotionEvent(e)
    }
}
