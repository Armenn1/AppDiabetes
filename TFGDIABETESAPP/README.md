# TFG Diabetes App

Aplicación Android para la gestión de diabetes tipo 1. Permite calcular dosis de insulina (bolo), visualizar glucosa en tiempo real desde LibreLinkUp y consultar el historial de registros.

## Funcionalidades

- **Bolo de insulina** — Cálculo automático de dosis según raciones, glucosa actual y factores personalizados (HC, sensibilidad, objetivo)
- **Glucosa en tiempo real** — Integración con la API de LibreLinkUp (FreeStyle Libre) para mostrar lecturas continuas
- **Historial** — Registro y visualización de todos los bolos administrados
- **Asistente IA** — Chat con Gemini para resolver dudas sobre diabetes
- **Widget** — Acceso rápido al cálculo de bolo desde la pantalla de inicio
- **Ajustes** — Configuración de factores personalizados y credenciales LibreLinkUp

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| Auth & Base de datos | Firebase Auth + Firestore |
| API glucosa | LibreLinkUp (Retrofit 2) |
| IA | Google Generative AI SDK — Gemini 1.5 Flash |
| Anuncios | Google AdMob |
| UI | Material Design 3, AndroidX |

## Requisitos

- Android 7.0+ (API 24)
- Cuenta en Firebase (Auth + Firestore)
- Cuenta LibreLinkUp con sensor FreeStyle Libre vinculado
- API key de Google AI (Gemini)
- API key de AdMob

## Configuración

1. Clona el repositorio y ábrelo en Android Studio.
2. Conecta el proyecto a tu proyecto Firebase (descarga `google-services.json` y colócalo en `app/`).
3. Crea el archivo `local.properties` en la raíz (ya ignorado por git) y añade:

```properties
GEMINI_API_KEY=tu_clave_aqui
ADMOB_APP_ID=tu_id_aqui
```

4. Construye y ejecuta en un dispositivo/emulador con API 24+.

## Estructura del proyecto

```
app/src/main/java/com/example/tfg_diabetesapp/
├── LoginActivity.kt          # Autenticación
├── RegisterActivity.kt       # Registro de usuario
├── MainActivity.kt           # Actividad principal con navegación
├── BoloActivity.kt           # Cálculo de bolo
├── QuickBoloWidget.kt        # Widget de pantalla de inicio
├── fragments/
│   ├── HomeFragment.kt       # Dashboard con glucosa actual
│   ├── HistoryFragment.kt    # Historial de bolos
│   ├── SettingsFragment.kt   # Ajustes de usuario
│   └── NewsFragment.kt       # Noticias sobre diabetes
└── glucose/
    ├── LibreLinkUpApi.kt     # Cliente REST LibreLinkUp
    ├── LibreLinkUpModels.kt  # Modelos de datos de glucosa
    └── LibreLinkUpRepository.kt  # Repositorio de datos
```

## Estructura Firestore

```
users/{userId}
  ├── factorHC        # Gramos de HC por ración
  ├── sensibilidad    # mg/dL que baja 1U de insulina
  ├── target          # Glucosa objetivo (mg/dL)
  ├── libreEmail      # Email LibreLinkUp
  └── librePassword   # Contraseña LibreLinkUp

users/{userId}/history/{logId}
  ├── fecha
  ├── glucosa
  ├── raciones
  └── dosisTotal
```

## Licencia

Proyecto académico — Trabajo de Fin de Grado.
