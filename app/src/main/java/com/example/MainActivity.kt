package com.example

import android.os.Bundle
import android.util.Log
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

    val repository = try {
      val database = StockDatabase.getDatabase(applicationContext)
      StockRepository(database.stockDao())
    } catch (e: Exception) {
      Log.e("MainActivity", "Error initializing database on startup", e)
      // Delete database and try again cleanly
      applicationContext.deleteDatabase("stock_database")
      val fallbackDb = StockDatabase.getDatabase(applicationContext)
      StockRepository(fallbackDb.stockDao())
    }

    setContent {
      MyApplicationTheme {
        val viewModel: StockViewModel = viewModel(
          factory = StockViewModel.Factory(applicationContext, repository)
        )
        StockApp(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

