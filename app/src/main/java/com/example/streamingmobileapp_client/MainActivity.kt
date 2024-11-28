package com.example.streamingmobileapp_client

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.streamingmobileapp_client.databinding.ActivityMainBinding
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var webSocketClient: WebSocketClient? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var isStreaming = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCamera()

        binding.recordButton.setOnClickListener {
            if (isStreaming) {
                stopStreaming()
            } else {
                startStreaming()
            }
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { image ->
                if (isStreaming) {
                    processFrame(image)
                } else {
                    image.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
            } catch (exc: Exception) {
                Log.e("Camera", "Error binding use cases", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startStreaming() {
        setupWebSocket()
        isStreaming = true
        binding.recordButton.text = "Stop Streaming"
        Log.d("Streaming", "Started streaming")
    }

    private fun stopStreaming() {
        webSocketClient?.close()
        isStreaming = false
        binding.recordButton.text = "Start Streaming"
        Log.d("Streaming", "Stopped streaming")
    }

    private fun setupWebSocket() {
        val uri = URI("ws://10.0.2.2:8080/stream") // Для эмулятора Android
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d("WebSocket", "Connected to server")
            }

            override fun onMessage(message: String?) {
                Log.d("WebSocket", "Received message: $message")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("WebSocket", "Connection closed: $reason")
            }

            override fun onError(ex: Exception?) {
                Log.e("WebSocket", "WebSocket error", ex)
            }
        }
        webSocketClient?.connect()
    }

    private fun processFrame(image: ImageProxy) {
        try {
            val bitmap = image.toBitmap()
            val jpegData = bitmap.toJpegByteArray()

            Log.d("Camera", "Frame captured: ${jpegData.size} bytes")
            sendFrame(jpegData)
        } catch (e: Exception) {
            Log.e("Camera", "Error processing frame", e)
        } finally {
            image.close()
        }
    }

    private fun sendFrame(frame: ByteArray) {
        if (webSocketClient?.isOpen == true) {
            webSocketClient?.send(frame)
        } else {
            Log.e("WebSocket", "WebSocket is not open")
        }
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
        }
    }

    private fun Bitmap.toJpegByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return outputStream.toByteArray()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStreaming()
        cameraExecutor.shutdown()
    }
}
