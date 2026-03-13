package com.example.tfg_diabetesapp.fragments

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.tfg_diabetesapp.R


class ScannerFragment : Fragment() {

    // 1. DECLARAMOS LAS VARIABLES (El error rojo era porque faltaba esto o sus imports)
    private lateinit var ivFoodPhoto: ImageView
    private lateinit var btnTakePhoto: Button
    private lateinit var btnAnalyze: Button
    private lateinit var tvAiResult: TextView
    private lateinit var progressBarAI: ProgressBar

    private var imageBitmap: Bitmap? = null

    // 2. PREPARAMOS LA CÁMARA
    private val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            imageBitmap = bitmap
            ivFoodPhoto.setImageBitmap(bitmap)
            btnAnalyze.isEnabled = true // Activamos el botón cuando hay foto
        } else {
            Toast.makeText(requireContext(), "Foto cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    // NUEVO: PREPARAMOS EL POP-UP DE SEGURIDAD DE ANDROID
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takePicturePreview.launch(null) // Si da permiso, abre cámara
        } else {
            Toast.makeText(requireContext(), "Necesitas dar permiso de cámara para escanear", Toast.LENGTH_LONG).show()
        }
    }

    // 3. CREAMOS LA PANTALLA
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scanner, container, false)

        // 4. CONECTAMOS EL CÓDIGO CON EL DISEÑO VISUAL
        ivFoodPhoto = view.findViewById(R.id.ivFoodPhoto)
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto)
        btnAnalyze = view.findViewById(R.id.btnAnalyze)
        tvAiResult = view.findViewById(R.id.tvAiResult)
        progressBarAI = view.findViewById(R.id.progressBarAI)

        // 5. ACCIONES DE LOS BOTONES
        // BOTÓN DE FOTO CON CONTROL DE SEGURIDAD
        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // Ya tenemos permiso, abrimos cámara directo
                takePicturePreview.launch(null)
            } else {
                // No tenemos permiso, sacamos el pop-up de Android
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        btnAnalyze.setOnClickListener {
            if (imageBitmap != null) {
                analyzeImageWithGemini(imageBitmap!!)
            }
        }

        return view
    }

    // 6. LA MAGIA DE LA INTELIGENCIA ARTIFICIAL
    private fun analyzeImageWithGemini(bitmap: Bitmap) {
        btnAnalyze.isEnabled = false
        btnTakePhoto.isEnabled = false
        progressBarAI.visibility = View.VISIBLE
        tvAiResult.text = "Analizando el plato... 🕵️‍♂️"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = "AIzaSyCM33JU6aVKFUJJW0F3KfyAag5IDpUdOE4" // APi key
                )

                val inputContent = content {
                    image(bitmap)
                    text("Actúa como un nutricionista experto en diabetes tipo 1. Mira esta foto y dime qué comida principal ves. Luego, estima los gramos totales de Hidratos de Carbono (HC) de esa ración. Sé directo. Termina diciendo cuántas raciones son (1 ración = 10g de HC). Responde unicamente con que comida es, la cantidad de carbohidratos y las raciones, Respuesta breve")
                }

                val response = generativeModel.generateContent(inputContent)

                withContext(Dispatchers.Main) {
                    tvAiResult.text = response.text
                    progressBarAI.visibility = View.GONE
                    btnAnalyze.isEnabled = true
                    btnTakePhoto.isEnabled = true
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvAiResult.text = "Error de conexión: ${e.message}"
                    progressBarAI.visibility = View.GONE
                    btnAnalyze.isEnabled = true
                    btnTakePhoto.isEnabled = true
                }
            }
        }
    }
}