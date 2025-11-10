package com.example.appdiabetes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appdiabetes.ui.theme.AppDiabetesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDiabetesTheme {
                // Usamos un Surface para aplicar un color de fondo a toda la app
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    Scaffold(
        topBar = { AppHeader() },
        bottomBar = { AppFooter() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp) // Añadimos padding general al contenido
                .fillMaxSize()
        ) {
            // Cada sección es un composable separado para mayor claridad
            GlucoseSection(hasData = false) // Cambia a 'true' para ver el otro estado
            Spacer(modifier = Modifier.height(24.dp))
            DoseCalculatorSection()
            Spacer(modifier = Modifier.height(24.dp))
            TipsCarouselSection()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader() {
    TopAppBar(
        title = {
            Text(
                text = "AppDiabetes",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        actions = {
            IconButton(onClick = { /* TODO: Navegar al perfil */ }) {
                Icon(Icons.Default.Person, contentDescription = "Perfil de usuario")
            }
            IconButton(onClick = { /* TODO: Abrir menú lateral */ }) {
                Icon(Icons.Default.Menu, contentDescription = "Menú")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun GlucoseSection(hasData: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Columna para los datos de glucosa o el botón
            Column(modifier = Modifier.weight(1f)) {
                Text("Glucosa Actual", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (hasData) {
                    Text(
                        "120 mg/dL",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Última medición: 10:35 AM")
                } else {
                    Button(onClick = { /* TODO: Abrir pantalla para añadir dato */ }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Añadir dato")
                    }
                }
            }
            // Espacio para el gráfico
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Color.LightGray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Gráfico", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun DoseCalculatorSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = { /* TODO: Navegar a la calculadora de dosis */ }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Calcular Dosis", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Insulina para tu próxima comida",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                Icons.Default.Add,
                contentDescription = "Calcular dosis",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun TipsCarouselSection() {
    Column {
        Text("Blog, Recetas y Consejos", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        // Datos de ejemplo para el carrusel
        val items = listOf(
            "Receta: Ensalada saludable",
            "Tip: Medir glucosa post-ejercicio",
            "Blog: Nuevas tecnologías"
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                TipCard(title = item)
            }
        }
    }
}

@Composable
fun TipCard(title: String) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AppFooter() {
    NavigationBar {
        // Aquí irían los items del menú de navegación inferior
        // Ejemplo con 3 items:
        NavigationBarItem(
            icon = { Icon(Icons.Default.Menu, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            selected = true,
            onClick = { /* TODO */ }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Add, contentDescription = "Registros") },
            label = { Text("Registros") },
            selected = false,
            onClick = { /* TODO */ }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Comunidad") },
            label = { Text("Comunidad") },
            selected = false,
            onClick = { /* TODO */ }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    AppDiabetesTheme {
        HomeScreen()
    }
}
