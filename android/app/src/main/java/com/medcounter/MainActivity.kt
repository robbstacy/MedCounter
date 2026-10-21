package com.medcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.medcounter.ui.AppNavigation
import com.medcounter.ui.theme.MedCounterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MedCounterApplication).container
        setContent {
            MedCounterTheme {
                AppNavigation(container)
            }
        }
    }
}
