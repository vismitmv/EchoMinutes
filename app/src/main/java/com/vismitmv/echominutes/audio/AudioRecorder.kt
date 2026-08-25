package com.vismitmv.echominutes.audio

import android.media.MediaRecorder
import java.io.File

class AudioRecorder {
    private var recorder: MediaRecorder? = null

    fun start(outputFile: File) {
        recorder?.release()
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
    }

    fun stop() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    fun release() {
        recorder?.release()
        recorder = null
    }
}
