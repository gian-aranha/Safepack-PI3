package br.edu.puccampinas.safepack.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.puccampinas.safepack.R
import br.edu.puccampinas.safepack.databinding.ActivityFotoClienteBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FotoClienteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFotoClienteBinding
    private lateinit var cameraExecutor: ExecutorService
    private var qtdClientes: String? = null
    private var clienteAtual: String? = null
    private var imageCapture: ImageCapture? = null
    private val outputDirectory: File by lazy {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply {mkdirs()}
        }
        if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFotoClienteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        qtdClientes = intent.getStringExtra("qtdClientes")
        clienteAtual = intent.getStringExtra("clienteAtual")

        startCamera()

        binding.voltarButton.setOnClickListener {
            val iOpcaoCadastro = Intent(this, OpcaoDeCadastroActivity::class.java)
            startActivity(iOpcaoCadastro)
        }

        binding.tirarFotoButton.setOnClickListener {
            takePhoto()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("FotoClienteActivity", "Erro ao executar câmera")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        blinkPreview()

        val imageCapture = imageCapture?: return
        val fileName = "FOTO-${System.currentTimeMillis()}.jpg"

        val photoFile = File(
            outputDirectory,
            fileName
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object: ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("FotoClienteActivity", "Erro na captura de foto: ${exc.message}")
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("FotoClienteActivity", "Path: ${photoFile.absolutePath}")

                    val iNFC = Intent(this@FotoClienteActivity, CadastroNfcActivity::class.java)
                    iNFC.putExtra("qtdClientes", qtdClientes)
                    if(qtdClientes.equals("2") && clienteAtual.equals("1")) {
                        iNFC.putExtra("photoFilePath1", photoFile.absolutePath)
                        iNFC.putExtra("clienteAtual", clienteAtual)
                    } else if(qtdClientes.equals("2") && clienteAtual.equals("2")) {
                        iNFC.putExtra("photoFilePath2", photoFile.absolutePath)
                        iNFC.putExtra("clienteAtual", clienteAtual)
                    } else {
                        iNFC.putExtra("photoFilePath1", photoFile.absolutePath)
                    }

                    startActivity(iNFC)
                }
            }
        )
    }

    private fun blinkPreview() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }
}