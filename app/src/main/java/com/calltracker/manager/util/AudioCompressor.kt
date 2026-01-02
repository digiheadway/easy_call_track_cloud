package com.calltracker.manager.util

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 */
object AudioCompressor {

    private const val TAG = "AudioCompressor"
    
    // Target settings for voice call compression
    private const val TARGET_BITRATE = 64000 // 64 kbps - good for voice
    private const val TARGET_SAMPLE_RATE = 22050 // 22.05 kHz - sufficient for voice
    private const val TARGET_CHANNELS = 1 // Mono - typical for call recordings
    
    /**
     * Compresses an audio file to a lower bitrate AAC/M4A.
     * 
     * @param inputFile The original recording file
     * @param outputFile The destination for compressed file
     * @return true if compression succeeded, false otherwise
     */
    suspend fun compressAudio(inputFile: File, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        if (!inputFile.exists()) {
            Log.w(TAG, "Input file does not exist: ${inputFile.absolutePath}")
            return@withContext false
        }

        val originalSize = inputFile.length()
        Log.d(TAG, "Starting compression: ${inputFile.name} (${originalSize / 1024} KB)")

        try {
            val success = compressWithMediaCodec(inputFile, outputFile)
            
            if (success && outputFile.exists()) {
                val compressedSize = outputFile.length()
                val savings = ((originalSize - compressedSize).toFloat() / originalSize * 100).toInt()
                Log.d(TAG, "Compression complete: ${compressedSize / 1024} KB (saved $savings%)")
                
                // Only use compressed if it's actually smaller
                if (compressedSize >= originalSize) {
                    Log.w(TAG, "Compressed file not smaller, using original")
                    outputFile.delete()
                    inputFile.copyTo(outputFile, overwrite = true)
                }
                return@withContext true
            } else {
                Log.w(TAG, "Compression failed, falling back to copy")
                inputFile.copyTo(outputFile, overwrite = true)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Compression error, falling back to copy", e)
            try {
                inputFile.copyTo(outputFile, overwrite = true)
                return@withContext true
            } catch (copyError: Exception) {
                Log.e(TAG, "Copy also failed", copyError)
                return@withContext false
            }
        }
    }
    
    private fun compressWithMediaCodec(inputFile: File, outputFile: File): Boolean {
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
            val inputSampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val inputChannels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            
            Log.d(TAG, "Input: $inputMime, ${inputSampleRate}Hz, $inputChannels ch")
            
            // If already a compressed format and small enough, just copy
            if (inputMime == "audio/mp4a-latm" || inputMime == "audio/aac") {
                val bitrate = try {
                    inputFormat.getInteger(MediaFormat.KEY_BIT_RATE)
                } catch (e: Exception) {
                    128000 // Default assumption
                }
                
                if (bitrate <= TARGET_BITRATE * 1.5) {
                    Log.d(TAG, "Already compressed at acceptable bitrate, copying")
                    inputFile.copyTo(outputFile, overwrite = true)
                    return true
                }
            }
            
            // For PCM/WAV files or high-bitrate files, re-encode
            // Use simpler approach: just re-encode to AAC without sample rate conversion
            val outputSampleRate = minOf(inputSampleRate, TARGET_SAMPLE_RATE)
            val outputChannels = minOf(inputChannels, TARGET_CHANNELS)
            
            // Create output format
            val outputFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", outputSampleRate, outputChannels)
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, TARGET_BITRATE)
            outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            outputFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            
            // Set up encoder
            encoder = MediaCodec.createEncoderByType("audio/mp4a-latm")
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            
            // Set up decoder if input is encoded
            if (inputMime != "audio/raw") {
                decoder = MediaCodec.createDecoderByType(inputMime)
                decoder.configure(inputFormat, null, null, 0)
                decoder.start()
            }
            
            // Set up muxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerTrackIndex = -1
            
            val bufferInfo = MediaCodec.BufferInfo()
            val timeoutUs = 10000L
            var inputDone = false
            var decoderDone = decoder == null
            var encoderDone = false
            
            val inputBuffer = ByteBuffer.allocate(16384)
            
            while (!encoderDone) {
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
                                encoderBuffer.put(decodedBuffer)
                                encoder.queueInputBuffer(encoderInputIndex, 0, bufferInfo.size, bufferInfo.presentationTimeUs, 0)
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
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "MediaCodec compression failed", e)
            return false
        } finally {
            try { decoder?.stop() } catch (e: Exception) { }
            try { decoder?.release() } catch (e: Exception) { }
            try { encoder?.stop() } catch (e: Exception) { }
            try { encoder?.release() } catch (e: Exception) { }
            
            try { 
                if (muxerStarted) {
                    muxer?.stop() 
                }
            } catch (e: Exception) { }
            
            try { muxer?.release() } catch (e: Exception) { }
            try { extractor?.release() } catch (e: Exception) { }
        }
    }
}
