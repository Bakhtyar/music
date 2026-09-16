package com.example.player

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

object PlayerManager {
    var exoPlayer: ExoPlayer? = null
    var equalizer: Equalizer? = null
    
    fun initPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context.applicationContext).build()
            
            exoPlayer?.addListener(object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    super.onAudioSessionIdChanged(audioSessionId)
                    setupEqualizer(audioSessionId)
                }
            })
        }
        return exoPlayer!!
    }

    private fun setupEqualizer(audioSessionId: Int) {
        try {
            if (equalizer != null) {
                equalizer?.release()
            }
            equalizer = Equalizer(0, audioSessionId)
            equalizer?.enabled = false // Default disabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
