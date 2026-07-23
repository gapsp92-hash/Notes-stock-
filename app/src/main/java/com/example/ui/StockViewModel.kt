package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.StockItem
import com.example.data.StockRepository
import com.example.data.StockTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class StockViewModel(private val repository: StockRepository) : ViewModel() {

    val categories = listOf("Dry Items", "Chilled Items", "Frozen Items", "Bar Items", "Packaging Items", "Chemical Items")

    private val _selectedCategory = MutableStateFlow("Dry Items")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val allItems: StateFlow<List<StockItem>> = repository.allItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lowStockItems: StateFlow<List<StockItem>> = repository.lowStockItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTransactions: StateFlow<List<StockTransaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive flow of items belonging to the selected category
    val itemsInSelectedCategory: StateFlow<List<StockItem>> = _selectedCategory
        .flatMapLatest { category ->
            repository.getItemsByCategory(category)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addItem(name: String, category: String, quantity: Int, minLimit: Int, note: String) {
        viewModelScope.launch {
            val newItem = StockItem(
                name = name,
                category = category,
                quantity = quantity,
                minLimit = minLimit
            )
            val id = repository.insertItem(newItem)
            // Log initial transaction if quantity > 0
            if (quantity > 0) {
                repository.recordMovement(
                    itemId = id.toInt(),
                    type = "IN",
                    quantityChanged = quantity,
                    note = if (note.isEmpty()) "Initial stock" else note
                )
            }
        }
    }

    fun recordIn(itemId: Int, quantity: Int, note: String) {
        viewModelScope.launch {
            repository.recordMovement(itemId, "IN", quantity, note)
        }
    }

    fun recordOut(itemId: Int, quantity: Int, note: String) {
        viewModelScope.launch {
            repository.recordMovement(itemId, "OUT", quantity, note)
        }
    }

    fun updateItem(item: StockItem) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun deleteItem(itemId: Int) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
        }
    }

    // Export entire inventory to JSON string for multi-user sharing & sync
    fun exportStockToJson(): String {
        val array = JSONArray()
        for (item in allItems.value) {
            val obj = JSONObject()
            obj.put("name", item.name)
            obj.put("category", item.category)
            obj.put("quantity", item.quantity)
            obj.put("minLimit", item.minLimit)
            array.put(obj)
        }
        return array.toString(2)
    }

    // Import inventory from JSON string (sync items from another user/device)
    fun importStockFromJson(jsonString: String, onComplete: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                val array = JSONArray(jsonString.trim())
                var count = 0
                val existing = allItems.value.associateBy { it.name.lowercase(Locale.getDefault()) }
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val name = obj.getString("name")
                    val category = obj.optString("category", "Dry Items")
                    val quantity = obj.optInt("quantity", 0)
                    val minLimit = obj.optInt("minLimit", 5)

                    val existingItem = existing[name.lowercase(Locale.getDefault())]
                    if (existingItem != null) {
                        repository.updateItem(
                            existingItem.copy(
                                category = category,
                                quantity = quantity,
                                minLimit = minLimit
                            )
                        )
                    } else {
                        val newItem = StockItem(name = name, category = category, quantity = quantity, minLimit = minLimit)
                        val newId = repository.insertItem(newItem)
                        if (quantity > 0) {
                            repository.recordMovement(newId.toInt(), "IN", quantity, "Synced from shared stock")
                        }
                    }
                    count++
                }
                onComplete(true, count)
            } catch (e: Exception) {
                onComplete(false, 0)
            }
        }
    }

    // Generate monthly report CSV / Text content
    fun generateReportData(): String {
        val currentItems = allItems.value
        val transactions = allTransactions.value

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val generatedAt = sdf.format(Date())

        val builder = StringBuilder()
        builder.append("=========================================\n")
        builder.append("   NOTES STOCK - MONTHLY INVENTORY REPORT\n")
        builder.append("=========================================\n")
        builder.append("Generated At: $generatedAt\n\n")

        builder.append("--- CURRENT STOCK LEVEL SUMMARY ---\n")
        builder.append(String.format("%-25s | %-15s | %-8s | %-8s | %s\n", "Item Name", "Category", "Qty", "Min Lmt", "Status"))
        builder.append("---------------------------------------------------------------------------------\n")
        for (item in currentItems) {
            val status = if (item.quantity <= item.minLimit) "⚠️ LOW STOCK" else "✅ OK"
            builder.append(String.format("%-25s | %-15s | %-8d | %-8d | %s\n", 
                if (item.name.length > 25) item.name.take(22) + "..." else item.name,
                item.category,
                item.quantity,
                item.minLimit,
                status
            ))
        }
        builder.append("\n")

        builder.append("--- PREVIOUS STOCK MOVEMENTS HISTORY ---\n")
        builder.append(String.format("%-19s | %-20s | %-12s | %-4s | %-5s | %-5s | %s\n", 
            "Timestamp", "Item Name", "Category", "Type", "Chg", "Bal", "Notes"
        ))
        builder.append("----------------------------------------------------------------------------------------------------\n")
        for (tx in transactions) {
            val txDate = sdf.format(Date(tx.timestamp))
            builder.append(String.format("%-19s | %-20s | %-12s | %-4s | %-5d | %-5d | %s\n",
                txDate,
                if (tx.itemName.length > 20) tx.itemName.take(17) + "..." else tx.itemName,
                tx.category,
                tx.type,
                tx.quantityChanged,
                tx.balanceAfter,
                tx.note
            ))
        }

        return builder.toString()
    }

    // Factory to instantiate ViewModel with Repository
    class Factory(private val repository: StockRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StockViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StockViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
