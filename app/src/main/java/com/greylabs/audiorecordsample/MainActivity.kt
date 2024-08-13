package com.greylabs.audiorecordsample

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.greylabs.audiorecordsample.ui.DynamicAudioButton
import com.greylabs.audiorecordsample.ui.theme.AudioRecordSampleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : ComponentActivity() {

    private var permissionToRecordAccepted = false
    private var permission = Manifest.permission.RECORD_AUDIO

    private var player: MediaPlayer? = null

    private val realtimeAudioRecorder = RealtimeAudioRecorder(this@MainActivity)

    private val requestPermissionLauncher by lazy {
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                permissionToRecordAccepted = true
            } else {
                permissionToRecordAccepted = false
                requestRecordPermission()
            }
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var scaleParamsList by mutableStateOf(listOf(1f, 1f, 1f))

        checkPermission()
        lifecycleScope.launch {
            realtimeAudioRecorder.audioDataFlow.collect { data ->
                scaleParamsList = data.map { 1f + it / 8 }
            }
        }

        setContent {
            AudioRecordSampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Spacer(modifier = Modifier.height(120.dp))
                        DynamicAudioButton(
                            scales = scaleParamsList,
                            dynamicWavesResList = listOf(
                                R.drawable.img_wave_1,
                                R.drawable.img_wave_2,
                                R.drawable.img_wave_3,
                            ),
                            isRecordActive = realtimeAudioRecorder.isRecording,
                            onClickAction = {
                                if (realtimeAudioRecorder.isRecording) {
                                    stopRecord()
                                } else {
                                    startRecord()
                                }
                            }
                        )
                        OutlinedButton(onClick = {}) {
                            Text(text = if (realtimeAudioRecorder.isRecording) "Stop" else "Play")
                        }
                    }
                }
            }
        }
    }

    private fun startRecord() {
        realtimeAudioRecorder.reInitRecording()
        lifecycleScope.launch(Dispatchers.IO) {
            realtimeAudioRecorder.startRecording()
        }
    }

    private fun stopRecord() {
        realtimeAudioRecorder.stopRecording()
    }

    private fun checkPermission() {
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            permissionToRecordAccepted = true
            return
        } else {
            requestRecordPermission()
        }
    }

    private fun requestRecordPermission() {
        requestPermissionLauncher.launch(permission)
    }

    private fun startPlaying() {
        player = MediaPlayer().apply {
            try {
//                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    private companion object {
        const val LOG_TAG = "DYNAMIC_AUDIO_BTN"
    }
}
