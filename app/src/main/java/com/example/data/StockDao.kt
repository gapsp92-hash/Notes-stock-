package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items ORDER BY id ASC")
    suspend fun getAllItemsSnapshot(): List<StockItem>

    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSnapshot(): List<StockTransaction>

    @Query("SELECT * FROM stock_items WHERE category = :category ORDER BY name ASC")
    fun getItemsByCategory(category: String): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items WHERE quantity <= minLimit ORDER BY name ASC")
    fun getLowStockItems(): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Int): StockItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: StockItem): Long

    @Update
    suspend fun updateItem(item: StockItem)

    @Query("DELETE FROM stock_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransaction)

    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<StockTransaction>>

    // Update stock and write history atomically
    @Transaction
    suspend fun recordMovement(itemId: Int, type: String, quantityChanged: Int, note: String): Boolean {
        val item = getItemById(itemId) ?: return false
        val newQuantity = if (type == "IN") {
            item.quantity + quantityChanged
        } else {
            val res = item.quantity - quantityChanged
            if (res < 0) 0 else res
        }

        val updatedItem = item.copy(quantity = newQuantity)
        updateItem(updatedItem)

        val tx = StockTransaction(
            itemId = itemId,
            itemName = item.name,
            category = item.category,
            type = type,
            quantityChanged = quantityChanged,
            balanceAfter = newQuantity,
            note = note
        )
        insertTransaction(tx)
        return true
    }
}
