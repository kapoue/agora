package com.kapoue.agora.data.repository

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedImagePool @Inject constructor() {

    private var urls: List<String> = emptyList()

    fun setUrls(list: List<String>) {
        urls = list
    }

    fun getUrl(index: Int): String? = urls.getOrNull(index)

    fun clear() {
        urls = emptyList()
    }

    val size: Int get() = urls.size
}
