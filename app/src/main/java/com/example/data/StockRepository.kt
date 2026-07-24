package com.example.data

import kotlinx.coroutines.flow.Flow

class StockRepository(val stockDao: StockDao) {
    val allItems: Flow<List<StockItem>> = stockDao.getAllItems()
    val lowStockItems: Flow<List<StockItem>> = stockDao.getLowStockItems()
    val allTransactions: Flow<List<StockTransaction>> = stockDao.getAllTransactions()

    fun getItemsByCategory(category: String): Flow<List<StockItem>> {
        return stockDao.getItemsByCategory(category)
    }

    suspend fun insertItem(item: StockItem): Long {
        return stockDao.insertItem(item)
    }

    suspend fun updateItem(item: StockItem) {
        stockDao.updateItem(item)
    }

    suspend fun deleteItem(id: Int) {
        stockDao.deleteItemById(id)
    }

    suspend fun recordMovement(itemId: Int, type: String, quantityChanged: Int, note: String): Boolean {
        return stockDao.recordMovement(itemId, type, quantityChanged, note)
    }
}
