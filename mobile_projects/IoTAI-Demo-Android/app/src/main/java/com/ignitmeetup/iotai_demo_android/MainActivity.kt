package com.ignitmeetup.iotai_demo_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.ignitmeetup.iotai_demo_android.ui.theme.IoTAIDemoAndroidTheme
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load TFLite model from assets
        val modelFile = loadModelFile()
        val interpreter = Interpreter(modelFile)
        val contentManager = ContentManager(interpreter)

        setContent {
            IoTAIDemoAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ContentView(
                        contentManager = contentManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetManager = assets
        val fileDescriptor = assetManager.openFd("HVACModel.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}