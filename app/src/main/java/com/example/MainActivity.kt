package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.StockDatabase
import com.example.data.StockRepository
import com.example.ui.StockApp
import com.example.ui.StockViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Database and Repository
    val database = StockDatabase.getDatabase(applicationContext)
    val repository = StockRepository(database.stockDao())

    setContent {
      MyApplicationTheme {
        val viewModel: StockViewModel = viewModel(
          factory = StockViewModel.Factory(repository)
        )
        StockApp(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}
