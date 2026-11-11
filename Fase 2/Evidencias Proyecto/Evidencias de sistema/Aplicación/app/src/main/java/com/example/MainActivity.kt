package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import com.example.ui.util.DataUriFetcher
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BarberiaViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    // Fotos del personal guardadas como data URL en la base de datos (11-11-2026)
    Coil.setImageLoader(ImageLoader.Builder(this).components { add(DataUriFetcher.Factory()) }.crossfade(true).build())
    setContent {
      val viewModel: BarberiaViewModel = viewModel()
      val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

      MyApplicationTheme(darkTheme = isDarkMode) {
        MainAppScreen(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}
