package com.diabetly.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.diabetly.app.fragments.AnalysisFragment
import com.diabetly.app.fragments.HistoryFragment
import com.diabetly.app.fragments.HomeFragment
import com.diabetly.app.fragments.ScannerFragment
import com.diabetly.app.fragments.SettingsFragment
import com.diabetly.app.glucose.GlucoseSyncWorker
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 1. Cargar la pantalla de Inicio al abrir la app
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        GlucoseSyncWorker.schedule(applicationContext)

        // 2. Escuchar los clicks en el menú de abajo
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true // Devuelve true para indicar que el click fue procesado
                }
                R.id.nav_history -> {
                    replaceFragment(HistoryFragment())
                    true
                }
                R.id.nav_scanner -> {
                    replaceFragment(ScannerFragment())
                    true
                }
                R.id.nav_analysis -> {
                    replaceFragment(AnalysisFragment())
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    // 3. Función auxiliar para cambiar la "carta" del centro
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}