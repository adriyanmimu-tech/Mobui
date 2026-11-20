package com.example.data

import com.example.model.NovelItem

interface MuvioCatalogRepository {
    fun getTopTabs(): List<String>
    fun getNovels(): List<NovelItem>
}

class MuvioSeedRepository : MuvioCatalogRepository {

    override fun getTopTabs(): List<String> = listOf(
        "Trending",
        "Movies",
        "Series",
        "Anime",
        "Bengali",
        "18+"
    )

    override fun getNovels(): List<NovelItem> = emptyList()
}
