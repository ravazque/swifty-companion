package com.ravazque.swiftycompanion.data.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Mirrors of the API JSON. Only the fields the app reads; everything optional has a default.

internal val IntraJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

@Serializable
data class UserDto(
    val id: Long,
    val login: String,
    val email: String? = null,
    val phone: String? = null,
    val displayname: String? = null,
    @SerialName("usual_full_name") val usualFullName: String? = null,
    val image: ImageDto? = null,
    val location: String? = null,
    val wallet: Int = 0,
    @SerialName("correction_point") val correctionPoint: Int = 0,
    @SerialName("pool_month") val poolMonth: String? = null,
    @SerialName("pool_year") val poolYear: String? = null,
    @SerialName("staff?") val staff: Boolean = false,
    @SerialName("cursus_users") val cursusUsers: List<CursusUserDto> = emptyList(),
    @SerialName("projects_users") val projectsUsers: List<ProjectUserDto> = emptyList(),
    val campus: List<CampusDto> = emptyList(),
    @SerialName("campus_users") val campusUsers: List<CampusUserDto> = emptyList(),
    val titles: List<TitleDto> = emptyList(),
    @SerialName("titles_users") val titlesUsers: List<TitleUserDto> = emptyList(),
)

@Serializable
data class ImageDto(val link: String? = null, val versions: ImageVersionsDto? = null)

@Serializable
data class ImageVersionsDto(val medium: String? = null)

@Serializable
data class CursusUserDto(
    val level: Double = 0.0,
    val grade: String? = null,
    @SerialName("begin_at") val beginAt: String? = null,
    val cursus: CursusDto,
    val skills: List<SkillDto> = emptyList(),
)

@Serializable
data class CursusDto(val id: Int, val name: String, val slug: String)

@Serializable
data class SkillDto(val name: String, val level: Double = 0.0)

@Serializable
data class ProjectUserDto(
    val status: String? = null,
    @SerialName("validated?") val validated: Boolean? = null,
    @SerialName("final_mark") val finalMark: Int? = null,
    @SerialName("marked_at") val markedAt: String? = null,
    @SerialName("cursus_ids") val cursusIds: List<Int> = emptyList(),
    val project: ProjectDto,
)

@Serializable
data class ProjectDto(val name: String)

@Serializable
data class CampusDto(val id: Int, val name: String)

@Serializable
data class CampusUserDto(
    @SerialName("campus_id") val campusId: Int,
    @SerialName("is_primary") val isPrimary: Boolean = false,
)

@Serializable
data class TitleDto(val id: Int, val name: String)

@Serializable
data class TitleUserDto(@SerialName("title_id") val titleId: Int, val selected: Boolean = false)

@Serializable
data class CoalitionDto(
    val name: String,
    val color: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
)
