package com.example.tfg_diabetesapp.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tfg_diabetesapp.BuildConfig
import com.example.tfg_diabetesapp.R
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScannerFragment : Fragment() {

    private lateinit var ivFoodPhoto: ImageView
    private lateinit var layoutPlaceholder: LinearLayout
    private lateinit var btnTakePhoto: MaterialButton
    private lateinit var btnAnalyze: MaterialButton
    private lateinit var tvAiResult: TextView
    private lateinit var layoutLoading: LinearLayout

    private var imageBitmap: Bitmap? = null

    private val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            imageBitmap = bitmap
            layoutPlaceholder.visibility = View.GONE
            ivFoodPhoto.visibility = View.VISIBLE
            ivFoodPhoto.setImageBitmap(bitmap)
            btnAnalyze.isEnabled = true
        } else {
            Toast.makeText(requireContext(), "Foto cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takePicturePreview.launch(null)
        } else {
            Toast.makeText(requireContext(), "Necesitas dar permiso de cámara para escanear", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scanner, container, false)

        ivFoodPhoto = view.findViewById(R.id.ivFoodPhoto)
        layoutPlaceholder = view.findViewById(R.id.layoutPlaceholder)
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto)
        btnAnalyze = view.findViewById(R.id.btnAnalyze)
        tvAiResult = view.findViewById(R.id.tvAiResult)
        layoutLoading = view.findViewById(R.id.layoutLoading)

        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takePicturePreview.launch(null)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        btnAnalyze.setOnClickListener {
            imageBitmap?.let { analyzeImageWithGemini(it) }
        }

        return view
    }

    private fun analyzeImageWithGemini(bitmap: Bitmap) {
        btnAnalyze.isEnabled = false
        btnTakePhoto.isEnabled = false
        layoutLoading.visibility = View.VISIBLE
        tvAiResult.text = ""

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = BuildConfig.GEMINI_API_KEY
                )

                val inputContent = content {
                    image(bitmap)
                    text("Actúa como un nutricionista experto en diabetes tipo 1. Mira esta foto y dime qué comida principal ves. Luego, estima los gramos totales de Hidratos de Carbono (HC) de esa ración. Sé directo. Termina diciendo cuántas raciones son (1 ración = 10g de HC). Responde únicamente con qué comida es, la cantidad de carbohidratos y las raciones. Respuesta breve.")
                }

                val response = generativeModel.generateContent(inputContent)

                withContext(Dispatchers.Main) {
                    layoutLoading.visibility = View.GONE
                    tvAiResult.text = response.text
                    btnAnalyze.isEnabled = true
                    btnTakePhoto.isEnabled = true
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    layoutLoading.visibility = View.GONE
                    tvAiResult.text = "Error de conexión: ${e.message}"
                    btnAnalyze.isEnabled = true
                    btnTakePhoto.isEnabled = true
                }
            }
        }
    }
}
