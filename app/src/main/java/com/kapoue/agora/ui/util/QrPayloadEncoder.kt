package com.kapoue.agora.ui.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kapoue.agora.domain.model.PlayerResult
import com.kapoue.agora.domain.model.QrPayload
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object QrPayloadEncoder {

    private val gson = Gson()

    fun encodePayload(payload: QrPayload): String {
        val json = gson.toJson(payload)
        return gzipAndBase64(json)
    }

    fun decodePayload(encoded: String): QrPayload {
        val json = base64AndGunzip(encoded)
        return gson.fromJson(json, QrPayload::class.java)
    }

    fun encodeResult(result: PlayerResult): String {
        val json = gson.toJson(result)
        return gzipAndBase64(json)
    }

    fun decodeResult(encoded: String): PlayerResult {
        val json = base64AndGunzip(encoded)
        return gson.fromJson(json, PlayerResult::class.java)
    }

    fun encodeResultList(results: List<PlayerResult>): String {
        val json = gson.toJson(results)
        return json
    }

    fun decodeResultList(json: String): List<PlayerResult> {
        val type = object : TypeToken<List<PlayerResult>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun encodeStringList(list: List<String>): String = gson.toJson(list)

    fun decodeStringList(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun gzipAndBase64(input: String): String {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(input.toByteArray(Charsets.UTF_8)) }
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    private fun base64AndGunzip(input: String): String {
        val bytes = Base64.getDecoder().decode(input)
        return GZIPInputStream(ByteArrayInputStream(bytes)).use {
            it.readBytes().toString(Charsets.UTF_8)
        }
    }
}
