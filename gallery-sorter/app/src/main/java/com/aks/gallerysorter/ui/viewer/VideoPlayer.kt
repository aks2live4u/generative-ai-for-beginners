package com.aks.gallerysorter.ui.viewer

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.VaultRow
import com.aks.gallerysorter.vault.PrivateHandle
import com.aks.gallerysorter.vault.VaultStore

/** A Media3 ExoPlayer for one page; paused when the page is not [active], released when it leaves. */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(key: Any, active: Boolean, source: (Context) -> MediaSource) {
    val context = LocalContext.current
    val player = remember(key) {
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(source(context))
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(player, active) { if (!active) player.pause() }
    AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxSize())
}

@OptIn(UnstableApi::class)
fun galleryVideo(row: MediaRow): (Context) -> MediaSource = { context ->
    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, row.id)
    ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context)).createMediaSource(MediaItem.fromUri(uri))
}

/** Streams a vault video through [VaultDataSource]: decrypted segment by segment, nothing on disk. */
@OptIn(UnstableApi::class)
fun vaultVideo(priv: PrivateHandle, row: VaultRow, store: VaultStore): (Context) -> MediaSource = {
    ProgressiveMediaSource.Factory(DataSource.Factory { VaultDataSource(priv, row, store) })
        .createMediaSource(MediaItem.fromUri("vault://${row.id}"))
}
