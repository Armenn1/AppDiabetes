package com.example.tfg_diabetesapp.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
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
import com.example.tfg_diabetesapp.BoloActivity
import com.example.tfg_diabetesapp.BoloLog
import com.example.tfg_diabetesapp.BuildConfig
import com.example.tfg_diabetesapp.CarbsCalculator
import com.example.tfg_diabetesapp.FoodScanResult
import com.example.tfg_diabetesapp.IobCalculator
import com.example.tfg_diabetesapp.R
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class ScannerFragment : Fragment() {

    // ── Vistas ───────────────────────────────────────────────────────────────
    private lateinit var ivFoodPhoto: ImageView
    private lateinit var layoutPlaceholder: LinearLayout
    private lateinit var btnTakePhoto: MaterialButton
    private lateinit var btnAnalyze: MaterialButton
    private lateinit var layoutLoading: LinearLayout
    private lateinit var cardResultado: MaterialCardView
    private lateinit var layoutBotonesAccion: LinearLayout
    private lateinit var btnGuardar: MaterialButton
    private lateinit var btnCalcularBolo: MaterialButton
    private lateinit var tvNombreComida: TextView
    private lateinit var tvDescripcionComida: TextView
    private lateinit var tvCarbohidratos: TextView
    private lateinit var tvFibra: TextView
    private lateinit var tvProteinas: TextView
    private lateinit var tvGrasas: TextView
    private lateinit var tvRaciones: TextView
    private lateinit var tvIndiceGlucemico: TextView
    private lateinit var tvConfianza: TextView
    private lateinit var tvContextGlucosa: TextView
    private lateinit var tvContextIob: TextView
    private lateinit var tvContextCob: TextView

    // ── Estado ───────────────────────────────────────────────────────────────
    private var imageBitmap: Bitmap? = null
    private var resultadoActual: FoodScanResult? = null
    private var glucosaActual: Int = 0
    private var tendenciaActual: Int = 0
    private var iobActual: Double = 0.0
    private var cobActual: Double = 0.0
    private var diaHoras: Double = 5.0
    private var peakMin: Int = 75

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // ── Cámara ───────────────────────────────────────────────────────────────
    private val takePicturePreview = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) takePicturePreview.launch(null)
        else Toast.makeText(requireContext(), "Necesitas permiso de cámara", Toast.LENGTH_LONG).show()
    }

    // ── onCreateView ─────────────────────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scanner, container, false)

        ivFoodPhoto          = view.findViewById(R.id.ivFoodPhoto)
        layoutPlaceholder    = view.findViewById(R.id.layoutPlaceholder)
        btnTakePhoto         = view.findViewById(R.id.btnTakePhoto)
        btnAnalyze           = view.findViewById(R.id.btnAnalyze)
        layoutLoading        = view.findViewById(R.id.layoutLoading)
        cardResultado        = view.findViewById(R.id.cardResultado)
        layoutBotonesAccion  = view.findViewById(R.id.layoutBotonesAccion)
        btnGuardar           = view.findViewById(R.id.btnGuardar)
        btnCalcularBolo      = view.findViewById(R.id.btnCalcularBolo)
        tvNombreComida       = view.findViewById(R.id.tvNombreComida)
        tvDescripcionComida  = view.findViewById(R.id.tvDescripcionComida)
        tvCarbohidratos      = view.findViewById(R.id.tvCarbohidratos)
        tvFibra              = view.findViewById(R.id.tvFibra)
        tvProteinas          = view.findViewById(R.id.tvProteinas)
        tvGrasas             = view.findViewById(R.id.tvGrasas)
        tvRaciones           = view.findViewById(R.id.tvRaciones)
        tvIndiceGlucemico    = view.findViewById(R.id.tvIndiceGlucemico)
        tvConfianza          = view.findViewById(R.id.tvConfianza)
        tvContextGlucosa     = view.findViewById(R.id.tvContextGlucosa)
        tvContextIob         = view.findViewById(R.id.tvContextIob)
        tvContextCob         = view.findViewById(R.id.tvContextCob)

        cargarContexto()

        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                takePicturePreview.launch(null)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        btnAnalyze.setOnClickListener {
            imageBitmap?.let { analizarConGemini(it) }
        }

        btnGuardar.setOnClickListener {
            resultadoActual?.let { scan ->
                guardarMealScan(scan, dosisCalculada = 0.0)
                guardarComidaEnHistory(scan)
            }
        }

        btnCalcularBolo.setOnClickListener {
            resultadoActual?.let { scan ->
                guardarMealScan(scan, dosisCalculada = 0.0)
                guardarComidaEnHistory(scan)
                val intent = Intent(requireContext(), BoloActivity::class.java).apply {
                    putExtra(BoloActivity.EXTRA_RACIONES, scan.raciones)
                    putExtra("tendencia", tendenciaActual)
                    putExtra(BoloActivity.EXTRA_IMAGEN_URL, scan.imagenUrl)
                }
                startActivity(intent)
            }
        }

        return view
    }

    // ── Cargar contexto (glucosa + IOB + COB) ─────────────────────────────────
    private fun cargarContexto() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                diaHoras = doc.getDouble("diaHoras") ?: 5.0
                peakMin  = (doc.getLong("insulinaPeakMin") ?: 75L).toInt()

                // Última lectura de glucosa
                db.collection("users").document(userId)
                    .collection("glucoseReadings")
                    .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { readings ->
                        val ultima = readings.documents.firstOrNull()
                        glucosaActual  = ultima?.getLong("valor")?.toInt() ?: 0
                        tendenciaActual = ultima?.getLong("tendencia")?.toInt() ?: 0
                        val tendenciaStr = when (tendenciaActual) {
                            1 -> "↓↓"; 2 -> "↓"; 3 -> "→"; 4 -> "↑"; 5 -> "↑↑"
                            else -> ""
                        }
                        tvContextGlucosa.text = if (glucosaActual > 0)
                            "$glucosaActual mg/dL $tendenciaStr" else "-- mg/dL"
                    }

                // IOB
                val cutoff = System.currentTimeMillis() - (diaHoras * 3_600_000).toLong()
                db.collection("users").document(userId)
                    .collection("history")
                    .whereGreaterThan("fecha", cutoff)
                    .get()
                    .addOnSuccessListener { docs ->
                        val logs = docs.map { d ->
                            BoloLog(fecha = d.getLong("fecha") ?: 0L,
                                    dosisTotal = d.getDouble("dosisTotal") ?: 0.0)
                        }
                        iobActual = IobCalculator.calcular(logs, (diaHoras * 60).toInt(), peakMin)
                        tvContextIob.text = "${(iobActual * 10).roundToInt() / 10.0} U"
                    }

                // COB — leer mealScans de las últimas 4.5h (270 min)
                val cutoffCob = System.currentTimeMillis() - (270 * 60_000L)
                db.collection("users").document(userId)
                    .collection("mealScans")
                    .whereGreaterThan("fecha", cutoffCob)
                    .get()
                    .addOnSuccessListener { docs ->
                        val scans = docs.map { d ->
                            FoodScanResult(
                                fecha = d.getLong("fecha") ?: 0L,
                                carbohidratosNetos = d.getDouble("carbohidratosNetos") ?: 0.0,
                                grasas = d.getDouble("grasas") ?: 0.0
                            )
                        }
                        cobActual = CarbsCalculator.calcular(scans)
                        val cobRed = (cobActual * 10).roundToInt() / 10.0
                        tvContextCob.text = "$cobRed g"
                    }
            }
    }

    // ── Análisis con Gemini ───────────────────────────────────────────────────
    private fun analizarConGemini(bitmap: Bitmap) {
        btnAnalyze.isEnabled = false
        btnTakePhoto.isEnabled = false
        layoutLoading.visibility = View.VISIBLE
        cardResultado.visibility = View.GONE
        layoutBotonesAccion.visibility = View.GONE

        val prompt = """
            Analiza el plato de la imagen y devuelve ÚNICAMENTE un JSON válido con esta estructura,
            sin texto adicional antes ni después:
            {
              "comida": "nombre del plato",
              "descripcion": "ingredientes principales visibles",
              "carbohidratos": 0.0,
              "fibra": 0.0,
              "proteinas": 0.0,
              "grasas": 0.0,
              "carbohidratos_netos": 0.0,
              "indice_glucemico": 0,
              "carga_glucemica": 0.0,
              "raciones": 0.0,
              "confianza": "alta"
            }
            Notas:
            - carbohidratos_netos = carbohidratos - fibra
            - raciones = carbohidratos_netos / 10
            - carga_glucemica = (indice_glucemico × carbohidratos_netos) / 100
            - confianza "baja" si el plato no se identifica claramente
            - Responde SOLO con el JSON, sin texto adicional
        """.trimIndent()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val model = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = BuildConfig.GEMINI_API_KEY
                )
                val response = model.generateContent(content {
                    image(bitmap)
                    text(prompt)
                })

                val raw = response.text ?: throw Exception("Respuesta vacía")
                // Limpiar posibles bloques markdown que Gemini añade a veces
                val jsonStr = raw.trim()
                    .removePrefix("```json").removePrefix("```")
                    .removeSuffix("```").trim()

                val json = JSONObject(jsonStr)
                val ahora = System.currentTimeMillis()
                val rutaFoto = guardarBitmapLocal(bitmap, ahora)
                val scan = FoodScanResult(
                    fecha               = ahora,
                    comida              = json.optString("comida", "Plato desconocido"),
                    descripcion         = json.optString("descripcion", ""),
                    carbohidratos       = json.optDouble("carbohidratos", 0.0),
                    fibra               = json.optDouble("fibra", 0.0),
                    proteinas           = json.optDouble("proteinas", 0.0),
                    grasas              = json.optDouble("grasas", 0.0),
                    carbohidratosNetos  = json.optDouble("carbohidratos_netos", 0.0),
                    indiceGlucemico     = json.optInt("indice_glucemico", 0),
                    cargaGlucemica      = json.optDouble("carga_glucemica", 0.0),
                    raciones            = json.optDouble("raciones", 0.0),
                    confianza           = json.optString("confianza", "media"),
                    glucosaAlEscanear   = glucosaActual,
                    tendenciaAlEscanear = tendenciaActual,
                    iobAlEscanear       = iobActual,
                    cobAlEscanear       = cobActual,
                    tipoComida          = inferirTipoComida(),
                    imagenUrl           = rutaFoto
                )

                withContext(Dispatchers.Main) {
                    mostrarResultado(scan)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    layoutLoading.visibility = View.GONE
                    btnAnalyze.isEnabled = true
                    btnTakePhoto.isEnabled = true
                    Toast.makeText(requireContext(),
                        "Error al analizar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ── Mostrar resultado ─────────────────────────────────────────────────────
    private fun mostrarResultado(scan: FoodScanResult) {
        resultadoActual = scan
        layoutLoading.visibility = View.GONE
        btnAnalyze.isEnabled = true
        btnTakePhoto.isEnabled = true
        cardResultado.visibility = View.VISIBLE
        layoutBotonesAccion.visibility = View.VISIBLE

        tvNombreComida.text      = scan.comida
        tvDescripcionComida.text = scan.descripcion
        tvCarbohidratos.text     = "${round1(scan.carbohidratos)} g"
        tvFibra.text             = "${round1(scan.fibra)} g"
        tvProteinas.text         = "${round1(scan.proteinas)} g"
        tvGrasas.text            = "${round1(scan.grasas)} g"
        tvRaciones.text          = "${round1(scan.raciones)} rac"
        tvIndiceGlucemico.text   = scan.indiceGlucemico.toString()

        // Badge de confianza
        val (confianzaTexto, confianzaColor) = when (scan.confianza.lowercase(Locale.ROOT)) {
            "alta"  -> Pair("Alta", Color.parseColor("#4CAF50"))
            "baja"  -> Pair("Baja", Color.parseColor("#EF5350"))
            else    -> Pair("Media", Color.parseColor("#FF9800"))
        }
        tvConfianza.text = confianzaTexto
        tvConfianza.setBackgroundColor(confianzaColor)
    }

    // ── Guardar en Firestore ──────────────────────────────────────────────────
    private fun guardarMealScan(scan: FoodScanResult, dosisCalculada: Double) {
        val userId = auth.currentUser?.uid ?: return
        val data = hashMapOf(
            "fecha"              to scan.fecha,
            "comida"             to scan.comida,
            "descripcion"        to scan.descripcion,
            "carbohidratos"      to scan.carbohidratos,
            "fibra"              to scan.fibra,
            "proteinas"          to scan.proteinas,
            "grasas"             to scan.grasas,
            "carbohidratosNetos" to scan.carbohidratosNetos,
            "indiceGlucemico"    to scan.indiceGlucemico,
            "cargaGlucemica"     to scan.cargaGlucemica,
            "raciones"           to scan.raciones,
            "confianza"          to scan.confianza,
            "glucosaAlEscanear"  to scan.glucosaAlEscanear,
            "tendenciaAlEscanear" to scan.tendenciaAlEscanear,
            "iobAlEscanear"      to scan.iobAlEscanear,
            "cobAlEscanear"      to scan.cobAlEscanear,
            "dosisCalculada"     to dosisCalculada,
            "tipoComida"         to scan.tipoComida,
            "imagenUrl"          to scan.imagenUrl
        )
        db.collection("users").document(userId)
            .collection("mealScans")
            .document(scan.fecha.toString())
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Guardado ✓", Toast.LENGTH_SHORT).show()
                cardResultado.visibility = View.GONE
                layoutBotonesAccion.visibility = View.GONE
                resultadoActual = null
                imageBitmap = null
                ivFoodPhoto.visibility = View.GONE
                layoutPlaceholder.visibility = View.VISIBLE
                btnAnalyze.isEnabled = false
                cargarContexto()   // refrescar COB tras guardar
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Guardar Comida en history (entrada tipo="Comida") ────────────────────
    private fun guardarComidaEnHistory(scan: FoodScanResult) {
        val userId = auth.currentUser?.uid ?: return
        val mealScanId = scan.fecha.toString()
        val log = BoloLog(
            fecha            = scan.fecha,
            raciones         = scan.raciones,
            gramos           = scan.carbohidratos,
            tipo             = "Comida",
            nota             = scan.comida,
            tipoComida       = scan.tipoComida,
            imagenUrl        = scan.imagenUrl,
            mealScanId       = mealScanId,
            proteinas        = scan.proteinas,
            grasas           = scan.grasas,
            fibra            = scan.fibra,
            indiceGlucemico  = scan.indiceGlucemico,
            tendencia        = scan.tendenciaAlEscanear
        )
        db.collection("users").document(userId)
            .collection("history")
            .document(mealScanId)
            .set(log)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun round1(v: Double) = (v * 10).roundToInt() / 10.0

    private fun guardarBitmapLocal(bitmap: Bitmap, fecha: Long): String {
        return try {
            val dir = File(requireContext().filesDir, "meal_photos").apply { mkdirs() }
            val file = File(dir, "$fecha.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (_: Exception) { "" }
    }

    private fun inferirTipoComida(): String {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hora) {
            in 6..10  -> "desayuno"
            in 11..15 -> "comida"
            in 16..19 -> "merienda"
            in 20..23 -> "cena"
            else      -> "snack"
        }
    }
}
