package org.fmdx.app.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.fmdx.app.R
import org.fmdx.app.data.FmDxRepository
import org.fmdx.app.model.TunerInfo
import org.fmdx.app.model.TunerState
import java.util.Locale

data class NowPlayingFields(
    val title: String,
    val artist: String?,
    val subtitle: String?,
    val description: String?,
    val albumTitle: String?,
    val station: String?,
    val artworkUri: Uri?
)

fun nowPlayingFieldsFor(
    context: Context,
    tunerInfo: TunerInfo?,
    tunerState: TunerState?,
    logoUrl: String?
): NowPlayingFields = nowPlayingFieldsFor(
    appName = context.getString(R.string.app_name),
    tunerInfo = tunerInfo,
    tunerState = tunerState,
    logoUrl = logoUrl
)

fun nowPlayingFieldsFor(
    appName: String,
    tunerInfo: TunerInfo?,
    tunerState: TunerState?,
    logoUrl: String?
): NowPlayingFields {
    val ps = tunerState?.ps?.trim()?.takeIf { it.isNotBlank() }
    val rt0 = tunerState?.rt0?.trim()?.takeIf { it.isNotBlank() }
    val rt1 = tunerState?.rt1?.trim()?.takeIf { it.isNotBlank() }
    val freqText = tunerState?.freqMHz?.let {
        String.format(Locale.US, "%.1f MHz", it)
    }
    val tunerName = tunerInfo?.tunerName?.trim()?.takeIf { it.isNotBlank() } ?: appName
    val artwork = logoUrl
        ?.takeIf { it.isNotBlank() && it != FmDxRepository.DEFAULT_LOGO_URL }
        ?.let(Uri::parse)

    val artistLine = listOfNotNull(freqText, rt0).joinToString(" · ")
        .takeIf { it.isNotBlank() }

    return NowPlayingFields(
        title = ps ?: freqText ?: appName,
        artist = artistLine,
        subtitle = rt0,
        description = rt1,
        albumTitle = tunerName,
        station = ps ?: tunerName,
        artworkUri = artwork
    )
}

fun mediaMetadataFor(fields: NowPlayingFields): MediaMetadata =
    MediaMetadata.Builder()
        .setTitle(fields.title)
        .setDisplayTitle(fields.title)
        .setArtist(fields.artist)
        .setSubtitle(fields.subtitle)
        .setDescription(fields.description)
        .setAlbumTitle(fields.albumTitle)
        .setStation(fields.station)
        .setArtworkUri(fields.artworkUri)
        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .build()

fun buildMediaItemForServer(
    context: Context,
    serverUrl: String,
    tunerInfo: TunerInfo?,
    tunerState: TunerState?,
    logoUrl: String?
): MediaItem {
    val fields = nowPlayingFieldsFor(context, tunerInfo, tunerState, logoUrl)
    return MediaItem.Builder()
        .setMediaId(serverUrl)
        .setMediaMetadata(mediaMetadataFor(fields))
        .build()
}

fun buildPlayableServerItem(
    serverUrl: String,
    title: String,
    subtitle: String? = null,
    description: String? = null,
    artworkUri: Uri? = null
): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setDisplayTitle(title)
        .setSubtitle(subtitle)
        .setDescription(description)
        .setArtworkUri(artworkUri)
        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .build()
    return MediaItem.Builder()
        .setMediaId(serverUrl)
        .setMediaMetadata(metadata)
        .build()
}

fun buildBrowsableFolderItem(
    mediaId: String,
    title: String,
    subtitle: String? = null,
    mediaType: Int = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setDisplayTitle(title)
        .setSubtitle(subtitle)
        .setMediaType(mediaType)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .build()
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setMediaMetadata(metadata)
        .build()
}
