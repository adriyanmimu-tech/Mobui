package com.example.data

import com.example.R
import com.example.model.NovelItem
import com.example.model.PosterArtwork

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

    override fun getNovels(): List<NovelItem> = listOf(
        NovelItem(
            id = "nov_1",
            title = "Crimson Lanterns Over Padma",
            author = "A. Rahman",
            genre = "Mystery • Romance",
            chaptersCount = 48,
            rating = 4.9,
            isHot = true,
            synopsis = "An investigative illustrator discovers coded letters inside an antique bookstore in Dhaka.",
            sampleChapter = "Rain tapped steadily against the arched windows of Shakhari Bazar as Ariyan unfolded the faded ink map. Every red lantern marked on the riverfront aligned with a name from 1998...",
            poster = PosterArtwork(
                fallbackRes = R.drawable.img_poster_drama,
                tintHex = 0xFF1A1424,
                accentHex = 0xFF00E07A
            )
        ),
        NovelItem(
            id = "nov_2",
            title = "The Astral Sovereign",
            author = "K. Takahashi",
            genre = "Fantasy • Action",
            chaptersCount = 112,
            rating = 4.8,
            isHot = true,
            synopsis = "Exiled from the floating academy, a rune-smith awakens an ancient star core.",
            sampleChapter = "Above the cloud sea, the seventh sanctuary bells rang for the first time in three centuries. Sparks of emerald light spiraled around Kenji's gauntlets...",
            poster = PosterArtwork(
                fallbackRes = R.drawable.img_poster_anime,
                tintHex = 0xFF111D2C,
                accentHex = 0xFF38BDF8
            )
        ),
        NovelItem(
            id = "nov_3",
            title = "Midnight Contract",
            author = "Lin Mei",
            genre = "Drama • CEO Romance",
            chaptersCount = 64,
            rating = 4.7,
            isHot = false,
            synopsis = "Now adapted into a hit short drama! Read the complete original novel chapters.",
            sampleChapter = "The elevator doors slid open on the 42nd floor. Calmly placing the blue folder on the marble desk, Mei looked straight across the skyline...",
            poster = PosterArtwork(
                fallbackRes = R.drawable.img_hero_cinema,
                tintHex = 0xFF23131A,
                accentHex = 0xFFF59E0B
            )
        )
    )
}
