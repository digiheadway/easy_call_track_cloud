package com.miniclick.calltrackmanage.util.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

/**
 * AudioCompressor - Compresses audio recordings to AAC format to reduce file size
 * 
 * Target: 32kbps, 16000Hz, Mono
 * Expected compression: ~80% reduction for typical call recordings
 * 
 * Based on Callyzer Pro's approach of compressing before upload.
 */
object AudioCompressor {

    private const val TAG = "AudioCompressor"
    
    // Target compression settings
    private const val TARGET_BITRATE = 32000 // 32kbps
    private const val TARGET_SAMPLE_RATE = 16000 // 16kHz
    private const val TARGET_CHANNEL_COUNT = 1 // Mono
    private const val TIMEOUT_US = 10000L

    /**
     * Compress an audio file to AAC format
     * 
     * @param context App context
     * @param inputPath Path to original audio file (can be content:// or file://)
     * @param outputFile Output file for compressed audio
     * @return true if compression succeeded, false otherwise
     */
    fun compress(context: Context, inputPath: String, outputFile: File): Boolean {
        Log.d(TAG, "Starting compression: $inputPath -> ${outputFile.absolutePath}")
        
        return try {
            // Try MediaCodec approach first
            compressWithMediaCodec(context, inputPath, outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Compression failed", e)
            // If compression fails, just copy the original
            false
        }
    }

    /**
     * Get estimated compressed size (rough approximation)
     */
    fun estimateCompressedSize(originalDurationMs: Long): Long {
        // 32kbps = 4000 bytes per second = 4 bytes per ms
        return (originalDurationMs * 4).coerceAtLeast(1024) // Min 1KB
    }

    /**
     * Check if compression would be beneficial
     * Only compress files larger than 100KB and expected to reduce by at least 50%
     */
    fun shouldCompress(originalSizeBytes: Long, durationMs: Long): Boolean {
        if (originalSizeBytes < 100 * 1024) return false // Too small
        val estimatedCompressedSize = estimateCompressedSize(durationMs)
        return estimatedCompressedSize < originalSizeBytes * 0.5
    }

    private fun compressWithMediaCodec(context: Context, inputPath: String, outputFile: File): Boolean {
        var extractor: MediaExtractor? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        
        try {
            extractor = MediaExtractor()
            
            // Set data source
            if (inputPath.startsWith("content://")) {
                extractor.setDataSource(context, Uri.parse(inputPath), null)
            } else {
                extractor.setDataSource(inputPath)
            }

            // Find audio track
            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    inputFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || inputFormat == null) {
                Log.e(TAG, "No audio track found")
                return false
            }

            extractor.selectTrack(audioTrackIndex)

            // Get input properties
            val inputSampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val inputChannelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            Log.d(TAG, "Input: ${inputSampleRate}Hz, $inputChannelCount channels")

            // Create encoder format
            val outputFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                TARGET_SAMPLE_RATE,
                TARGET_CHANNEL_COUNT
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, TARGET_BITRATE)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            // Create encoder
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            // Create muxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerTrackIndex = -1
            var muxerStarted = false

            val inputBufferInfo = MediaCodec.BufferInfo()
            var isInputEOS = false
            var isOutputEOS = false

            while (!isOutputEOS) {
                // Feed input
                if (!isInputEOS) {
                    val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        
                        if (sampleSize < 0) {
                            encoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isInputEOS = true
                        } else {
                            val sampleTime = extractor.sampleTime
                            encoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // Get output
                val outputBufferIndex = encoder.dequeueOutputBuffer(inputBufferInfo, TIMEOUT_US)
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            val newFormat = encoder.outputFormat
                            muxerTrackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            muxerStarted = true
                            Log.d(TAG, "Muxer started")
                        }
                    }
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)!!
                        
                        if (inputBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            inputBufferInfo.size = 0
                        }

                        if (inputBufferInfo.size > 0 && muxerStarted) {
                            outputBuffer.position(inputBufferInfo.offset)
                            outputBuffer.limit(inputBufferInfo.offset + inputBufferInfo.size)
                            muxer.writeSampleData(muxerTrackIndex, outputBuffer, inputBufferInfo)
                        }

                        encoder.releaseOutputBuffer(outputBufferIndex, false)

                        if (inputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            isOutputEOS = true
                        }
                    }
                }
            }

            Log.d(TAG, "Compression complete: ${outputFile.length()} bytes")
            return outputFile.exists() && outputFile.length() > 0

        } finally {
            try {
                extractor?.release()
            } catch (e: Exception) { }
            try {
                encoder?.stop()
                encoder?.release()
            } catch (e: Exception) { }
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) { }
        }
    }

    /**
     * Simple fallback: Just copy the file without compression
     * Used when MediaCodec fails (e.g., unsupported input format)
     */
    fun copyFile(context: Context, inputPath: String, outputFile: File): Boolean {
        return try {
            val inputStream = if (inputPath.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(inputPath))
            } else {
                FileInputStream(inputPath)
            }
            
            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            outputFile.exists() && outputFile.length() > 0
        } catch (e: Exception) {
            Log.e(TAG, "Copy failed", e)
            false
        }
    }
}
