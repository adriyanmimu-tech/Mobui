package com.example.data

import com.example.R
import com.example.model.ApiCategory
import com.example.model.ApiQuotaInfo
import com.example.model.CommentItem
import com.example.model.DubVersion
import com.example.model.EpisodeItem
import com.example.model.FilterOptions
import com.example.model.LiveMatchItem
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.PosterArtwork
import com.example.model.ShortReelItem
import com.example.model.StaffMember
import com.example.model.StreamResource
import com.example.model.SubtitleTrack
import org.json.JSONArray
import org.json.JSONObject

internal object MovieBoxParsers {

    fun unwrapPayload(root: JSONObject): JSONObject {
        var current = root
        repeat(2) {
            val nested = current.optJSONObject("data")
                ?: current.optJSONObject("result")
                ?: current.optJSONObject("res")
                ?: current.optJSONObject("home")
            if (nested != null) {
                current = nested
            } else {
                val dataArray = current.optJSONArray("data") ?: current.optJSONArray("results")
                if (dataArray != null) {
                    return JSONObject().put("items", dataArray)
                }
                return current
            }
        }
        return current
    }

    fun parseHomePayload(payload: JSONObject): HomeFeedResult {
        val allCollected = mutableListOf<MediaItem>()
        val heroCollected = mutableListOf<MediaItem>()
        val sectionsMap = linkedMapOf<String, List<MediaItem>>()
        val homeSports = mutableListOf<LiveMatchItem>()

        val bannerArr = payload.optJSONArray("banner")
            ?: payload.optJSONArray("banners")
            ?: payload.optJSONArray("hero")
            ?: payload.optJSONArray("slider")
        if (bannerArr != null) {
            val parsedBanners = parseMediaArray(bannerArr, "hero", startRank = 1)
            heroCollected += parsedBanners
            allCollected += parsedBanners
        }

        val directSportsArr = payload.optJSONArray("sports") ?: payload.optJSONArray("liveMatches")
        if (directSportsArr != null) {
            homeSports += parseSportsArray(directSportsArr)
        }

        val sectionArr = payload.optJSONArray("operatingList")
            ?: payload.optJSONArray("sections")
            ?: payload.optJSONArray("rows")
            ?: payload.optJSONArray("homeList")
            ?: payload.optJSONArray("lists")
            ?: payload.optJSONArray("categories")

        if (sectionArr != null) {
            for (i in 0 until sectionArr.length()) {
                val secObj = sectionArr.optJSONObject(i) ?: continue
                val secTitle = secObj.optString("title")
                    .ifBlank { secObj.optString("name") }
                    .ifBlank { secObj.optString("heading") }
                    .ifBlank { "Featured ${i + 1}" }

                if (isExcludedSportsCategory(secTitle, secObj.optString("path"))) continue

                val bannerObj = secObj.optJSONObject("banner")
                if (bannerObj != null) {
                    val bItems = bannerObj.optJSONArray("items")
                    if (bItems != null) {
                        val parsed = parseMediaArray(bItems, "hero", startRank = 1)
                        heroCollected += parsed
                        allCollected += parsed
                        continue
                    }
                }

                val liveList = secObj.optJSONArray("liveList") ?: secObj.optJSONArray("matches")
                if (liveList != null) {
                    homeSports += parseSportsArray(liveList)
                    continue
                }

                val listArr = secObj.optJSONArray("subjects")
                    ?: secObj.optJSONArray("subjectList")
                    ?: secObj.optJSONArray("items")
                    ?: secObj.optJSONArray("list")
                    ?: secObj.optJSONArray("contents")
                    ?: secObj.optJSONArray("records")
                    ?: secObj.optJSONArray("data")
                    ?: continue

                val tag = classifySectionTag(secTitle)
                val parsed = parseMediaArray(listArr, tag, startRank = 1)
                if (parsed.isNotEmpty()) {
                    sectionsMap[secTitle] = parsed
                    allCollected += parsed
                }
            }
        }

        val flatKeys = listOf(
            "trending" to ("Trending Now" to "most_trending"),
            "trendingMovies" to ("Trending Movies" to "trending_movies"),
            "movies" to ("Trending Movies" to "trending_movies"),
            "trendingSeries" to ("Trending TV Series" to "trending_series"),
            "series" to ("Trending TV Series" to "trending_series"),
            "dramas" to ("Asian & Bengali Dramas" to "gems"),
            "anime" to ("Anime" to "anime"),
            "hot" to ("Hot Short TV" to "hot_short"),
            "items" to ("Explore Catalog" to "most_trending"),
            "subjects" to ("Explore Catalog" to "most_trending"),
            "results" to ("Explore Catalog" to "most_trending")
        )
        for ((key, pair) in flatKeys) {
            val arr = payload.optJSONArray(key) ?: continue
            val (displayTitle, tag) = pair
            val parsed = parseMediaArray(arr, tag, startRank = 1)
            if (parsed.isNotEmpty()) {
                sectionsMap.putIfAbsent(displayTitle, parsed)
                allCollected += parsed
            }
        }

        val mergedById = linkedMapOf<String, MediaItem>()
        for (item in allCollected) {
            val existing = mergedById[item.id]
            if (existing == null) {
                mergedById[item.id] = item
            } else {
                mergedById[item.id] = existing.copy(
                    sectionTags = (existing.sectionTags + item.sectionTags).distinct()
                )
            }
        }

        val finalAll = mergedById.values.toList()
        val finalHero = heroCollected.distinctBy { it.id }.ifEmpty { finalAll.take(4) }
        return HomeFeedResult(
            heroItems = finalHero,
            allItems = finalAll,
            sections = sectionsMap,
            sports = homeSports.distinctBy { it.id }
        )
    }

    fun extractItemList(payload: JSONObject, sectionTag: String): List<MediaItem> {
        val arr = payload.optJSONArray("items")
            ?: payload.optJSONArray("subjects")
            ?: payload.optJSONArray("subjectList")
            ?: payload.optJSONArray("results")
            ?: payload.optJSONArray("list")
            ?: payload.optJSONArray("contents")
            ?: payload.optJSONArray("records")
            ?: payload.optJSONArray("movies")
            ?: payload.optJSONArray("works")
            ?: payload.optJSONArray("shorts")
            ?: payload.optJSONArray("upcoming")
            ?: payload.optJSONArray("data")
            ?: return emptyList()
        return parseMediaArray(arr, sectionTag, startRank = 1)
    }

