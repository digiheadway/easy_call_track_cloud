package com.miniclick.calltrackmanage.util

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.nio.ByteBuffer

/**
 * AudioCompressor - Compresses audio files using Android's built-in MediaCodec.
 * 
 * This approach:
 * - Works on Android 5.0+ (API 21+) - no external dependencies
 * - Converts to AAC format at lower bitrate
 * - Typically achieves 50-70% file size reduction
 * - Preserves audio quality for voice calls
 * - Has timeout protection to prevent worker timeouts
 * - Falls back to original file on any error
 */
object AudioCompressor {

    private const val TAG = "AudioCompressor"
    
    // Target settings for voice call compression
    private const val TARGET_BITRATE = 48000 // 48 kbps - excellent for voice, smaller files
    private const val TARGET_SAMPLE_RATE = 16000 // 16 kHz - sufficient for voice calls
    private const val TARGET_CHANNELS = 1 // Mono - typical for call recordings
    
    // Timeout for compression (in milliseconds) - prevents worker timeouts
    private const val COMPRESSION_TIMEOUT_MS = 60_000L // 1 minute max
    
    // Minimum file size to attempt compression (files smaller than this won't benefit much)
    private const val MIN_SIZE_FOR_COMPRESSION = 50 * 1024L // 50 KB
    
    // Maximum file size to attempt compression (very large files may timeout)
    private const val MAX_SIZE_FOR_COMPRESSION = 100 * 1024 * 1024L // 100 MB
    
    /**
     * Result of compression operation
     */
    enum class CompressionResult {
        SUCCESS,           // Compressed successfully
        SKIPPED_TOO_SMALL, // File too small, not worth compressing
        SKIPPED_TOO_LARGE, // File too large, risk of timeout
        SKIPPED_ALREADY_COMPRESSED, // Already compressed at acceptable bitrate
        FAILED_TIMEOUT,    // Compression timed out
        FAILED_ERROR,      // Compression failed with error
        COPIED_FALLBACK    // Failed but copied original as fallback
    }
    
    data class CompressionStats(
        val result: CompressionResult,
        val originalSize: Long,
        val finalSize: Long,
        val savingsPercent: Int,
        val durationMs: Long
    )
    
    /**
     * Compresses an audio file to a lower bitrate AAC/M4A.
     * 
     * @param inputFile The original recording file
     * @param outputFile The destination for compressed file
     * @return CompressionStats with result and statistics
     */
    suspend fun compressAudioWithStats(inputFile: File, outputFile: File): CompressionStats = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        if (!inputFile.exists()) {
            Log.w(TAG, "Input file does not exist: ${inputFile.absolutePath}")
            return@withContext CompressionStats(
                CompressionResult.FAILED_ERROR, 0, 0, 0, 0
            )
        }

        val originalSize = inputFile.length()
        Log.d(TAG, "Starting compression: ${inputFile.name} (${originalSize / 1024} KB)")

        // Skip compression for very small files
        if (originalSize < MIN_SIZE_FOR_COMPRESSION) {
            Log.d(TAG, "File too small for compression (${originalSize / 1024} KB), copying directly")
            inputFile.copyTo(outputFile, overwrite = true)
            return@withContext CompressionStats(
                CompressionResult.SKIPPED_TOO_SMALL, originalSize, originalSize, 0,
                System.currentTimeMillis() - startTime
            )
        }
        
        // Skip compression for very large files (risk of timeout)
        if (originalSize > MAX_SIZE_FOR_COMPRESSION) {
            Log.w(TAG, "File too large for compression (${originalSize / (1024*1024)} MB), copying directly")
            inputFile.copyTo(outputFile, overwrite = true)
            return@withContext CompressionStats(
                CompressionResult.SKIPPED_TOO_LARGE, originalSize, originalSize, 0,
                System.currentTimeMillis() - startTime
            )
        }

