package com.calltracker.manager.util

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AudioCompressor {

    /**
     * Compresses an audio file to a lower bitrate MP3 using FFmpeg.
     * Target: 64k constant bitrate to balance quality and file size.
     */
    suspend fun compressAudio(inputFile: File, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        if (!inputFile.exists()) return@withContext false
        
        // Remove output file if it already exists to avoid ffmpeg prompt
        if (outputFile.exists()) {
            outputFile.delete()
        }

        // Command: -i [input] -codec:a libmp3lame -b:a 64k [output]
        // -y to overwrite if needed (though we delete above)
        val command = "-i \"${inputFile.absolutePath}\" -codec:a libmp3lame -b:a 64k \"${outputFile.absolutePath}\" -y"
        
        val session = FFmpegKit.execute(command)
        val returnCode = session.returnCode

        ReturnCode.isSuccess(returnCode)
    }
}
