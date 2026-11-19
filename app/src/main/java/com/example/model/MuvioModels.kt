package com.example.model

import androidx.annotation.DrawableRes

enum class MainTab(val route: String) {
    HOME("home"),
    NOVEL_HUB("novel_hub"),
    SHORTS("shorts"),
    DOWNLOADS("downloads"),
    ME("me")
}

enum class MediaType {
    MOVIE,
    SERIES,
    ANIME,
    DRAMA,
    SHORT_TV
}

data class PosterArtwork(
    val remoteUrl: String? = null,
    @DrawableRes val fallbackRes: Int,
    val tintHex: Long,
    val accentHex: Long,
    val badgeTitle: String = ""
)

enum class ContentSource {
    PRIMARY,
    SECONDARY
}

data class EpisodeItem(
    val number: Int,
    val title: String,
    val duration: String,
    val streamUrl: String = "",
    val sources: List<StreamResource> = emptyList()
)

data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
    val url: String
)

data class DubVersion(
    val title: String,
    val language: String,
    val subjectId: String = ""
)

data class StaffMember(
    val id: String,
    val name: String,
    val role: String = "Cast",
    val avatarUrl: String? = null
)

data class StreamResource(
    val id: String,
    val label: String,
    val language: String,
    val quality: String,
    val uploader: String,
    val streamUrl: String = "",
    val subtitles: List<SubtitleTrack> = emptyList(),
    val headers: Map<String, String> = emptyMap()
)

data class MediaItem(
    val id: String,
    val title: String,
    val year: Int,
    val country: String,
    val genre: String,
    val rating: Double,
    val languageBadge: String?,
    val rankNumber: Int? = null,
    val mediaType: MediaType,
    val subCategory: String,
    val sectionTags: List<String>,
    val synopsis: String,
    val duration: String,
    val poster: PosterArtwork,
    val backdropRes: Int,
    val backdropUrl: String? = null,
    val episodes: List<EpisodeItem> = emptyList(),
    val seasonEpisodes: Map<Int, List<EpisodeItem>> = emptyMap(),
    val resources: List<StreamResource> = emptyList(),
    val dubs: List<DubVersion> = emptyList(),
    val cast: List<StaffMember> = emptyList(),
    val releaseDateText: String = "",
    val bookedCount: String = "",
    val contentSource: ContentSource = ContentSource.PRIMARY,
    val sourceDetailPath: String = ""
)

data class LiveMatchItem(
    val id: String,
    val league: String,
    val homeTeam: String,
    val homeShort: String,
    val homeBadgeColor: Long,
    val awayTeam: String,
    val awayShort: String,
    val awayBadgeColor: Long,
    val scoreOrTime: String,
    val subTime: String = "",
    val isLive: Boolean,
    val isNotified: Boolean = false,
    val streamUrl: String = "",
    val homeLogoUrl: String? = null,
    val awayLogoUrl: String? = null,
    val sportType: String = "Sports",
    val headers: Map<String, String> = emptyMap()
)

data class CategoryBannerItem(
    val id: String,
    val label: String,
    val filterKey: String,
    val artworkRes: Int,
    val accentHex: Long,
    val apiPath: String = ""
)

data class ApiCategory(
    val id: String,
    val name: String,
    val path: String
)

data class PlatformBrand(
    val apiName: String,
    val displayName: String,
    val accentHex: Long
)

data class FilterOptions(
    val genres: List<String> = emptyList(),
    val years: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val sorts: List<String> = emptyList()
)

data class ApiQuotaInfo(
    val clientName: String = "",
    val plan: String = "",
    val quota: String = "",
    val expiresAt: String = ""
)

data class ShortReelItem(
    val id: String,
    val linkedMediaId: String,
    val title: String,
    val episodeTag: String,
    val description: String,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val poster: PosterArtwork,
    val streamUrl: String = ""
)

data class NovelItem(
    val id: String,
    val title: String,
    val author: String,
    val genre: String,
    val chaptersCount: Int,
    val rating: Double,
    val isHot: Boolean,
    val synopsis: String,
    val sampleChapter: String,
    val poster: PosterArtwork
)

data class CommentItem(
    val id: String,
    val author: String,
    val avatarInitials: String,
    val avatarColor: Long,
    val flagEmoji: String,
    val timestamp: String,
    val content: String,
    val likes: Int,
    val isLiked: Boolean = false,
    val repliesCount: Int = 0,
    val hasDownloadPromo: Boolean = false
)

enum class DownloadStatus {
    DOWNLOADING,
    PAUSED,
    COMPLETED
}

data class DownloadItem(
    val mediaId: String,
    val title: String,
    val quality: String,
    val sizeText: String,
    val progress: Float,
    val status: DownloadStatus,
    val poster: PosterArtwork
)
