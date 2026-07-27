package com.pompom.pompom

import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import es.antonborri.home_widget.HomeWidgetLaunchIntent

/// Quick Settings tile: a single tap opens PomPom and starts a focus
/// session immediately, mirroring the widget's idle "Start" button.
class PomPomQuickTileService : TileService() {

  override fun onStartListening() {
    super.onStartListening()
    qsTile?.apply {
      label = "Start PomPom"
      icon = Icon.createWithResource(this@PomPomQuickTileService, R.mipmap.ic_launcher)
      state = Tile.STATE_INACTIVE
      updateTile()
    }
  }

  override fun onClick() {
    super.onClick()
    val uri = Uri.parse("pompom://start")

    if (Build.VERSION.SDK_INT >= 34) {
      val pendingIntent = HomeWidgetLaunchIntent.getActivity(this, MainActivity::class.java, uri)
      startActivityAndCollapse(pendingIntent)
    } else {
      @Suppress("DEPRECATION")
      val intent = android.content.Intent(this, MainActivity::class.java).apply {
        data = uri
        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
      }
      @Suppress("DEPRECATION") startActivityAndCollapse(intent)
    }
  }
}
