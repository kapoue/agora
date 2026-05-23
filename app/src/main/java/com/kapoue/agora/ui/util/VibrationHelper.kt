package com.kapoue.agora.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationHelper {

    /** 150ms — scan réussi */
    fun vibrateOnScan(context: Context) {
        vibrate(context, VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /** pattern 0, 150, 100, 150 — QR résultat affiché */
    fun vibrateOnResult(context: Context) {
        vibrate(context, VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1))
    }

    private fun vibrate(context: Context, effect: VibrationEffect) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(effect)
    }
}
