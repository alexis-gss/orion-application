package com.orion.app.cinema.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class SearchResponse(
    val page: Int = 1,
    val results: List<SearchResult> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1
)

@Serializable
data class SearchResult(
    val id: Int,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    val overview: String? = null
) {
    // Neutral fallback (no translation here: this data model has no access to
    // resources / Context). In practice, TMDB always returns at least one of the two.
    val displayTitle: String get() = title ?: name ?: "—"
    val year: String? get() = (releaseDate ?: firstAirDate)?.take(4)
    val resolvedMediaType: String get() = mediaType ?: if (title != null) "movie" else "tv"
}

// ---------------- NEW CREDITS, VIDEOS, PROVIDERS & RECOMMENDATIONS MODELS ----------------

@Serializable
data class CreditsResponse(
    val cast: List<CastMember> = emptyList(),
    val crew: List<CrewMember> = emptyList()
)

@Serializable
data class CastMember(
    val id: Int,
    val name: String,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
data class VideosResponse(
    val results: List<VideoItem> = emptyList()
)

@Serializable
data class VideoItem(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val type: String
)

@Serializable
data class WatchProvidersResponse(
    val results: Map<String, WatchProviderRegion>? = null
)

@Serializable
data class WatchProviderRegion(
    val link: String? = null,
    val flatrate: List<WatchProviderItem> = emptyList(),
    val rent: List<WatchProviderItem> = emptyList(),
    val buy: List<WatchProviderItem> = emptyList()
)

@Serializable
data class WatchProviderItem(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String,
    @SerialName("logo_path") val logoPath: String? = null
)

@Serializable
data class Network(
    val id: Int,
    val name: String,
    @SerialName("logo_path") val logoPath: String? = null
)

// ---------------- EXTENDED FOR MOVIE AND SHOW DETAILS ----------------

@Serializable
data class MovieDetail(
    val id: Int,
    val title: String,
    val overview: String? = null,
    val tagline: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: List<Genre> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val budget: Long? = null,
    val revenue: Long? = null,
    val credits: CreditsResponse? = null,
    val videos: VideosResponse? = null,
    @SerialName("watch/providers") val watchProviders: WatchProvidersResponse? = null,
    val recommendations: SearchResponse? = null
)

@Serializable
data class TvDetail(
    val id: Int,
    val name: String,
    val overview: String? = null,
    val tagline: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val genres: List<Genre> = emptyList(),
    val seasons: List<SeasonSummary> = emptyList(),
    @SerialName("next_episode_to_air") val nextEpisodeToAir: EpisodeInfo? = null,
    @SerialName("last_episode_to_air") val lastEpisodeToAir: EpisodeInfo? = null,
    val status: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    val networks: List<Network> = emptyList(),
    val credits: CreditsResponse? = null,
    val videos: VideosResponse? = null,
    @SerialName("watch/providers") val watchProviders: WatchProvidersResponse? = null,
    val recommendations: SearchResponse? = null
)

@Serializable
data class Genre(val id: Int, val name: String)

@Serializable
data class SeasonSummary(
    @SerialName("season_number") val seasonNumber: Int,
    val name: String,
    @SerialName("episode_count") val episodeCount: Int,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("air_date") val airDate: String? = null
)

@Serializable
data class EpisodeInfo(
    val id: Int,
    val name: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("episode_number") val episodeNumber: Int? = null,
    @SerialName("season_number") val seasonNumber: Int? = null,
    val runtime: Int? = null   // duration in minutes, provided by TMDB on the season detail
)

@Serializable
data class SeasonDetail(
    @SerialName("season_number") val seasonNumber: Int,
    val episodes: List<EpisodeInfo> = emptyList()
)

@Serializable
data class ConfigurationResponse(val images: ImagesConfig)

@Serializable
data class ImagesConfig(
    @SerialName("secure_base_url") val secureBaseUrl: String,
    @SerialName("poster_sizes") val posterSizes: List<String>
)

@Serializable
data class ImagesResponse(
    val backdrops: List<TmdbImage> = emptyList(),
    val posters: List<TmdbImage> = emptyList()
)

@Serializable
data class CrewMember(
    val id: Int,
    val name: String,
    val job: String,
    val department: String? = null,
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
data class TmdbImage(
    @SerialName("file_path") val filePath: String,
    val width: Int? = null,
    val height: Int? = null
)

interface TmdbApi {

    @GET("configuration")
    suspend fun getConfiguration(): ConfigurationResponse

    @GET("configuration")
    suspend fun testApiKeyDirect(@Query("api_key") key: String): ConfigurationResponse

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("trending/all/{time_window}")
    suspend fun getTrending(
        @Path("time_window") timeWindow: String = "day",
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): SearchResponse

    // append_to_response is used to fetch every needed piece of info in a single HTTP call!
    @GET("movie/{id}")
    suspend fun getMovieDetail(
        @Path("id") id: Int,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = "credits,videos,watch/providers,recommendations"
    ): MovieDetail

    @GET("tv/{id}")
    suspend fun getTvDetail(
        @Path("id") id: Int,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = "credits,videos,watch/providers,recommendations"
    ): TvDetail

    @GET("tv/{id}/season/{season_number}")
    suspend fun getSeasonDetail(
        @Path("id") id: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = "en-US"
    ): SeasonDetail

    @GET("movie/{id}/images")
    suspend fun getMovieImages(
        @Path("id") id: Int,
        @Query("include_image_language") includeImageLanguage: String = "en,null"
    ): ImagesResponse

    @GET("tv/{id}/images")
    suspend fun getTvImages(
        @Path("id") id: Int,
        @Query("include_image_language") includeImageLanguage: String = "en,null"
    ): ImagesResponse

    @GET("tv/{id}/season/{season_number}/images")
    suspend fun getSeasonImages(
        @Path("id") id: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("include_image_language") includeImageLanguage: String = "en,null"
    ): ImagesResponse
}