        try {
            // Run compression with timeout protection
            val success = withTimeoutOrNull(COMPRESSION_TIMEOUT_MS) {
                compressWithMediaCodecSafe(inputFile, outputFile)
            }
            
            when {
                success == null -> {
                    // Timeout occurred
                    Log.w(TAG, "Compression timed out after ${COMPRESSION_TIMEOUT_MS/1000}s, using original")
                    outputFile.delete()
                    inputFile.copyTo(outputFile, overwrite = true)
                    return@withContext CompressionStats(
                        CompressionResult.FAILED_TIMEOUT, originalSize, originalSize, 0,
                        System.currentTimeMillis() - startTime
                    )
                }
                success == true && outputFile.exists() -> {
                    val compressedSize = outputFile.length()
                    val savings = ((originalSize - compressedSize).toFloat() / originalSize * 100).toInt()
                    Log.d(TAG, "Compression complete: ${compressedSize / 1024} KB (saved $savings%)")
                    
                    // Only use compressed if it's actually smaller by at least 10%
                    if (compressedSize >= originalSize * 0.9) {
                        Log.w(TAG, "Compressed file not significantly smaller ($savings%), using original")
                        outputFile.delete()
                        inputFile.copyTo(outputFile, overwrite = true)
                        return@withContext CompressionStats(
                            CompressionResult.SKIPPED_ALREADY_COMPRESSED, originalSize, originalSize, 0,
                            System.currentTimeMillis() - startTime
                        )
                    }
                    
                    return@withContext CompressionStats(
                        CompressionResult.SUCCESS, originalSize, compressedSize, savings,
                        System.currentTimeMillis() - startTime
                    )
                }
                else -> {
                    Log.w(TAG, "Compression failed, falling back to copy")
                    inputFile.copyTo(outputFile, overwrite = true)
                    return@withContext CompressionStats(
                        CompressionResult.COPIED_FALLBACK, originalSize, originalSize, 0,
                        System.currentTimeMillis() - startTime
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Compression error, falling back to copy", e)
            try {
                inputFile.copyTo(outputFile, overwrite = true)
                return@withContext CompressionStats(
                    CompressionResult.COPIED_FALLBACK, originalSize, originalSize, 0,
                    System.currentTimeMillis() - startTime
                )
            } catch (copyError: Exception) {
                Log.e(TAG, "Copy also failed", copyError)
                return@withContext CompressionStats(
                    CompressionResult.FAILED_ERROR, originalSize, 0, 0,
                    System.currentTimeMillis() - startTime
                )
            }
        }
    }
    
    /**
     * Simple wrapper for backward compatibility
     */
    suspend fun compressAudio(inputFile: File, outputFile: File): Boolean {
        val stats = compressAudioWithStats(inputFile, outputFile)
        return stats.result != CompressionResult.FAILED_ERROR
    }
    
    /**
     * Safe MediaCodec compression with better error handling
     */
    private fun compressWithMediaCodecSafe(inputFile: File, outputFile: File): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        var encoder: MediaCodec? = null
        var decoder: MediaCodec? = null
        var muxerStarted = false
        
        try {
            // Set up extractor to read input file
            extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)
            
            // Find audio track
            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null
            
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    inputFormat = format
                    break
                }
            }
            
            if (audioTrackIndex == -1 || inputFormat == null) {
                Log.w(TAG, "No audio track found in input file")
                return false
            }
            
            extractor.selectTrack(audioTrackIndex)
            
            val inputMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: "audio/raw"
            val inputSampleRate = try {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } catch (e: Exception) {
                44100 // Default
            }
            val inputChannels = try {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } catch (e: Exception) {
                1 // Default mono
            }
            
            Log.d(TAG, "Input: $inputMime, ${inputSampleRate}Hz, $inputChannels ch")
            
            // If already a compressed format at low bitrate, just copy
            if (inputMime == "audio/mp4a-latm" || inputMime == "audio/aac") {
                val bitrate = try {
                    inputFormat.getInteger(MediaFormat.KEY_BIT_RATE)
                } catch (e: Exception) {
                    128000 // Default assumption
                }
                
                if (bitrate <= TARGET_BITRATE * 1.3) {
                    Log.d(TAG, "Already compressed at acceptable bitrate ($bitrate bps), copying")
                    inputFile.copyTo(outputFile, overwrite = true)
                    return true
                }
            }
            
            // Calculate output parameters (keep original if lower than target)
            val outputSampleRate = minOf(inputSampleRate, TARGET_SAMPLE_RATE)
            val outputChannels = minOf(inputChannels, TARGET_CHANNELS)
            
