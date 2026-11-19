package com.example

import com.example.data.MovieBoxApiClient
import com.example.data.MovieBoxParsers
import com.example.data.SecondaryCatalogApiClient
import com.example.model.ContentSource
import com.example.model.MainTab
import com.example.model.MediaType
import com.example.ui.viewmodel.MuvioViewModel
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MuvioApiAndViewModelTest {

    @Test
    fun parseHomeJson_extractsBannerOperatingListAndSections() {
        val client = MovieBoxApiClient()
        val json = JSONObject(
            """
            {
              "data": {
                "operatingList": [
                  {
                    "type": "BANNER",
                    "title": "Featured",
                    "banner": {
                      "items": [
                        {
                          "subjectId": "sq_1",
                          "title": "Squid Game",
                          "releaseDate": "2025-12-26",
                          "genre": ["Thriller", "Drama"],
                          "imdbRatingValue": "8.4",
                          "countryName": "South Korea",
                          "subjectType": 2,
                          "cover": { "url": "https://cdn.example.com/squid.jpg" }
                        }
                      ]
                    }
                  },
                  {
                    "title": "Trending Movies",
                    "subjects": [
                      {
                        "subjectId": "mv_1",
                        "title": "Godzilla Minus Zero [Hindi]",
                        "releaseDate": "2026-01-10",
                        "genre": "Action",
                        "rating": 7.9,
                        "countryName": "Japan",
                        "subjectType": 1,
                        "cover": "https://cdn.example.com/godzilla.jpg"
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val result = client.parseHomeJson(json)
        assertEquals(1, result.heroItems.size)
        assertEquals("Squid Game", result.heroItems.first().title)
        assertEquals(MediaType.SERIES, result.heroItems.first().mediaType)
        assertEquals("https://cdn.example.com/squid.jpg", result.heroItems.first().poster.remoteUrl)

        assertTrue(result.sections.containsKey("Trending Movies"))
        val movies = result.sections["Trending Movies"].orEmpty()
        assertEquals(1, movies.size)
        assertEquals("Hindi", movies.first().languageBadge)
        assertEquals(2026, movies.first().year)
    }

    @Test
    fun parseDetailsAndStreamsJson_extractsEpisodesDubsCastSourcesAndSubtitles() {
        val client = MovieBoxApiClient()
        val detailsJson = JSONObject(
            """
            {
              "data": {
                "subject": {
                  "subjectId": "sq_1",
                  "title": "Squid Game",
                  "description": "Hundreds of cash-strapped players accept a strange invitation.",
                  "subjectType": 2,
                  "imdbRatingValue": "8.5"
                },
                "seasons": [
                  { "se": 1, "maxEp": 9 },
                  { "se": 2, "maxEp": 7 }
                ],
                "dubs": [
                  { "title": "Squid Game [Hindi]", "language": "Hindi dub", "subjectId": "sq_hi" },
                  { "title": "Squid Game", "language": "Original", "subjectId": "sq_1" }
                ],
                "stars": [
                  { "staffId": "101", "name": "Lee Jung-jae", "character": "Seong Gi-hun" }
                ]
              }
            }
            """.trimIndent()
        )

        val detailResult = client.parseDetailsJson(detailsJson, "Squid Game")
        assertNotNull(detailResult.updatedItem)
        assertEquals(2, detailResult.seasonsCount)
        assertEquals(9, detailResult.updatedItem?.episodes?.size)
        assertEquals(2, detailResult.updatedItem?.dubs?.size)
        assertEquals("Hindi dub", detailResult.updatedItem?.dubs?.first()?.language)
        assertEquals(1, detailResult.updatedItem?.cast?.size)
        assertEquals("Lee Jung-jae", detailResult.updatedItem?.cast?.first()?.name)

        val streamJson = JSONObject(
            """
            {
              "data": {
                "processedSources": [
                  { "url": "https://cdn.example.com/sq_480.mp4", "quality": "480", "language": "English" },
                  { "url": "https://cdn.example.com/sq_480p.mp4", "quality": "480p", "language": "English" },
                  { "url": "https://cdn.example.com/sq_1080_raw.mp4", "quality": "1080", "language": "English" },
                  { "url": "https://cdn.example.com/sq_1080.mp4", "quality": "1080p", "language": "English" },
                  { "url": "https://cdn.example.com/sq_720.mp4", "quality": "720p", "language": "Hindi dub" }
                ],
                "captions": [
                  { "id": "sub_en", "lan": "en", "lanName": "English", "url": "https://cdn.example.com/sq_en.vtt" }
                ]
              }
            }
            """.trimIndent()
        )

        val streams = client.parseStreamsJson(streamJson, "Squid Game")
        // Deduplicated: 1080 + 1080p -> single 1080p, 480 + 480p -> single 480p; sorted descending (1080p, 720p, 480p)
        assertEquals(3, streams.size)
        assertEquals("1080p", streams[0].quality)
        assertEquals("720p", streams[1].quality)
        assertEquals("480p", streams[2].quality)
        assertEquals(1, streams[0].subtitles.size)
        assertEquals("English", streams[0].subtitles[0].label)
    }

    @Test
    fun parseSportsCategoriesShortsCommentsAndQuota_workAndFilterAdultContent() {
        val client = MovieBoxApiClient()

        // 1. /sports (.m3u8)
        val sportsJson = JSONObject(
            """
            {
              "data": {
                "matches": [
                  {
                    "id": "m1",
                    "league": "UEFA Champions League",
                    "homeName": "Real Madrid",
                    "awayName": "Arsenal",
                    "score": "2 - 1",
                    "status": "live",
                    "streamUrl": "https://live.example.com/ucl.m3u8"
                  }
                ]
              }
            }
            """.trimIndent()
        )
        val matches = client.parseSportsJson(sportsJson)
        assertEquals(1, matches.size)
        assertTrue(matches.first().isLive)
        assertEquals("https://live.example.com/ucl.m3u8", matches.first().streamUrl)

        // 2. /categories (sports/cricket/football/originals excluded; 18+ kept)
        val catsPayload = JSONObject(
            """
            {
              "categories": [
                { "id": "c1", "name": "Bollywood", "path": "/bollywood" },
                { "id": "c2", "name": "Adult 18+", "path": "/adult18plus" },
                { "id": "c3", "name": "Anime", "path": "/anime" },
                { "id": "c4", "name": "Cricket Live", "path": "/cricket" },
                { "id": "c5", "name": "Football", "path": "/football" },
                { "id": "c6", "name": "Sports Arena", "path": "/sports" }
              ]
            }
            """.trimIndent()
        )
        val cats = MovieBoxParsers.parseCategories(catsPayload)
        assertEquals(3, cats.size)
        assertTrue(cats.any { it.path == "/adult18plus" })
        assertTrue(cats.none { it.path.contains("sport", ignoreCase = true) || it.path.contains("cricket", ignoreCase = true) || it.path.contains("football", ignoreCase = true) })

        // 3. /shorts
        val shortsPayload = JSONObject(
            """
            {
              "items": [
                {
                  "subjectId": "sh1",
                  "title": "CEO Secret Marriage",
                  "description": "Episode 1 micro-drama",
                  "streamUrl": "https://cdn.example.com/short1.mp4"
                }
              ]
            }
            """.trimIndent()
        )
        val shorts = MovieBoxParsers.parseShortsPayload(shortsPayload)
        assertEquals(1, shorts.size)
        assertEquals("https://cdn.example.com/short1.mp4", shorts.first().streamUrl)

        // 4. /comments
        val commentsPayload = JSONObject(
            """
            {
              "comments": [
                { "id": "cm1", "author": "Arif", "content": "Hindi dub is awesome!", "likes": 42 }
              ]
            }
            """.trimIndent()
        )
        val comments = MovieBoxParsers.parseCommentsPayload(commentsPayload)
        assertEquals(1, comments.size)
        assertEquals("Arif", comments.first().author)
        assertEquals(42, comments.first().likes)

        // 5. /me quota
        val quotaPayload = JSONObject(
            """
            { "name": "Client 11-19", "plan": "UNLIMITED", "quota": "UNLIMITED", "expiresAt": "2036-11-16T02:53:10.831Z" }
            """.trimIndent()
        )
        val quota = MovieBoxParsers.parseQuotaInfo(quotaPayload)
        assertEquals("Client 11-19", quota.clientName)
        assertEquals("UNLIMITED", quota.plan)
        assertEquals("2036-11-16", quota.expiresAt)

        // 6. /suggest & /hot-search
        val suggestPayload = JSONObject(
            """
            { "suggestions": ["Squid Game", "Squid Game Season 2"] }
            """.trimIndent()
        )
        val suggestions = MovieBoxParsers.parseStringList(suggestPayload, listOf("suggestions"))
        assertEquals(2, suggestions.size)
        assertEquals("Squid Game", suggestions.first())
    }

    @Test
    fun viewModel_startsCleanWithoutDemoVideosAndSupportsAdultGateAndWatchlist() {
        val vm = MuvioViewModel()
        val initial = vm.uiState.value

        // Zero hardcoded demo videos, zero demo watchlist/history/downloads, no Football/Cricket/Originals tabs
        assertTrue(initial.allMedia.isEmpty())
        assertTrue(initial.watchlistIds.isEmpty())
        assertTrue(initial.watchHistoryIds.isEmpty())
        assertTrue(initial.downloads.isEmpty())
        assertTrue("18+" in initial.topTabs)
        assertFalse("Football" in initial.topTabs)
        assertFalse("Cricket" in initial.topTabs)
        assertFalse("Originals" in initial.topTabs)

        // 18+ Age Gate flow
        assertFalse(vm.requestAdultAccess())
        assertTrue(vm.uiState.value.showAdultGateDialog)
        vm.confirmAdultAge()
        assertTrue(vm.uiState.value.isAdultVerified)
        assertFalse(vm.uiState.value.showAdultGateDialog)
        assertEquals("18+", vm.uiState.value.selectedTopTab)

        // Watchlist, Comments, Sleep Timer & Watch Progress
        val sampleId = "live_item_1"
        vm.toggleWatchlist(sampleId)
        assertTrue(sampleId in vm.uiState.value.watchlistIds)

        vm.postComment(sampleId, "Great movie!")
        val afterComments = vm.uiState.value.commentsByMedia[sampleId].orEmpty()
        assertEquals(1, afterComments.size)
        assertEquals("Great movie!", afterComments.first().content)

        vm.selectMood("Action Rush")
        assertEquals("Action Rush", vm.uiState.value.selectedMood)

        vm.cycleSleepTimer()
        assertEquals(15, vm.uiState.value.sleepTimerMinutes)
        vm.cycleSleepTimer()
        assertEquals(30, vm.uiState.value.sleepTimerMinutes)

        vm.updateWatchProgress(sampleId, 0.72f)
        assertEquals(0.72f, vm.uiState.value.watchProgressByMedia[sampleId] ?: 0f, 0.01f)

        // Watchlist shortcut & quality-specific download
        vm.selectMeSection(2)
        assertEquals(2, vm.uiState.value.meSectionIndex)
        vm.openWatchlistShortcut()
        assertEquals(MainTab.ME, vm.uiState.value.selectedTab)
        assertEquals(0, vm.uiState.value.meSectionIndex)

        val liveMovie = MovieBoxParsers.parseSingleMedia(
            JSONObject("""{ "subjectId": "mv_live_99", "title": "Live Test Movie", "subjectType": 1 }"""),
            "trending_movies"
        )!!
        vm.startDownload(liveMovie, "720p")
        assertEquals("720p", vm.uiState.value.downloads.first { it.mediaId == liveMovie.id }.quality)
    }

    @Test
    fun parseMultiSeasonAnimeAndSwitchSeasons_populatesAllSeasonsAndEpisodes() {
        val client = MovieBoxApiClient()

        // 1. Base title cleanup & S1-S4 detection
        assertEquals("Naruto: Shippuden", client.extractBaseTitle("Naruto: Shippuden [English] S1-S4 (2007)"))
        assertEquals(4, MovieBoxParsers.extractTitleSeasonCount("Naruto: Shippuden [English] S1-S4 (2007)"))

        val narutoCard = MovieBoxParsers.parseSingleMedia(
            JSONObject(
                """
                {
                  "subjectId": "naruto_1",
                  "title": "Naruto: Shippuden [English] S1-S4 (2007)",
                  "genre": "Animation, Action, Adventure",
                  "releaseDate": "2007-02-15"
                }
                """.trimIndent()
            ),
            "most_trending"
        )!!
        assertEquals(MediaType.ANIME, narutoCard.mediaType)

        // 2. Multi-season resource.seasons with allEp, episodeCount, and String maxEp
        val multiSeasonJson = JSONObject(
            """
            {
              "data": {
                "subject": {
                  "subjectId": "naruto_1",
                  "title": "Naruto: Shippuden [English] S1-S4 (2007)",
                  "subjectType": 2
                },
                "resource": {
                  "seasons": [
                    { "se": 1, "allEp": "1-32" },
                    { "se": 2, "episodeCount": 21 },
                    { "se": 3, "maxEp": "18" },
                    { "se": 4, "episodes": 17 }
                  ]
                }
              }
            }
            """.trimIndent()
        )
        val parsedDetail = client.parseDetailsJson(multiSeasonJson, narutoCard.title)
        assertEquals(4, parsedDetail.seasonsCount)
        assertEquals(32, parsedDetail.seasonEpisodes[1]?.size)
        assertEquals(21, parsedDetail.seasonEpisodes[2]?.size)
        assertEquals(18, parsedDetail.seasonEpisodes[3]?.size)
        assertEquals(17, parsedDetail.seasonEpisodes[4]?.size)

        // 3. ViewModel immediately infers 4 seasons + 12 episodes fallback on openMediaDetail and switches seasons cleanly
        val vm = MuvioViewModel()
        vm.openMediaDetail(narutoCard)
        assertEquals(4, vm.uiState.value.seasonsCountByMedia[narutoCard.id])
        assertEquals(12, vm.uiState.value.seasonEpisodesByMedia[narutoCard.id]?.get(1)?.size)

        vm.selectSeason(narutoCard, 3)
        assertEquals(3, vm.uiState.value.selectedSeasonByMedia[narutoCard.id])
        assertEquals(1, vm.uiState.value.selectedEpisodeByMedia[narutoCard.id])

        vm.loadMoreEpisodesForCurrentSeason(narutoCard)
        assertEquals(36, vm.uiState.value.seasonEpisodesByMedia[narutoCard.id]?.get(3)?.size)

        // 4. Exact API season counts (Bleach: TYBW style) must NEVER be inflated by series-wide totalEpisodes or length/count
        val bleachJson = JSONObject(
            """
            {
              "data": {
                "subject": {
                  "subjectId": "bleach_tybw",
                  "title": "Bleach: Thousand-Year Blood War [Hindi]",
                  "subjectType": 2,
                  "totalEpisodes": 220
                },
                "resource": {
                  "seasons": [
                    { "se": 1, "maxEp": 13, "count": 240, "length": 1440, "allEp": "" },
                    { "se": 2, "maxEp": 13, "resolutions": [{ "resolution": 1080, "epNum": 13 }] },
                    { "se": 3, "maxEp": 14 },
                    { "se": 4, "maxEp": 0 }
                  ]
                }
              }
            }
            """.trimIndent()
        )
        val bleachDetail = client.parseDetailsJson(bleachJson, "Bleach: Thousand-Year Blood War [Hindi]")
        assertEquals(4, bleachDetail.seasonsCount)
        assertEquals(13, bleachDetail.seasonEpisodes[1]?.size)
        assertEquals(13, bleachDetail.seasonEpisodes[2]?.size)
        assertEquals(14, bleachDetail.seasonEpisodes[3]?.size)
        assertEquals(0, bleachDetail.seasonEpisodes[4]?.size)
    }

    @Test
    fun secondaryCatalogApi_parsesRelativeUrlsIsolatesEpisodesAndInterleaves5050() {
        val baseUrl = "https://movei-link.onrender.com/api/v1"
        val originUrl = "https://movei-link.onrender.com"
        val secondaryClient = SecondaryCatalogApiClient(
            baseUrl = baseUrl,
            apiKeyProvider = { "test_cinepulse_key" }
        )

        val homeJson = JSONObject(
            """
            {
              "banners": [
                {
                  "title": "Toofan (2024) Bangla",
                  "path": "/movie/toofan-2024",
                  "poster": "/img/toofan.jpg"
                }
              ],
              "trending": [
                {
                  "title": "Solo Leveling Season 1 Anime",
                  "path": "/series/solo-leveling-s1",
                  "poster": "https://cdn.example.com/solo.jpg"
                }
              ]
            }
            """.trimIndent()
        )
        val parsedItems = secondaryClient.parseHomeJson(homeJson)
        assertEquals(2, parsedItems.size)
        assertEquals(ContentSource.SECONDARY, parsedItems[0].contentSource)
        assertTrue(parsedItems[0].id.startsWith("s2_"))
        // Root-relative path resolves against origin (NOT /api/v1/img/toofan.jpg)
        assertEquals("$originUrl/img/toofan.jpg", parsedItems[0].poster.remoteUrl)
        assertEquals("Bengali", parsedItems[0].languageBadge)
        assertEquals(MediaType.ANIME, parsedItems[1].mediaType)

        val detailJson = JSONObject(
            """
            {
              "detail": {
                "title": "Solo Leveling Season 1",
                "storyline": "In a world of hunters, Sung Jinwoo rises from the weakest rank.",
                "cast": ["Taito Ban", "Genta Nakamura"],
                "episodes": [
                  {
                    "number": 1,
                    "title": "I'm Used to It",
                    "sources": [
                      { "type": "mp4", "url": "https://fsl.movielink.tv/p/ep1.mp4" },
                      { "type": "hls", "url": "/api/hls/master.m3u8?u=ep1" }
                    ]
                  },
                  {
                    "number": 2,
                    "title": "If I Had One More Chance",
                    "sources": [
                      { "type": "hls", "url": "/api/hls/master.m3u8?u=ep2" }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()
        )
        val detailResult = secondaryClient.parseDetailsJson(detailJson, parsedItems[1])
        assertEquals(1, detailResult.seasonsCount)
        assertEquals(2, detailResult.seasonEpisodes[1]?.size)
        assertEquals("In a world of hunters, Sung Jinwoo rises from the weakest rank.", detailResult.updatedItem.synopsis)
        assertEquals(2, detailResult.updatedItem.cast.size)
        assertEquals("https://fsl.movielink.tv/p/ep1.mp4", detailResult.initialStreams[0].streamUrl)
        assertEquals("Direct MP4", detailResult.initialStreams[0].quality)
        assertEquals("$originUrl/api/hls/master.m3u8?u=ep1", detailResult.initialStreams[1].streamUrl)
        assertEquals("Adaptive HLS", detailResult.initialStreams[1].quality)
        assertEquals("test_cinepulse_key", detailResult.initialStreams[1].headers["X-API-Key"])
        assertEquals(
            "$originUrl/api/hls/master.m3u8?u=ep2",
            detailResult.seasonEpisodes[1]?.get(1)?.sources?.first()?.streamUrl
        )

        // Verify /stream endpoint parser extracts Direct MP4 + Adaptive HLS
        val streamJson = JSONObject(
            """
            {
              "data": {
                "sources": [
                  { "type": "mp4", "url": "https://fsl.movielink.tv/p/bachelor_ep1.mp4", "quality": "1080p" },
                  { "type": "hls", "url": "/api/hls/master.m3u8?u=bp1", "quality": "720p" }
                ]
              }
            }
            """.trimIndent()
        )
        val streamRes = secondaryClient.parseStreamsJson(streamJson, "Bachelor Point", "Bengali")
        assertEquals(2, streamRes.size)
        assertEquals("https://fsl.movielink.tv/p/bachelor_ep1.mp4", streamRes[0].streamUrl)
        assertEquals("$originUrl/api/hls/master.m3u8?u=bp1", streamRes[1].streamUrl)

        val suggestRes = secondaryClient.parseSuggestJson(
            JSONObject("""{ "suggestions": [ { "title": "Bachelor Point", "path": "/series/bp", "poster": "/img/bp.jpg" } ] }""")
        )
        assertEquals(listOf("Bachelor Point"), suggestRes.suggestions)
        assertEquals(1, suggestRes.items.size)
        assertEquals(ContentSource.SECONDARY, suggestRes.items.first().contentSource)

        // Verify 50/50 interleaving between PRIMARY and SECONDARY sources
        val primary1 = MovieBoxParsers.parseSingleMedia(
            JSONObject("""{ "subjectId": "p1", "title": "Primary Movie 1", "subjectType": 1 }"""),
            "trending_movies"
        )!!
        val primary2 = MovieBoxParsers.parseSingleMedia(
            JSONObject("""{ "subjectId": "p2", "title": "Primary Movie 2", "subjectType": 1 }"""),
            "trending_movies"
        )!!
        val vm = MuvioViewModel()
        val blended = vm.interleaveSources5050(listOf(primary1, primary2, parsedItems[0], parsedItems[1]))
        assertEquals(4, blended.size)
        assertEquals(ContentSource.PRIMARY, blended[0].contentSource)
        assertEquals(ContentSource.SECONDARY, blended[1].contentSource)
        assertEquals(ContentSource.PRIMARY, blended[2].contentSource)
        assertEquals(ContentSource.SECONDARY, blended[3].contentSource)

        // Verify strict episode isolation: selecting Ep 2 on Secondary item loads ONLY Ep 2's own Source 2 stream
        vm.selectEpisode(detailResult.updatedItem, 2)
        val activeStreams = vm.uiState.value.streamsByMedia[detailResult.updatedItem.id].orEmpty()
        assertEquals(1, activeStreams.size)
        assertEquals("$originUrl/api/hls/master.m3u8?u=ep2", activeStreams.first().streamUrl)
    }

    @Test
    fun bengaliSubtitleService_detectsPrioritizesAndParsesSrtVttCleanly() {
        val en = com.example.model.SubtitleTrack("1", "en", "English", "https://cdn.example.com/en.srt")
        val bn = com.example.model.SubtitleTrack("2", "bn", "Bangla", "https://cdn.example.com/bn.vtt")
        val hi = com.example.model.SubtitleTrack("3", "hi", "Hindi", "https://cdn.example.com/hi.srt")

        assertTrue(com.example.data.BengaliSubtitleService.isBengaliTrack(bn))
        assertFalse(com.example.data.BengaliSubtitleService.isBengaliTrack(en))

        val prioritized = com.example.data.BengaliSubtitleService.prioritizeBengaliFirst(listOf(en, hi, bn))
        assertEquals("bn", prioritized.first().language)

        val srtSample = """
            1
            00:00:01,500 --> 00:00:04,250
            <i>But this man wasn't born</i>
            for just one household.

            2
            00:00:05,000 --> 00:00:07,800
            Welcome to Muvio.
        """.trimIndent()

        val cues = com.example.data.BengaliSubtitleService.parseCues(srtSample)
        assertEquals(2, cues.size)
        assertEquals("00:00:01.500", cues[0].startTime)
        assertEquals("00:00:04.250", cues[0].endTime)
        assertEquals("But this man wasn't born\nfor just one household.", cues[0].text)

        val vtt = com.example.data.BengaliSubtitleService.buildWebVtt(cues)
        assertTrue(vtt.startsWith("WEBVTT"))
        assertTrue(vtt.contains("00:00:01.500 --> 00:00:04.250"))
    }
}

