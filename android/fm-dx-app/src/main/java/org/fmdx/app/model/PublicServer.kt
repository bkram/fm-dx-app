package org.fmdx.app.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class PublicServer(
    val name: String,
    val description: String?,
    val contact: String?,
    val tuner: String?,
    val version: String?,
    val bandwidthLimit: String?,
    val latitude: Double?,
    val longitude: Double?,
    val url: String,
    val countryCode: String?,
    val statusCode: Int,
    val audioQuality: String?,
    val audioChannels: Int?,
    val countryName: String?,
    val city: String?,
    val os: String?
) {
    val displayLocation: String? = displayLocation()

    val isOnline: Boolean = statusCode == STATUS_ONLINE

    fun matchesQuery(rawQuery: String): Boolean {
        val query = rawQuery.trim().lowercase(Locale.ROOT)
        if (query.isEmpty()) return true
        return listOfNotNull(
            name,
            description,
            contact,
            tuner,
            version,
            bandwidthLimit,
            url,
            countryCode,
            audioQuality,
            countryName,
            city,
            os
        ).any { candidate ->
            candidate.lowercase(Locale.ROOT).contains(query)
        }
    }

    companion object {
        const val STATUS_ONLINE = 1

        fun fromJson(json: JSONObject): PublicServer? {
            val url = cleanUrl(json.optString("url"))
                .takeIf { it.isNotBlank() }
                ?: return null
            val coords = json.optJSONArray("coords")
            val (lat, lon) = parseCoords(coords)
            val name = json.optString("name").ifBlank { url }
            val description = json.optString("desc").takeIf { it.isNotBlank() }
            val contact = json.optString("contact").takeIf { it.isNotBlank() }
            val tuner = json.optString("tuner").takeIf { it.isNotBlank() }
            val version = json.optString("version").takeIf { it.isNotBlank() }
            val bwLimit = json.optString("bwLimit").takeIf { it.isNotBlank() }
            val statusCode = json.optInt("status", 0)
            val audioQuality = json.optString("audioQuality").takeIf { it.isNotBlank() }
            val audioChannels = json.optInt("audioChannels").takeIf { it != 0 }
            val countryName = json.optString("countryName").takeIf { it.isNotBlank() }
            val city = json.optString("city").takeIf { it.isNotBlank() }
            val countryCode = json.optString("country").takeIf { it.isNotBlank() }
            val os = json.optString("os").takeIf { it.isNotBlank() }
            return PublicServer(
                name = name,
                description = description,
                contact = contact,
                tuner = tuner,
                version = version,
                bandwidthLimit = bwLimit,
                latitude = lat,
                longitude = lon,
                url = url,
                countryCode = countryCode,
                statusCode = statusCode,
                audioQuality = audioQuality,
                audioChannels = audioChannels,
                countryName = countryName,
                city = city,
                os = os
            )
        }

        private fun parseCoords(array: JSONArray?): Pair<Double?, Double?> {
            if (array == null || array.length() < 2) {
                return null to null
            }
            val lat = array.optString(0)?.toDoubleOrNull()
            val lon = array.optString(1)?.toDoubleOrNull()
            return lat to lon
        }

        fun sample(): PublicServer = PublicServer(
            name = "Sample DX Server",
            description = "5 element Yagi aimed NW, 256k AAC audio.",
            contact = "ops@fmdx.org",
            tuner = "TEF6687",
            version = "1.3.11",
            bandwidthLimit = "64 - 108 MHz",
            latitude = 47.5,
            longitude = 19.05,
            url = "https://sample.server.example/",
            countryCode = "hu",
            statusCode = STATUS_ONLINE,
            audioQuality = "256k",
            audioChannels = 2,
            countryName = "Hungary",
            city = "Budapest",
            os = "Linux"
        )
    }

    private fun displayLocation(): String? {
        val normalizedCity = city.normalized()
        val normalizedCountryName = countryName.normalized()
        val resolvedCountry = normalizedCountryName
            ?: countryCode
                ?.normalized()
                ?.let { Locale("", it).displayCountry.takeIf { name -> name.isNotBlank() } }
            ?: countryCode
                ?.normalized()
                ?.uppercase(Locale.ROOT)

        return when {
            normalizedCity != null && resolvedCountry != null -> "$normalizedCity, $resolvedCountry"
            normalizedCity != null -> normalizedCity
            resolvedCountry != null -> resolvedCountry
            else -> null
        }
    }
}

private fun cleanUrl(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val trimmed = raw.trim()
    val fragmentIndex = trimmed.indexOf('#')
    if (fragmentIndex == -1) return trimmed
    val before = trimmed.substring(0, fragmentIndex)
    val after = trimmed.substring(fragmentIndex + 1)
    return if (before.endsWith("/")) {
        before + after
    } else {
        before + "/" + after
    }
}

private fun String?.normalized(): String? {
    val trimmed = this?.trim()
    if (trimmed.isNullOrEmpty()) return null
    if (trimmed.equals("null", ignoreCase = true)) return null
    return trimmed
}