            // Create output format
            val outputFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", outputSampleRate, outputChannels)
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, TARGET_BITRATE)
            outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            outputFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            
            // Set up encoder with error handling
            encoder = try {
                MediaCodec.createEncoderByType("audio/mp4a-latm")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create AAC encoder", e)
                return false
            }
            
            try {
                encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                encoder.start()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to configure/start encoder", e)
                encoder.release()
                return false
            }
            
            // Set up decoder if input is encoded (not raw PCM)
            if (inputMime != "audio/raw" && inputMime != "audio/pcm") {
                try {
                    decoder = MediaCodec.createDecoderByType(inputMime)
                    decoder.configure(inputFormat, null, null, 0)
                    decoder.start()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create/configure decoder for $inputMime", e)
                    // Try without decoder (treat as raw)
                    decoder?.release()
                    decoder = null
                }
            }
            
            // Set up muxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerTrackIndex = -1
            
            val bufferInfo = MediaCodec.BufferInfo()
            val timeoutUs = 5000L // Reduced timeout for faster iteration
            var inputDone = false
            var decoderDone = decoder == null
            var encoderDone = false
            var loopCount = 0
            val maxLoops = 1_000_000 // Safety limit to prevent infinite loops
            
            while (!encoderDone && loopCount < maxLoops) {
                loopCount++
                
                // Feed data to decoder (if applicable) or directly to encoder
                if (!inputDone) {
                    if (decoder != null) {
                        val inputBufferIndex = decoder.dequeueInputBuffer(timeoutUs)
                        if (inputBufferIndex >= 0) {
                            val buffer = decoder.getInputBuffer(inputBufferIndex)!!
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                val presentationTimeUs = extractor.sampleTime
                                decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)
                                extractor.advance()
                            }
                        }
                    } else {
                        // Direct to encoder for PCM
                        val inputBufferIndex = encoder.dequeueInputBuffer(timeoutUs)
                        if (inputBufferIndex >= 0) {
                            val buffer = encoder.getInputBuffer(inputBufferIndex)!!
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) {
                                encoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                val presentationTimeUs = extractor.sampleTime
                                encoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)
                                extractor.advance()
                            }
                        }
                    }
                }
                
                // Get decoded output and feed to encoder
                if (decoder != null && !decoderDone) {
                    val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                    if (outputBufferIndex >= 0) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            decoderDone = true
                            // Signal end of stream to encoder
                            val encoderInputIndex = encoder.dequeueInputBuffer(timeoutUs)
                            if (encoderInputIndex >= 0) {
                                encoder.queueInputBuffer(encoderInputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            }
                        } else if (bufferInfo.size > 0) {
                            val decodedBuffer = decoder.getOutputBuffer(outputBufferIndex)!!
                            
                            // Feed to encoder
                            val encoderInputIndex = encoder.dequeueInputBuffer(timeoutUs)
                            if (encoderInputIndex >= 0) {
                                val encoderBuffer = encoder.getInputBuffer(encoderInputIndex)!!
                                encoderBuffer.clear()
                                
                                // Limit copy to encoder buffer capacity
                                val bytesToCopy = minOf(bufferInfo.size, encoderBuffer.remaining())
                                decodedBuffer.limit(decodedBuffer.position() + bytesToCopy)
                                encoderBuffer.put(decodedBuffer)
                                
                                encoder.queueInputBuffer(encoderInputIndex, 0, bytesToCopy, bufferInfo.presentationTimeUs, 0)
                            }
                        }
                        decoder.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }
                
                // Get encoded output and write to muxer
                val encoderOutputIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                when {
                    encoderOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            val newFormat = encoder.outputFormat
                            muxerTrackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    encoderOutputIndex >= 0 -> {
                        val encodedBuffer = encoder.getOutputBuffer(encoderOutputIndex)!!
                        
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        
                        if (bufferInfo.size > 0 && muxerStarted) {
                            encodedBuffer.position(bufferInfo.offset)
                            encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(muxerTrackIndex, encodedBuffer, bufferInfo)
                        }
                        
                        encoder.releaseOutputBuffer(encoderOutputIndex, false)
                        
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            encoderDone = true
                        }
                    }
                }
            }
            
            if (loopCount >= maxLoops) {
                Log.w(TAG, "Compression exceeded max loop count, aborting")
                return false
            }
            
            Log.d(TAG, "Compression completed in $loopCount iterations")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "MediaCodec compression failed", e)
            return false
        } finally {
            // Cleanup in reverse order of creation
            try { decoder?.stop() } catch (e: Exception) { Log.w(TAG, "Error stopping decoder", e) }
            try { decoder?.release() } catch (e: Exception) { Log.w(TAG, "Error releasing decoder", e) }
            try { encoder?.stop() } catch (e: Exception) { Log.w(TAG, "Error stopping encoder", e) }
            try { encoder?.release() } catch (e: Exception) { Log.w(TAG, "Error releasing encoder", e) }
            
            try { 
                if (muxerStarted) {
                    muxer?.stop() 
                }
            } catch (e: Exception) { Log.w(TAG, "Error stopping muxer", e) }
            
            try { muxer?.release() } catch (e: Exception) { Log.w(TAG, "Error releasing muxer", e) }
            try { extractor?.release() } catch (e: Exception) { Log.w(TAG, "Error releasing extractor", e) }
        }
    }
}
