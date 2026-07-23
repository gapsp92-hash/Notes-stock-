package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_items")
data class StockItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "Dry Items", "Chilled Items", "Frozen Items", "Bar Items"
    val quantity: Int,
    val minLimit: Int = 5 // Stock summary lower stock limit
)

@Entity(tableName = "stock_transactions")
data class StockTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: Int,
    val itemName: String,
    val category: String,
    val type: String, // "IN" (stock added) or "OUT" (moved out / consumed)
    val quantityChanged: Int,
    val balanceAfter: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