    fun parseMediaArray(
        arr: JSONArray,
        sectionTag: String,
        startRank: Int
    ): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val inner = obj.optJSONObject("subject") ?: obj
            parseSingleMedia(inner, sectionTag, rank = startRank + i)?.let { list += it }
        }
        return list
    }

    fun parseSingleMedia(
        obj: JSONObject,
        sectionTag: String,
        rank: Int? = null
    ): MediaItem? {
        val title = obj.optString("title")
            .ifBlank { obj.optString("name") }
            .ifBlank { obj.optString("subjectName") }
            .ifBlank { obj.optString("originalTitle") }
            .trim()
        if (title.isBlank()) return null

        val rawId = obj.optString("subjectId")
            .ifBlank { obj.optString("id") }
            .ifBlank { obj.optString("_id") }
            .ifBlank { obj.optString("slug") }
            .ifBlank { title.lowercase().replace(Regex("[^a-z0-9]+"), "_") }

        val posterUrl = extractImageUrl(
            obj,
            listOf("cover", "verticalCover", "poster", "poster_path", "image", "thumbnail", "thumb", "verticalPoster", "img")
        )
        val backdropUrl = extractImageUrl(
            obj,
            listOf("backdrop", "banner", "horizontalCover", "bg", "stills")
        ) ?: posterUrl

        val genreRaw = when (val g = obj.opt("genre") ?: obj.opt("genres") ?: obj.opt("category")) {
            is JSONArray -> (0 until g.length()).mapNotNull { idx ->
                val item = g.opt(idx)
                when (item) {
                    is JSONObject -> item.optString("name").ifBlank { item.optString("title") }
                    else -> item?.toString()
                }
            }.filter { it.isNotBlank() }.take(2).joinToString(", ")
            is String -> g
            else -> ""
        }.ifBlank { "Drama" }

        val rating = obj.optDouble("imdbRatingValue", Double.NaN).takeIf { !it.isNaN() && it > 0.0 }
            ?: obj.optString("imdbRatingValue").toDoubleOrNull()
            ?: obj.optDouble("rating", Double.NaN).takeIf { !it.isNaN() && it > 0.0 }
            ?: obj.optString("rating").toDoubleOrNull()
            ?: obj.optDouble("score", 8.1).takeIf { !it.isNaN() && it > 0.0 }
            ?: 8.1

        val releaseDateText = obj.optString("releaseDate")
            .ifBlank { obj.optString("releaseTime") }
            .ifBlank { obj.optString("year") }
            .ifBlank { obj.optString("releaseYear") }
        val year = Regex("(19|20)\\d{2}").find(releaseDateText)?.value?.toIntOrNull() ?: 2026

        val country = obj.optString("countryName")
            .ifBlank { obj.optString("country") }
            .ifBlank { obj.optString("region") }
            .ifBlank { "International" }

        val subjectType = obj.optInt("subjectType", obj.optString("subjectType").toIntOrNull() ?: -1)
        val typeText = obj.optString("type").lowercase()
        val hasTitleSeasonHint = extractTitleSeasonCount(title) > 0
        val hasSeasonField = obj.optJSONArray("seasons")?.let { it.length() > 0 } == true ||
            readPositiveInt(obj, listOf("totalSeasons", "seasonCount", "numberOfSeasons", "seasons", "maxSeason")) > 0
        val mediaType = when {
            genreRaw.contains("Anime", ignoreCase = true) ||
                typeText.contains("anime") ||
                sectionTag.contains("anime", ignoreCase = true) ||
                (genreRaw.contains("Animation", ignoreCase = true) && (hasTitleSeasonHint || hasSeasonField || subjectType == 2)) -> MediaType.ANIME
            sectionTag == "hot_short" || typeText.contains("short") -> MediaType.SHORT_TV
            subjectType == 2 ||
                typeText.contains("tv") ||
                typeText.contains("series") ||
                sectionTag.contains("/tv", ignoreCase = true) ||
                sectionTag == "trending_series" ||
                hasTitleSeasonHint ||
                hasSeasonField -> MediaType.SERIES
            typeText.contains("drama") -> MediaType.DRAMA
            else -> MediaType.MOVIE
        }

        val langBadge = extractLanguageBadge(obj, title)
        val synopsis = obj.optString("description")
            .ifBlank { obj.optString("synopsis") }
            .ifBlank { obj.optString("overview") }
            .ifBlank { obj.optString("plot") }
            .ifBlank { "Stream $title in HD on Muvio." }

        val duration = obj.optString("duration")
            .ifBlank { obj.optString("runtime") }
            .ifBlank { if (mediaType == MediaType.SERIES) "Series" else "HD Stream" }

        val bookedCount = obj.optString("bookedCount")
            .ifBlank { obj.optString("booked") }
            .ifBlank { obj.optString("reserveCount") }

        val fallbackRes = when (mediaType) {
            MediaType.ANIME -> R.drawable.img_poster_anime
            MediaType.DRAMA, MediaType.SERIES, MediaType.SHORT_TV -> R.drawable.img_poster_drama
            MediaType.MOVIE -> R.drawable.img_poster_action
        }

        val tags = mutableListOf(sectionTag)
        if (mediaType == MediaType.ANIME) tags += "anime"
        if (langBadge.equals("Bengali", ignoreCase = true)) tags += "bengali"
        if (mediaType == MediaType.SERIES || mediaType == MediaType.DRAMA) tags += "trending_series"
        if (mediaType == MediaType.MOVIE) tags += "trending_movies"

        return MediaItem(
            id = rawId,
            title = title,
            year = year,
            country = country,
            genre = genreRaw,
            rating = (Math.round(rating * 10.0) / 10.0),
            languageBadge = langBadge,
            rankNumber = rank?.takeIf { it in 1..10 },
            mediaType = mediaType,
            subCategory = when {
                mediaType == MediaType.ANIME -> "Anime"
                langBadge.equals("Bengali", ignoreCase = true) -> "New Bengali"
                mediaType == MediaType.SERIES -> "Top Series"
                else -> "Cinema"
            },
            sectionTags = tags.distinct(),
            synopsis = synopsis,
            duration = duration,
            poster = PosterArtwork(
                remoteUrl = posterUrl,
                fallbackRes = fallbackRes,
                tintHex = 0xFF101920,
                accentHex = 0xFF00E07A,
                badgeTitle = title
            ),
            backdropRes = R.drawable.img_hero_cinema,
            backdropUrl = backdropUrl,
            resources = listOf(
                StreamResource(
                    id = "${rawId}_default",
                    label = "$title [HD Stream]",
                    language = langBadge ?: "Original",
                    quality = "HD 1080p",
                    uploader = "Muvio Direct"
                )
            ),
            releaseDateText = releaseDateText,
            bookedCount = bookedCount
        )
    }

    fun parseDetailsPayload(
        root: JSONObject,
        payload: JSONObject,
        fallbackTitle: String
    ): MediaDetailResult {
        val subjectObj = payload.optJSONObject("subject")
            ?: root.optJSONObject("subject")
            ?: payload.optJSONObject("detail")
            ?: payload.optJSONObject("details")
            ?: payload.optJSONObject("info")
            ?: payload.optJSONObject("metadata")
            ?: payload

        var parsed = parseSingleMedia(subjectObj, "detail")
        val effectiveTitle = parsed?.title?.ifBlank { fallbackTitle } ?: fallbackTitle

        val containers = listOfNotNull(
            payload,
            root,
            subjectObj,
            payload.optJSONObject("resource"),
            root.optJSONObject("resource"),
            subjectObj.optJSONObject("resource"),
            payload.optJSONObject("detail"),
            payload.optJSONObject("details"),
            payload.optJSONObject("info"),
            payload.optJSONObject("media"),
            payload.optJSONObject("metadata"),
            payload.optJSONObject("playInfo")
        ).distinct()

        val seasonEpisodes = linkedMapOf<Int, List<EpisodeItem>>()

        // 1. Parse seasons array across all possible containers & key names
        val seasonArrayKeys = listOf("seasons", "seasonList", "seasonsList", "season_list", "seriesSeasons", "allSeasons")
        var foundSeasonsArr: JSONArray? = null
        outer@ for (container in containers) {
            for (key in seasonArrayKeys) {
                val arr = container.optJSONArray(key)
                if (arr != null && arr.length() > 0) {
                    foundSeasonsArr = arr
                    break@outer
                }
            }
        }

        if (foundSeasonsArr != null) {
            val parsedEntries = mutableListOf<Pair<Int, List<EpisodeItem>>>()
            for (i in 0 until foundSeasonsArr.length()) {
                val sObj = foundSeasonsArr.optJSONObject(i) ?: continue
                val rawSe = readIntOrNull(sObj, listOf("se", "season", "seasonNumber", "season_number", "number", "num", "index"))
                val seasonNum = rawSe ?: (i + 1)
                val eps = parseSeasonObjectEpisodes(sObj)
                parsedEntries += seasonNum to eps
            }
            val hasPositiveSeasons = parsedEntries.any { it.first > 0 }
            parsedEntries.forEachIndexed { idx, (rawNum, eps) ->
                if (hasPositiveSeasons && rawNum <= 0 && eps.isEmpty()) return@forEachIndexed
                val normalizedNum = if (rawNum > 0) rawNum else (idx + 1)
                seasonEpisodes[normalizedNum] = eps
            }
        }

        // 2. Parse seasons as a JSONObject map (e.g., { "1": [...], "2": [...] } or { "S1": 24 })
        if (seasonEpisodes.isEmpty()) {
            val mapKeys = listOf("seasons", "episodesBySeason", "seasonEpisodes", "seasonMap")
            outerMap@ for (container in containers) {
                for (key in mapKeys) {
                    val mapObj = container.optJSONObject(key) ?: continue
                    val keys = mapObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val seNum = Regex("\\d+").find(k)?.value?.toIntOrNull() ?: continue
                        val value = mapObj.opt(k)
                        val eps = when (value) {
                            is JSONArray -> parseEpisodeArray(value)
                            is JSONObject -> parseSeasonObjectEpisodes(value)
                            is Number -> generateNumberedEpisodes(value.toInt())
                            is String -> parseAllEpString(value)
                            else -> emptyList()
                        }
                        if (eps.isNotEmpty()) {
                            seasonEpisodes[seNum.coerceAtLeast(1)] = eps
                        }
                    }
                    if (seasonEpisodes.isNotEmpty()) break@outerMap
                }
            }
        }

        // 3. Parse flat episodes array (grouping by season number if episodes carry se/season)
        if (seasonEpisodes.values.all { it.isEmpty() }) {
            val epArrayKeys = listOf("episodes", "episodeList", "episode_list", "eps", "videos")
            outerEp@ for (container in containers) {
                for (key in epArrayKeys) {
                    val epArr = container.optJSONArray(key)
                    if (epArr != null && epArr.length() > 0) {
                        val grouped = linkedMapOf<Int, MutableList<EpisodeItem>>()
                        for (i in 0 until epArr.length()) {
                            val epObj = epArr.optJSONObject(i) ?: continue
                            val seNum = readPositiveInt(epObj, listOf("se", "season", "seasonNumber", "season_number")).coerceAtLeast(1)
                            val epNum = readPositiveInt(epObj, listOf("ep", "episode", "episodeNumber", "episode_number", "number", "num")).let {
                                if (it > 0) it else (grouped[seNum]?.size ?: 0) + 1
                            }
                            val epTitle = epObj.optString("title").ifBlank { epObj.optString("name") }.ifBlank { "Episode $epNum" }
                            val epDur = epObj.optString("duration").ifBlank { epObj.optString("runtime") }.ifBlank { "HD" }
                            val epUrl = epObj.optString("url").ifBlank { epObj.optString("streamUrl") }
                            grouped.getOrPut(seNum) { mutableListOf() } += EpisodeItem(epNum, epTitle, epDur, epUrl)
                        }
                        if (grouped.isNotEmpty()) {
                            grouped.forEach { (se, list) -> seasonEpisodes[se] = list }
                            break@outerEp
                        }
                    }
                }
            }
        }

        // 4. Determine total seasons count from explicit integer fields + parsed seasons + title hints (e.g. S1-S4)
        var declaredSeasonsCount = 0
        var declaredEpisodesCount = 0
        for (container in containers) {
            val sCount = readPositiveInt(
                container,
                listOf("totalSeasons", "total_seasons", "seasonCount", "season_count", "numberOfSeasons", "number_of_seasons", "seasons", "maxSeason")
            )
            if (sCount > declaredSeasonsCount) declaredSeasonsCount = sCount
            val eCount = readPositiveInt(
                container,
                listOf("totalEpisodes", "total_episodes", "episodeCount", "episode_count", "maxEp", "max_ep", "epCount", "episodes")
            )
            if (eCount > declaredEpisodesCount) declaredEpisodesCount = eCount
        }

        val titleSeasonsHint = maxOf(
            extractTitleSeasonCount(fallbackTitle),
            extractTitleSeasonCount(effectiveTitle)
        )
        val parsedSeasonsMax = maxOf(seasonEpisodes.size, seasonEpisodes.keys.maxOrNull() ?: 0)
        val hasExplicitApiSeasons = foundSeasonsArr != null || seasonEpisodes.isNotEmpty()
        val seasonsCount = if (hasExplicitApiSeasons) {
            maxOf(1, parsedSeasonsMax, declaredSeasonsCount)
        } else {
            maxOf(1, declaredSeasonsCount, titleSeasonsHint)
        }

        val isSeriesLike = seasonsCount > 1 ||
            seasonEpisodes.isNotEmpty() ||
            declaredEpisodesCount > 1 ||
            parsed?.mediaType == MediaType.SERIES ||
            parsed?.mediaType == MediaType.ANIME ||
            parsed?.mediaType == MediaType.DRAMA ||
            parsed?.mediaType == MediaType.SHORT_TV

        if (isSeriesLike && !hasExplicitApiSeasons) {
            // Only infer fallback per-season episode counts when the API returned NO explicit seasons array
            val perSeasonFallback = when {
                declaredEpisodesCount in 1..60 && seasonsCount == 1 -> declaredEpisodesCount
                declaredEpisodesCount in 1..200 && seasonsCount > 1 -> (declaredEpisodesCount / seasonsCount).coerceIn(6, 26)
                else -> 12
            }
            for (se in 1..seasonsCount) {
                if (seasonEpisodes[se].isNullOrEmpty()) {
                    seasonEpisodes[se] = generateNumberedEpisodes(perSeasonFallback)
                }
            }
        } else if (isSeriesLike && seasonEpisodes.values.all { it.isEmpty() }) {
            // Explicit seasons existed in API, but all had 0 parsed episodes -> seed clean 12-ep fallback for Season 1
            val perSeasonFallback = if (declaredEpisodesCount in 1..60 && seasonsCount == 1) declaredEpisodesCount else 12
            for (se in 1..seasonsCount) {
                seasonEpisodes[se] = generateNumberedEpisodes(perSeasonFallback)
            }
        }

        val sortedSeasonEpisodes = seasonEpisodes.toSortedMap().toMap()
        val firstSeasonEpisodes = sortedSeasonEpisodes[1]
            ?: sortedSeasonEpisodes.values.firstOrNull { it.isNotEmpty() }
            ?: emptyList()

        // Parse Dubs (+ Dubs in /details) across containers
        val dubs = mutableListOf<DubVersion>()
        val dubKeys = listOf("dubs", "languages", "versions", "audioTracks", "dubList")
        for (container in containers) {
            for (key in dubKeys) {
                val dubsArr = container.optJSONArray(key) ?: continue
                for (i in 0 until dubsArr.length()) {
                    val dObj = dubsArr.optJSONObject(i) ?: continue
                    val dTitle = dObj.optString("title").ifBlank { dObj.optString("name") }.ifBlank { fallbackTitle }
                    val dLang = dObj.optString("language")
                        .ifBlank { dObj.optString("lang") }
                        .ifBlank { dObj.optString("lanName") }
                        .ifBlank { dObj.optString("dub") }
                        .ifBlank { extractLanguageBadge(dObj, dTitle).orEmpty() }
                        .trim()
                    if (dLang.isBlank()) continue
                    val dId = dObj.optString("subjectId").ifBlank { dObj.optString("id") }
                    dubs += DubVersion(title = dTitle, language = dLang, subjectId = dId)
                }
            }
        }

        // Parse Cast / Staff (for /staff?staffId=...)
        val castList = mutableListOf<StaffMember>()
        val staffKeys = listOf("stars", "cast", "staff", "actors")
        for (container in containers) {
            for (key in staffKeys) {
                val staffArr = container.optJSONArray(key) ?: continue
                for (i in 0 until staffArr.length()) {
                    val sObj = staffArr.optJSONObject(i) ?: continue
                    val name = sObj.optString("name").ifBlank { sObj.optString("staffName") }.trim()
                    if (name.isBlank()) continue
                    val id = sObj.optString("staffId").ifBlank { sObj.optString("id") }.ifBlank { "${i + 1}" }
                    val role = sObj.optString("character")
                        .ifBlank { sObj.optString("role") }
                        .ifBlank { sObj.optString("profession") }
                        .ifBlank { "Cast" }
                    val avatar = extractImageUrl(sObj, listOf("avatarUrl", "avatar", "photo", "image"))
                    castList += StaffMember(id = id, name = name, role = role, avatarUrl = avatar)
                }
                if (castList.isNotEmpty()) break
            }
            if (castList.isNotEmpty()) break
        }

        if (parsed != null) {
            val resolvedType = if (isSeriesLike && parsed.mediaType == MediaType.MOVIE) {
                if (parsed.genre.contains("Animation", ignoreCase = true) || parsed.genre.contains("Anime", ignoreCase = true)) {
                    MediaType.ANIME
                } else {
                    MediaType.SERIES
                }
            } else {
                parsed.mediaType
            }
            parsed = parsed.copy(
                mediaType = resolvedType,
                episodes = firstSeasonEpisodes.ifEmpty { parsed.episodes },
                seasonEpisodes = sortedSeasonEpisodes.ifEmpty { parsed.seasonEpisodes },
                dubs = dubs.distinctBy { it.language.lowercase() },
                cast = castList.distinctBy { it.name }.take(14)
            )
        }

        val recArr = payload.optJSONArray("recommendations")
            ?: root.optJSONArray("recommendations")
            ?: payload.optJSONArray("forYou")
            ?: payload.optJSONArray("related")
            ?: root.optJSONArray("related")
            ?: payload.optJSONArray("similar")
        val recs = if (recArr != null) parseMediaArray(recArr, "most_trending", startRank = 1) else emptyList()

        if (parsed == null && firstSeasonEpisodes.isNotEmpty()) {
            parsed = MediaItem(
                id = fallbackTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
                title = fallbackTitle,
                year = 2026,
                country = "International",
                genre = "Series",
                rating = 8.2,
                languageBadge = "HD",
                mediaType = MediaType.SERIES,
                subCategory = "Top Series",
                sectionTags = listOf("trending_series"),
                synopsis = "",
                duration = "${firstSeasonEpisodes.size} Episodes",
                poster = PosterArtwork(fallbackRes = R.drawable.img_poster_drama, tintHex = 0xFF101920, accentHex = 0xFF00E07A),
                backdropRes = R.drawable.img_hero_cinema,
                episodes = firstSeasonEpisodes,
                seasonEpisodes = sortedSeasonEpisodes,
                dubs = dubs.distinctBy { it.language.lowercase() },
                cast = castList
            )
        }

        return MediaDetailResult(
            updatedItem = parsed,
            recommendations = recs,
            seasonsCount = seasonsCount,
            seasonEpisodes = sortedSeasonEpisodes
        )
    }

    fun inferSeriesSeasonsAndEpisodes(media: MediaItem): Pair<Int, Map<Int, List<EpisodeItem>>> {
        if (media.seasonEpisodes.isNotEmpty()) {
            return maxOf(media.seasonEpisodes.size, media.seasonEpisodes.keys.maxOrNull() ?: 1) to media.seasonEpisodes
        }
        val titleSeasons = extractTitleSeasonCount(media.title)
        val isSeriesLike = titleSeasons > 0 ||
            media.episodes.isNotEmpty() ||
            media.mediaType == MediaType.SERIES ||
            media.mediaType == MediaType.ANIME ||
            media.mediaType == MediaType.DRAMA ||
            media.mediaType == MediaType.SHORT_TV
        if (!isSeriesLike) return 1 to emptyMap()

        val count = maxOf(1, titleSeasons)
        val map = linkedMapOf<Int, List<EpisodeItem>>()
        for (se in 1..count) {
            map[se] = if (se == 1 && media.episodes.isNotEmpty()) {
                media.episodes
            } else {
                generateNumberedEpisodes(12)
            }
        }
        return count to map
    }

    internal fun extractTitleSeasonCount(title: String): Int {
        // Matches patterns like S1-S4, S01-S04, S1-4, Season 1-4, Season 1 to 4
        val rangeRegex = Regex("(?i)\\b(?:S0?(\\d{1,2})\\s*[-–to]+\\s*S?0?(\\d{1,2})|Season\\s*(\\d{1,2})\\s*[-–to]+\\s*(?:Season\\s*)?(\\d{1,2}))\\b")
        val rangeMatch = rangeRegex.find(title)
        if (rangeMatch != null) {
            val endVal = rangeMatch.groupValues[2].toIntOrNull()
                ?: rangeMatch.groupValues[4].toIntOrNull()
                ?: 0
            if (endVal in 1..30) return endVal
        }
        // Matches single season markers like S2, S03, Season 2
        val singleRegex = Regex("(?i)\\b(?:S0?(\\d{1,2})|Season\\s*(\\d{1,2}))\\b")
        val singleMatch = singleRegex.find(title)
        if (singleMatch != null) {
            val num = singleMatch.groupValues[1].toIntOrNull()
                ?: singleMatch.groupValues[2].toIntOrNull()
                ?: 0
            if (num in 1..30) return num
        }
        return 0
    }

    private fun parseSeasonObjectEpisodes(sObj: JSONObject): List<EpisodeItem> {
        // 1. Explicit episodes array inside season object
        val epKeys = listOf("episodes", "episodeList", "episode_list", "items", "list", "eps")
        for (key in epKeys) {
            val arr = sObj.optJSONArray(key)
            if (arr != null && arr.length() > 0) {
                return parseEpisodeArray(arr)
            }
        }
        // 2. Explicit episode count keys (prioritize maxEp / episodeCount; avoid generic length/count)
        val explicitCount = readPositiveInt(
            sObj,
            listOf("maxEp", "max_ep", "episodeCount", "episodesCount", "epCount", "ep_count", "totalEpisodes", "total_episodes", "episodes")
        )
        if (explicitCount > 0) {
            return generateNumberedEpisodes(explicitCount)
        }
        // 3. MovieBox resource.seasons[].resolutions[].epNum
        val resArr = sObj.optJSONArray("resolutions")
        if (resArr != null && resArr.length() > 0) {
            var maxResEp = 0
            for (i in 0 until resArr.length()) {
                val rObj = resArr.optJSONObject(i) ?: continue
                val epNum = readPositiveInt(rObj, listOf("epNum", "ep_num", "maxEp"))
                if (epNum > maxResEp) maxResEp = epNum
            }
            if (maxResEp > 0) return generateNumberedEpisodes(maxResEp)
        }
        // 4. allEp / epList string (e.g. "1,2,3...13" or "1-13")
        val allEpStr = sObj.optString("allEp")
            .ifBlank { sObj.optString("allEpisodes") }
            .ifBlank { sObj.optString("epList") }
            .trim()
        if (allEpStr.isNotBlank()) {
            val parsedFromStr = parseAllEpString(allEpStr)
            if (parsedFromStr.isNotEmpty()) return parsedFromStr
        }
        return emptyList()
    }

    private fun parseEpisodeArray(epArr: JSONArray): List<EpisodeItem> {
        val out = mutableListOf<EpisodeItem>()
        for (i in 0 until epArr.length()) {
            val entry = epArr.opt(i)
            when (entry) {
                is JSONObject -> {
                    val epNum = readPositiveInt(
                        entry,
                        listOf("ep", "episode", "episodeNumber", "episode_number", "number", "num", "index")
                    ).let { if (it > 0) it else i + 1 }
                    val epTitle = entry.optString("title").ifBlank { entry.optString("name") }.ifBlank { "Episode $epNum" }
                    val epDur = entry.optString("duration").ifBlank { entry.optString("runtime") }.ifBlank { "HD" }
                    val epUrl = entry.optString("url").ifBlank { entry.optString("streamUrl") }
                    out += EpisodeItem(epNum, epTitle, epDur, epUrl)
                }
                is Number -> {
                    val num = entry.toInt().coerceAtLeast(1)
                    out += EpisodeItem(num, "Episode $num", "HD")
                }
                is String -> {
                    val num = entry.trim().toIntOrNull() ?: (i + 1)
                    out += EpisodeItem(num, "Episode $num", "HD")
                }
            }
        }
        return out
    }

    private fun parseAllEpString(raw: String): List<EpisodeItem> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return emptyList()
        val rangeMatch = Regex("^(\\d+)\\s*[-–]\\s*(\\d+)$").find(trimmed)
        if (rangeMatch != null) {
            val start = rangeMatch.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
            val end = rangeMatch.groupValues[2].toIntOrNull()?.coerceAtMost(500) ?: start
            if (end >= start) {
                return (start..end).map { EpisodeItem(it, "Episode $it", "HD") }
            }
        }
        val parts = trimmed.split(",", " ").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..500 }.distinct()
        if (parts.size > 1) {
            return parts.take(500).map { EpisodeItem(it, "Episode $it", "HD") }
        }
        val single = trimmed.toIntOrNull()
        if (single != null && single in 1..100) {
            return generateNumberedEpisodes(single)
        }
        return emptyList()
    }

    internal fun generateNumberedEpisodes(count: Int): List<EpisodeItem> {
        val safe = count.coerceIn(1, 500)
        return (1..safe).map { epNum -> EpisodeItem(epNum, "Episode $epNum", "HD") }
    }

    private fun readIntOrNull(obj: JSONObject, keys: List<String>): Int? {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.opt(key)
            when (value) {
                is Number -> return value.toInt()
                is String -> value.trim().toIntOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun readPositiveInt(obj: JSONObject, keys: List<String>): Int {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.opt(key)
            val parsed = when (value) {
                is Number -> value.toInt()
                is String -> value.trim().toIntOrNull() ?: 0
                else -> 0
            }
            if (parsed > 0) return parsed
        }
        return 0
    }

    fun parseStreamsPayload(payload: JSONObject, title: String): List<StreamResource> {
        val result = mutableListOf<StreamResource>()
        val subtitles = parseSubtitles(payload)
        val rootHeaders = extractHeaders(payload)

        val directUrl = payload.optString("streamUrl")
            .ifBlank { payload.optString("playUrl") }
            .ifBlank { payload.optString("url") }
            .ifBlank { payload.optString("hls") }
            .ifBlank { payload.optString("mp4") }
        if (directUrl.startsWith("http")) {
            result += StreamResource(
                id = "direct_0",
                label = "$title [Direct Stream]",
                language = "Original",
                quality = payload.optString("quality").ifBlank { "HD 1080p" },
                uploader = "Muvio Direct",
                streamUrl = directUrl,
                subtitles = subtitles,
                headers = rootHeaders
            )
        }

        listOf("sources", "qualities", "streams", "urls").forEach { objKey ->
            val mapObj = payload.optJSONObject(objKey)
            if (mapObj != null) {
                val keys = mapObj.keys()
                var idx = 0
                while (keys.hasNext()) {
                    val qKey = keys.next()
                    val urlVal = mapObj.optString(qKey)
                    if (urlVal.startsWith("http")) {
                        result += StreamResource(
                            id = "${objKey}_map_$idx",
                            label = "$title [$qKey]",
                            language = "Original",
                            quality = qKey,
                            uploader = "Muvio Direct",
                            streamUrl = urlVal,
                            subtitles = subtitles,
                            headers = rootHeaders
                        )
                        idx++
                    }
                }
            }
        }

        val arrayKeys = listOf("processedSources", "streams", "sources", "downloads", "qualities", "formats", "urls", "links", "items", "list")
        for (key in arrayKeys) {
            val arr = payload.optJSONArray(key) ?: continue
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                if (obj != null) {
                    val url = obj.optString("url")
                        .ifBlank { obj.optString("directUrl") }
                        .ifBlank { obj.optString("file") }
                        .ifBlank { obj.optString("src") }
                        .ifBlank { obj.optString("link") }
                        .ifBlank { obj.optString("playUrl") }
                    if (url.startsWith("http")) {
                        val quality = obj.optString("quality")
                            .ifBlank { obj.optString("qualityLabel") }
                            .ifBlank { obj.optString("resolution") }
                            .ifBlank { obj.optString("resolutions") }
                            .ifBlank { obj.optString("label") }
                            .ifBlank { "HD" }
                        val lang = obj.optString("language")
                            .ifBlank { obj.optString("audio") }
                            .ifBlank { obj.optString("lang") }
                            .ifBlank { obj.optString("dub") }
                            .ifBlank { payload.optString("activeAudio") }
                            .ifBlank { "Original" }
                        val itemHeaders = rootHeaders + extractHeaders(obj)
                        result += StreamResource(
                            id = "${key}_$i",
                            label = "$title [$quality]",
                            language = lang,
                            quality = quality,
                            uploader = "Muvio Direct",
                            streamUrl = url,
                            subtitles = subtitles,
                            headers = itemHeaders
                        )
                    }
                } else {
                    val urlStr = arr.optString(i)
                    if (urlStr.startsWith("http")) {
                        result += StreamResource(
                            id = "${key}_$i",
                            label = "$title [Server ${i + 1}]",
                            language = "Original",
                            quality = "HD",
                            uploader = "Muvio Direct",
                            streamUrl = urlStr,
                            subtitles = subtitles,
                            headers = rootHeaders
                        )
                    }
                }
            }
        }

        return result
            .map { res ->
                val normalizedQ = normalizeQualityLabel(res.quality)
                res.copy(
                    quality = normalizedQ,
                    label = "$title [$normalizedQ]"
                )
            }
            .distinctBy { "${it.quality.lowercase()}|${it.language.lowercase()}" }
            .sortedByDescending { qualitySortRank(it.quality) }
    }

    private fun normalizeQualityLabel(raw: String): String {
        val clean = raw.trim()
        val digits = Regex("\\d{3,4}").find(clean)?.value
        return when {
            clean.contains("4k", ignoreCase = true) || digits == "2160" -> "4K"
            digits == "1440" -> "1440p"
            digits == "1080" -> "1080p"
            digits == "720" -> "720p"
            digits == "540" -> "540p"
            digits == "480" -> "480p"
            digits == "360" -> "360p"
            digits == "240" -> "240p"
            clean.isBlank() -> "HD"
            else -> clean
        }
    }

    private fun qualitySortRank(quality: String): Int {
        return when (quality.lowercase()) {
            "4k", "2160p" -> 2160
            "1440p" -> 1440
            "1080p", "full hd", "fhd" -> 1080
            "720p", "hd" -> 720
            "540p" -> 540
            "480p", "sd" -> 480
            "360p" -> 360
            "240p" -> 240
            else -> Regex("\\d+").find(quality)?.value?.toIntOrNull() ?: 600
        }
    }

    fun parseCategories(payload: JSONObject): List<ApiCategory> {
        val arr = payload.optJSONArray("categories")
            ?: payload.optJSONArray("items")
            ?: payload.optJSONArray("list")
            ?: payload.optJSONArray("data")
            ?: return emptyList()
        val list = mutableListOf<ApiCategory>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val name = obj.optString("name").ifBlank { obj.optString("title") }.ifBlank { obj.optString("label") }.trim()
            val path = obj.optString("path").ifBlank { obj.optString("url") }.ifBlank { obj.optString("slug") }.trim()
            if (name.isBlank() || isExcludedSportsCategory(name, path)) continue
            val normalizedPath = if (path.isNotBlank() && !path.startsWith("/")) "/$path" else path.ifBlank { "/${name.lowercase().replace(" ", "-")}" }
            list += ApiCategory(
                id = obj.optString("id").ifBlank { "cat_$i" },
                name = name,
                path = normalizedPath
            )
        }
        return list.distinctBy { it.path }
    }

    fun parseStringList(payload: JSONObject, keys: List<String>): List<String> {
        for (key in keys) {
            val arr = payload.optJSONArray(key) ?: continue
            val out = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                when (val entry = arr.opt(i)) {
                    is String -> if (entry.isNotBlank()) out += entry.trim()
                    is JSONObject -> {
                        val word = entry.optString("word")
                            .ifBlank { entry.optString("keyword") }
                            .ifBlank { entry.optString("title") }
                            .ifBlank { entry.optString("name") }
                            .ifBlank { entry.optString("query") }
                            .trim()
                        if (word.isNotBlank()) out += word
                    }
                }
            }
            if (out.isNotEmpty()) return out.distinct()
        }
        return emptyList()
    }

    fun parseSportsPayload(payload: JSONObject): List<LiveMatchItem> {
        val list = mutableListOf<LiveMatchItem>()
        val directArr = payload.optJSONArray("matches")
            ?: payload.optJSONArray("live")
            ?: payload.optJSONArray("sports")
            ?: payload.optJSONArray("items")
            ?: payload.optJSONArray("list")
            ?: payload.optJSONArray("events")
        if (directArr != null) {
            list += parseSportsArray(directArr)
        }
        // Check nested categories like football, cricket, nba
        listOf("football", "cricket", "nba", "basketball").forEach { sportKey ->
            val arr = payload.optJSONArray(sportKey)
            if (arr != null) {
                list += parseSportsArray(arr, defaultSport = sportKey.replaceFirstChar { it.uppercase() })
            }
        }
        return list.distinctBy { it.id }
    }

    fun parseSportsArray(arr: JSONArray, defaultSport: String = "Sports"): List<LiveMatchItem> {
        val out = mutableListOf<LiveMatchItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val homeObj = obj.optJSONObject("home") ?: obj.optJSONObject("team1") ?: obj.optJSONObject("homeTeam")
            val awayObj = obj.optJSONObject("away") ?: obj.optJSONObject("team2") ?: obj.optJSONObject("awayTeam")

            val homeName = homeObj?.optString("name")
                ?.ifBlank { homeObj.optString("title") }
                ?: obj.optString("homeName")
                    .ifBlank { obj.optString("homeTeam") }
                    .ifBlank { obj.optString("team1") }
                    .ifBlank { obj.optString("title").substringBefore(" vs ", "Team A") }
            val awayName = awayObj?.optString("name")
                ?.ifBlank { awayObj.optString("title") }
                ?: obj.optString("awayName")
                    .ifBlank { obj.optString("awayTeam") }
                    .ifBlank { obj.optString("team2") }
                    .ifBlank { obj.optString("title").substringAfter(" vs ", "Team B") }

            val league = obj.optString("league")
                .ifBlank { obj.optString("tournament") }
                .ifBlank { obj.optString("competition") }
                .ifBlank { defaultSport }

            val streamUrl = obj.optString("streamUrl")
                .ifBlank { obj.optString("m3u8") }
                .ifBlank { obj.optString("playUrl") }
                .ifBlank { obj.optString("url") }
                .ifBlank {
                    val streamsArr = obj.optJSONArray("streams") ?: obj.optJSONArray("links")
                    streamsArr?.optJSONObject(0)?.optString("url")
                        ?: streamsArr?.optString(0).orEmpty()
                }

            val homeScore = obj.optString("homeScore").ifBlank { homeObj?.optString("score").orEmpty() }
            val awayScore = obj.optString("awayScore").ifBlank { awayObj?.optString("score").orEmpty() }
            val scoreText = obj.optString("score")
                .ifBlank { if (homeScore.isNotBlank() && awayScore.isNotBlank()) "$homeScore - $awayScore" else "" }
                .ifBlank { obj.optString("startTime").ifBlank { obj.optString("time") }.ifBlank { "LIVE" } }

            val statusStr = obj.optString("status").lowercase()
            val isLive = obj.optBoolean("isLive", false) ||
                statusStr == "live" || statusStr == "playing" ||
                streamUrl.contains(".m3u8", ignoreCase = true)

            out += LiveMatchItem(
                id = obj.optString("id").ifBlank { obj.optString("matchId") }.ifBlank { "sport_$i" },
                league = league,
                homeTeam = homeName.trim(),
                homeShort = homeName.trim().take(3).uppercase(),
                homeBadgeColor = 0xFF1D4ED8,
                awayTeam = awayName.trim(),
                awayShort = awayName.trim().take(3).uppercase(),
                awayBadgeColor = 0xFF00B362,
                scoreOrTime = scoreText,
                subTime = obj.optString("time").ifBlank { if (isLive) "LIVE" else "Upcoming" },
                isLive = isLive,
                streamUrl = streamUrl,
                homeLogoUrl = extractImageUrl(homeObj ?: obj, listOf("logo", "icon", "homeLogo", "badge")),
                awayLogoUrl = extractImageUrl(awayObj ?: obj, listOf("logo", "icon", "awayLogo", "badge")),
                sportType = obj.optString("sport").ifBlank { defaultSport },
                headers = extractHeaders(obj)
            )
        }
        return out
    }

    fun parseShortsPayload(payload: JSONObject): List<ShortReelItem> {
        val arr = payload.optJSONArray("items")
            ?: payload.optJSONArray("shorts")
            ?: payload.optJSONArray("subjects")
            ?: payload.optJSONArray("list")
            ?: payload.optJSONArray("data")
            ?: return emptyList()
        val out = mutableListOf<ShortReelItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val inner = obj.optJSONObject("subject") ?: obj
            val title = inner.optString("title").ifBlank { inner.optString("name") }.trim()
            if (title.isBlank()) continue
            val id = inner.optString("subjectId").ifBlank { inner.optString("id") }.ifBlank { "short_$i" }
            val posterUrl = extractImageUrl(inner, listOf("cover", "poster", "thumbnail", "image", "verticalCover"))
            val streamUrl = inner.optString("streamUrl")
                .ifBlank { inner.optString("videoUrl") }
                .ifBlank { inner.optString("playUrl") }
                .ifBlank { inner.optString("url").takeIf { it.contains(".mp4") || it.contains(".m3u8") }.orEmpty() }
            out += ShortReelItem(
                id = "reel_$id",
                linkedMediaId = id,
                title = title,
                episodeTag = inner.optString("tag").ifBlank { inner.optString("episodeTag") }.ifBlank { "Short Drama • HD" },
                description = inner.optString("description").ifBlank { inner.optString("synopsis") }.ifBlank { "Watch full micro-drama on Muvio." },
                likesCount = inner.optInt("likes", 1200 + (i * 350)),
                commentsCount = inner.optInt("comments", 45 + (i * 12)),
                poster = PosterArtwork(
                    remoteUrl = posterUrl,
                    fallbackRes = R.drawable.img_poster_drama,
                    tintHex = 0xFF1A1424,
                    accentHex = 0xFF00E07A
                ),
                streamUrl = streamUrl
            )
        }
        return out
    }

    fun parseCommentsPayload(payload: JSONObject): List<CommentItem> {
        val arr = payload.optJSONArray("comments")
            ?: payload.optJSONArray("postList")
            ?: payload.optJSONArray("items")
            ?: payload.optJSONArray("list")
            ?: payload.optJSONArray("data")
            ?: return emptyList()
        val avatarPalette = listOf(0xFF2A5B4C, 0xFFE91E63, 0xFF3B82F6, 0xFF8B5CF6, 0xFFF59E0B)
        val out = mutableListOf<CommentItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val userObj = obj.optJSONObject("user")
            val author = obj.optString("author")
                .ifBlank { obj.optString("userName") }
                .ifBlank { obj.optString("nickname") }
                .ifBlank { userObj?.optString("nickname").orEmpty() }
                .ifBlank { userObj?.optString("name").orEmpty() }
                .ifBlank { "Muvio User" }
            val text = obj.optString("content")
                .ifBlank { obj.optString("text") }
                .ifBlank { obj.optString("message") }
                .ifBlank { obj.optString("comment") }
                .trim()
            if (text.isBlank()) continue
            out += CommentItem(
                id = obj.optString("id").ifBlank { obj.optString("commentId") }.ifBlank { "api_c_$i" },
                author = author,
                avatarInitials = author.firstOrNull()?.uppercase() ?: "U",
                avatarColor = avatarPalette[i % avatarPalette.size],
                flagEmoji = obj.optString("countryFlag"),
                timestamp = obj.optString("createdAt").ifBlank { obj.optString("time") }.ifBlank { "Recent" }.take(16),
                content = text,
                likes = obj.optInt("likes", obj.optInt("likeCount", 0)),
                repliesCount = obj.optInt("replies", obj.optInt("replyCount", 0))
            )
        }
        return out
    }

    fun parseFilterOptions(payload: JSONObject): FilterOptions {
        return FilterOptions(
            genres = parseStringList(payload, listOf("genres", "genre", "categories")),
            years = parseStringList(payload, listOf("years", "year")),
            countries = parseStringList(payload, listOf("countries", "country", "regions")),
            sorts = parseStringList(payload, listOf("sorts", "sort", "sortBy"))
        )
    }

    fun parseQuotaInfo(payload: JSONObject): ApiQuotaInfo {
        return ApiQuotaInfo(
            clientName = payload.optString("name").ifBlank { payload.optString("client") }.ifBlank { payload.optString("n") },
            plan = payload.optString("plan").ifBlank { payload.optString("p") }.ifBlank { "UNLIMITED" },
            quota = payload.optString("quota").ifBlank { payload.optString("remaining") }.ifBlank { "UNLIMITED" },
            expiresAt = payload.optString("expires").ifBlank { payload.optString("expiresAt") }.take(10)
        )
    }

    private fun parseSubtitles(payload: JSONObject): List<SubtitleTrack> {
        val list = mutableListOf<SubtitleTrack>()
        val arr = payload.optJSONArray("captions")
            ?: payload.optJSONArray("subtitles")
            ?: payload.optJSONArray("subs")
            ?: return emptyList()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val url = obj.optString("url").ifBlank { obj.optString("src") }
            if (url.startsWith("http")) {
                val lan = obj.optString("lan").ifBlank { obj.optString("lang") }.ifBlank { "en" }
                val label = obj.optString("lanName")
                    .ifBlank { obj.optString("label") }
                    .ifBlank { obj.optString("language") }
                    .ifBlank { lan.uppercase() }
                list += SubtitleTrack(
                    id = obj.optString("id").ifBlank { "sub_$i" },
                    language = lan,
                    label = label,
                    url = url
                )
            }
        }
        return list
    }

    private fun extractHeaders(obj: JSONObject): Map<String, String> {
        val map = linkedMapOf<String, String>()
        val headersObj = obj.optJSONObject("headers")
        if (headersObj != null) {
            val keys = headersObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = headersObj.optString(k)
                if (v.isNotBlank()) map[k] = v
            }
        }
        val referer = obj.optString("referer").ifBlank { obj.optString("Referer") }
        if (referer.isNotBlank()) map["Referer"] = referer
        val origin = obj.optString("origin").ifBlank { obj.optString("Origin") }
        if (origin.isNotBlank()) map["Origin"] = origin
        return map
    }

    private fun extractImageUrl(obj: JSONObject, keys: List<String>): String? {
        for (key in keys) {
            val value = obj.opt(key) ?: continue
            when (value) {
                is String -> if (value.startsWith("http")) return value
                is JSONObject -> {
                    val nestedUrl = value.optString("url")
                        .ifBlank { value.optString("src") }
                        .ifBlank { value.optString("image") }
                    if (nestedUrl.startsWith("http")) return nestedUrl
                }
                is JSONArray -> {
                    val first = value.opt(0)
                    if (first is String && first.startsWith("http")) return first
                    if (first is JSONObject) {
                        val u = first.optString("url")
                        if (u.startsWith("http")) return u
                    }
                }
            }
        }
        return null
    }

    private fun extractLanguageBadge(obj: JSONObject, title: String): String? {
        val explicit = obj.optString("language")
            .ifBlank { obj.optString("lang") }
            .ifBlank { obj.optString("audio") }
        if (explicit.isNotBlank()) return explicit
        return when {
            title.contains("Hindi", ignoreCase = true) -> "Hindi"
            title.contains("Bengali", ignoreCase = true) || title.contains("Bangla", ignoreCase = true) -> "Bengali"
            title.contains("English", ignoreCase = true) -> "English"
            title.contains("Tamil", ignoreCase = true) -> "Tamil"
            title.contains("Korean", ignoreCase = true) -> "Korean"
            else -> null
        }
    }

    private fun classifySectionTag(sectionTitle: String): String {
        val lower = sectionTitle.lowercase()
        return when {
            "banner" in lower || "hero" in lower -> "hero"
            "movie" in lower || "cinema" in lower -> "trending_movies"
            "series" in lower || "tv" in lower || "drama" in lower -> "trending_series"
            "anime" in lower -> "anime"
            "short" in lower -> "hot_short"
            "gem" in lower || "pick" in lower -> "gems"
            else -> "most_trending"
        }
    }

    private fun isExcludedSportsCategory(name: String, path: String): Boolean {
        val combined = "$name $path".lowercase()
        return "sport" in combined ||
            "football" in combined ||
            "soccer" in combined ||
            "cricket" in combined ||
            "nba" in combined ||
            "basketball" in combined ||
            "ipl" in combined ||
            "originals" in combined
    }
}
