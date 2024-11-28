package com.example.streamingmobileapp_client

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView // Используем PreviewView вместо SurfaceView
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class MainActivity : AppCompatActivity() {
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var recordButton: Button
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация кнопки записи
        recordButton = findViewById<Button>(R.id.recordButton)

        // Настройка WebSocket
        setupWebSocket()

        // Обработчик кнопки для старта и остановки записи
        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        // Инициализация камеры
        initCamera()
    }

    private fun startRecording() {
        isRecording = true
        recordButton.text = "Stop Recording"
        Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show()
        // Здесь можно добавить логику для захвата и передачи видео

        // Пример WebSocket подключения для отправки данных на сервер
        if (!webSocketClient.isOpen) {
            webSocketClient.connect()
        }
    }

    private fun stopRecording() {
        isRecording = false
        recordButton.text = "Start Recording"
        Toast.makeText(this, "Recording Stopped", Toast.LENGTH_SHORT).show()
        webSocketClient.close()
    }

    private fun setupWebSocket() {
        val uri = URI("ws://localhost/stream")
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                // Соединение установлено
            }

            override fun onMessage(message: String?) {
                // Обработка сообщений
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                // Соединение закрыто
            }

            override fun onError(ex: Exception?) {
                ex?.printStackTrace()
            }
        }
    }

    private fun initCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()

            // Привязка правильного типа для previewView
            val previewView = findViewById<PreviewView>(R.id.viewFinder) // Используем PreviewView
            preview.setSurfaceProvider(previewView.surfaceProvider) // Передаем surfaceProvider от PreviewView

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview
            )
        }, ContextCompat.getMainExecutor(this))
    }
}
