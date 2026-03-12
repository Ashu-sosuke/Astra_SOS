package com.example.sos.modelCread

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): File {
        outputFile = File(
            context.cacheDir,
            "sos_${System.currentTimeMillis()}.m4a"
        )

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }

        return outputFile!!
    }

    fun stopRecording(): File? {

        recorder?.apply {
            stop()
            release()
        }

        recorder = null
        return outputFile
    }
}