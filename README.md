# Diabetly

Aplicación Android para personas con diabetes tipo 1. Combina la lectura continua de glucosa desde LibreLinkUp con una calculadora de bolo que descuenta automáticamente la insulina activa, un escáner de comida con IA, predicción de evolución de glucosa post-bolo y un panel de análisis con estadísticas de control metabólico.

> **Aviso médico:** Diabetly es una herramienta de apoyo. La información que muestra y los cálculos que realiza **no sustituyen el criterio de un profesional médico**. Las decisiones sobre dosis de insulina y tratamiento deben tomarse siempre con tu equipo sanitario.

## Funcionalidades

- **Glucosa en tiempo real** — Integración con la API de LibreLinkUp (FreeStyle Libre) para mostrar valor y tendencia. Sincronización en background mediante `WorkManager`.
- **Calculadora de bolo con IOB** — Cálculo de dosis según raciones, glucosa actual, objetivo y sensibilidad, descontando automáticamente la insulina activa de bolos anteriores (modelo bi-exponencial con DIA configurable).
- **Escáner de comida con IA** — La cámara apunta al plato y Gemini devuelve un JSON nutricional estructurado: carbohidratos, fibra, proteínas, grasas, índice glucémico, carga glucémica y raciones, listo para llevar a la calculadora.
- **Predicción post-bolo** — Modelo matemático que proyecta la glucosa durante las próximas 3 horas combinando IOB, COB (carbohidratos pendientes de absorber) y el efecto retardo de las grasas (fenómeno pizza, ventana de absorción 180/210/270 min).
- **Panel de análisis** — Tiempo en rango (TIR), tiempo bajo/sobre rango (TBR/TAR), media de glucosa, desviación, coeficiente de variación y GMI sobre el histórico de lecturas.
- **Registro de actividad física** — Diario de ejercicio aeróbico/anaeróbico con intensidad y duración, almacenado junto a las comidas para futura correlación.
- **Historial fotográfico** — Cada escaneo guarda la foto del plato junto a los datos nutricionales para revisión posterior.
- **Perfiles horarios** — Posibilidad de definir distintos ratios HC y de sensibilidad por franja horaria.
- **Widget en pantalla de inicio** — Acceso rápido al cálculo de bolo sin abrir la app.

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| Auth y base de datos | Firebase Auth + Firestore |
| API glucosa | LibreLinkUp (Retrofit 2) |
| IA visión | Google Generative AI SDK — Gemini 2.5 Flash |
| Gráficas | MPAndroidChart |
| Background | WorkManager |
| Anuncios | Google AdMob |
| UI | Material Design 3, AndroidX |

## Requisitos

- Android 7.0+ (API 24, target API 36)
- Proyecto Firebase con Auth y Firestore habilitados
- Cuenta LibreLinkUp con sensor FreeStyle Libre vinculado
- API key de Google AI (Gemini)
- ID de aplicación AdMob

## Configuración

1. Clona el repositorio y ábrelo en Android Studio.
2. Conecta el proyecto a tu proyecto Firebase descargando `google-services.json` y colocándolo en `app/`.
3. Crea el archivo `local.properties` en la raíz (ignorado por git) y añade:

   ```properties
   GEMINI_API_KEY=tu_clave_aqui
   ADMOB_APP_ID=tu_id_aqui
   ```

4. Construye y ejecuta en un dispositivo o emulador con API 24 o superior.

## Estructura del proyecto

```
app/src/main/java/com/example/tfg_diabetesapp/
├── LoginActivity.kt              # Autenticación
├── RegisterActivity.kt           # Registro de usuario
├── MainActivity.kt               # Bottom nav container
├── BoloActivity.kt               # Calculadora de bolo
├── BoloLog.kt                    # Data class historial bolos
├── IobCalculator.kt              # Cálculo IOB bi-exponencial
├── CarbsCalculator.kt            # Cálculo COB con ventana variable según grasa
├── GlucosePredictionModel.kt     # Predicción de glucosa post-bolo
├── GlucoseStatsCalculator.kt     # TIR / TBR / TAR / GMI / CV
├── FoodScanResult.kt             # Data class resultado del scanner
├── ActivityLog.kt                # Data class registro de ejercicio
├── ActivityLogActivity.kt        # Pantalla de registro de actividad
├── PerfilHorario.kt              # Ratios HC/sensibilidad por franja horaria
├── HistoryAdapter.kt             # Adapter RecyclerView historial
├── QuickBoloWidget.kt            # Widget pantalla de inicio
├── widgets/
│   └── MedicalHeaderView.kt      # Componente UI reutilizable
├── fragments/
│   ├── HomeFragment.kt           # Dashboard glucosa + IOB + COB + predicción
│   ├── HistoryFragment.kt        # Historial de bolos
│   ├── ScannerFragment.kt        # Escáner de comida con Gemini
│   ├── AnalysisFragment.kt       # Estadísticas TIR y análisis
│   └── SettingsFragment.kt       # Ajustes médicos y de perfil
└── glucose/
    ├── LibreLinkUpApi.kt         # Cliente REST LibreLinkUp
    ├── LibreLinkUpModels.kt      # Modelos de datos
    ├── LibreLinkUpRepository.kt  # Repositorio
    └── GlucoseSyncWorker.kt      # Sincronización en background
```

## Estructura Firestore

```
users/{userId}
  ├── factorHC          # Ratio insulina/HC (U por ración de 10g)
  ├── sensibilidad      # mg/dL que baja 1U de insulina
  ├── objetivoMin       # Límite inferior rango objetivo
  ├── objetivoMax       # Límite superior rango objetivo
  ├── diaHoras          # Duración acción insulina (DIA)
  ├── peso, altura, sexo, fechaNacimiento
  ├── tipoDiabetes      # "Tipo 1" | "Tipo 2" | "LADA"
  ├── insulinaBasal     # Nombre comercial
  ├── dosisBasal        # U/día
  ├── libreEmail
  └── librePassword

users/{userId}/history/{logId}            # Bolos administrados
users/{userId}/glucoseReadings/{id}       # Lecturas continuas de glucosa
users/{userId}/mealScans/{id}             # Resultados del escáner de comida
users/{userId}/activityLogs/{id}          # Registros de ejercicio
```

## Modelo de predicción post-bolo

`GlucosePredictionModel` combina tres efectos sobre la glucosa actual:

```
glucosa(t) = glucosaActual
           + subidaCarbos(t, COB, grasas)
           - bajadaInsulina(t, IOB, DIA)
```

La ventana de absorción de carbohidratos se ajusta según el contenido en grasas de la última comida (180 / 210 / 270 minutos), modelando el conocido "fenómeno pizza" en el que las grasas retrasan el pico glucémico. El modelo devuelve puntos cada 15 minutos durante un horizonte de 3 horas, que se renderizan como overlay sobre la gráfica de MPAndroidChart.

## Estado del proyecto

Diabetly nació como Trabajo de Fin de Grado y continúa en desarrollo activo. Próximos pasos previstos:

- Modelo de predicción basado en machine learning (TensorFlow Lite) entrenado con histórico propio.
- Detección automática de deriva de parámetros (sensibilidad y ratio HC observados vs configurados).
- Exportación de datos en PDF/CSV para llevar a consulta médica.
- Notificaciones inteligentes ante predicción de hipoglucemia o hiperglucemia.

Cualquier sugerencia, issue o pull request es bienvenido.

## Licencia

Proyecto open source. Uso libre con fines personales y de aprendizaje. El uso clínico queda fuera del alcance de este software; consulta siempre a un profesional sanitario.
