package com.example.nossafeira

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nossafeira.navigation.NossaFeiraNavGraph
import com.example.nossafeira.ui.theme.NossaFeiraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NossaFeiraTheme {
                NossaFeiraNavGraph()
            }
        }
    }
}
