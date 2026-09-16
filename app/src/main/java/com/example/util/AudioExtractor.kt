package com.example.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object AudioExtractor {
    suspend fun extractAudio(
        context: Context,
        videoUri: Uri,
        videoPath: String,
        outputAudioPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            try {
                extractor.setDataSource(context, videoUri, null)
            } catch (e: Exception) {
                extractor.setDataSource(videoPath)
            }
            
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            
            if (audioTrackIndex < 0 || audioFormat == null) {
                Log.e("AudioExtractor", "No audio track found in $videoPath")
                return@withContext false
            }
            
            extractor.selectTrack(audioTrackIndex)
            
            val outFile = File(outputAudioPath)
            outFile.parentFile?.mkdirs()
            if (outFile.exists()) outFile.delete()
            
            // Mux into MPEG-4 container (standard for AAC audio streams)
            muxer = MediaMuxer(outputAudioPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerAudioTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()
            
            val maxChunkSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                1024 * 1024
            }
            val buffer = ByteBuffer.allocate(maxChunkSize)
            val bufferInfo = MediaCodec.BufferInfo()
            
            while (true) {
                val chunkSize = extractor.readSampleData(buffer, 0)
                if (chunkSize < 0) {
                    break
                }
                
                bufferInfo.size = chunkSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                var flags = 0
                if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                    flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                }
                bufferInfo.flags = flags
                
                muxer.writeSampleData(muxerAudioTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }
            
            true
        } catch (e: Exception) {
            Log.e("AudioExtractor", "Error extracting audio", e)
            try { File(outputAudioPath).delete() } catch (e2: Exception) {}
            false
        } finally {
            try { extractor.release() } catch (e: Exception) {}
            try { 
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {}
        }
    }
}
