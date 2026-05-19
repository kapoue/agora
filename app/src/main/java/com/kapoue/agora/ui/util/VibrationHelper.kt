package com.kapoue.agora.ui.util

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

object VibrationHelper {

    /** 150ms — scan réussi */
    fun vibrateOnScan(context: Context) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /** pattern 0, 150, 100, 150 — QR résultat affiché */
    fun vibrateOnResult(context: Context) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1))
    }
}
