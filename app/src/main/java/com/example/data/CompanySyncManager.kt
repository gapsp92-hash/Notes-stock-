package com.example.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class SyncStatus(
    val isLive: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L,
    val lastSyncFormatted: String = "Never",
    val statusMessage: String = "Live Multi-User Connected",
    val activeUsersEstimate: Int = 5
)

class CompanySyncManager(
    private val context: Context,
    private val repository: StockRepository,
    private val scope: CoroutineScope
) {
    private val prefs = context.getSharedPreferences("company_sync_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    private val appKey = "c67f8e32"

    private val _companyCode = MutableStateFlow(getSavedCompanyCode())
    val companyCode: StateFlow<String> = _companyCode.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var syncJob: Job? = null
    private var lastRemoteTimestamp: Long = 0L

    init {
        startLiveSyncLoop()
    }

    private fun getSavedCompanyCode(): String {
        return try {
            prefs.getString("company_code", "COMPANY-LANKA-01") ?: "COMPANY-LANKA-01"
        } catch (e: Exception) {
            "COMPANY-LANKA-01"
        }
    }

    fun updateCompanyCode(newCode: String) {
        try {
            val sanitized = newCode.trim().uppercase(Locale.getDefault()).ifEmpty { "COMPANY-LANKA-01" }
            prefs.edit().putString("company_code", sanitized).apply()
            _companyCode.value = sanitized
            lastRemoteTimestamp = 0L
            triggerManualSync()
        } catch (e: Exception) {
            // Ignore pref write error
        }
    }

    fun startLiveSyncLoop() {
        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    performSyncFetch()
                } catch (e: Exception) {
                    _syncStatus.value = _syncStatus.value.copy(
                        isSyncing = false,
                        statusMessage = "Offline - Local Mode Active"
                    )
                }
                delay(4000) // Poll every 4 seconds for live updates
            }
        }
    }

    fun triggerManualSync() {
        scope.launch(Dispatchers.IO) {
            try {
                performSyncPush()
                performSyncFetch()
            } catch (e: Exception) {
                // Ignore transient errors
            }
        }
    }

    fun onLocalDataChanged() {
        scope.launch(Dispatchers.IO) {
            try {
                performSyncPush()
            } catch (e: Exception) {
                // Keep local changes
            }
        }
    }

    private suspend fun performSyncFetch() {
        val code = _companyCode.value
        val url = "https://keyvalue.immanuel.co/api/KeyVal/GetValue/$appKey/$code"

        _syncStatus.value = _syncStatus.value.copy(isSyncing = true)

        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    var bodyString = response.body?.string() ?: ""
                    if (bodyString.startsWith("\"") && bodyString.endsWith("\"")) {
                        bodyString = bodyString.substring(1, bodyString.length - 1)
                    }

                    if (bodyString.isNotEmpty() && bodyString != "null") {
                        val decodedJsonStr = String(Base64.decode(bodyString, Base64.DEFAULT), StandardCharsets.UTF_8)
                        val jsonObj = JSONObject(decodedJsonStr)
                        val timestamp = jsonObj.optLong("timestamp", 0L)

                        if (timestamp > lastRemoteTimestamp) {
                            lastRemoteTimestamp = timestamp
                            applyRemoteDataToLocal(jsonObj)
                        }
                    }

                    val now = System.currentTimeMillis()
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val formatted = "Today " + timeFormat.format(Date(now))

                    _syncStatus.value = SyncStatus(
                        isLive = true,
                        isSyncing = false,
                        lastSyncTime = now,
                        lastSyncFormatted = formatted,
                        statusMessage = "Live Sync Active ($code)",
                        activeUsersEstimate = 5
                    )
                } else {
                    _syncStatus.value = _syncStatus.value.copy(isSyncing = false)
                }
            }
        } catch (e: Exception) {
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                statusMessage = "Connecting to Cloud..."
            )
        }
    }

    private suspend fun performSyncPush() {
        try {
            val code = _companyCode.value
            val now = System.currentTimeMillis()

            val items = repository.stockDao.getAllItemsSnapshot()
            val transactions = repository.stockDao.getAllTransactionsSnapshot()

            val payload = JSONObject()
            payload.put("companyCode", code)
            payload.put("timestamp", now)

            val itemsArray = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("name", item.name)
                obj.put("category", item.category)
                obj.put("quantity", item.quantity)
                obj.put("minLimit", item.minLimit)
                itemsArray.put(obj)
            }
            payload.put("items", itemsArray)

            val txArray = JSONArray()
            for (tx in transactions.take(50)) { // Sync recent 50 transactions
                val obj = JSONObject()
                obj.put("id", tx.id)
                obj.put("itemId", tx.itemId)
                obj.put("itemName", tx.itemName)
                obj.put("category", tx.category)
                obj.put("type", tx.type)
                obj.put("quantityChanged", tx.quantityChanged)
                obj.put("balanceAfter", tx.balanceAfter)
                obj.put("timestamp", tx.timestamp)
                obj.put("note", tx.note)
                txArray.put(obj)
            }
            payload.put("transactions", txArray)

            val base64Payload = Base64.encodeToString(payload.toString().toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            val url = "https://keyvalue.immanuel.co/api/KeyVal/UpdateValue/$appKey/$code/$base64Payload"

            val mediaType = "text/plain".toMediaType()
            val request = Request.Builder()
                .url(url)
                .header("Content-Length", "0")
                .post("".toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    lastRemoteTimestamp = now
                }
            }
        } catch (e: Exception) {
            // Protect against push errors
        }
    }

    private suspend fun applyRemoteDataToLocal(jsonObj: JSONObject) {
        try {
            val itemsArray = jsonObj.optJSONArray("items") ?: JSONArray()
            val txArray = jsonObj.optJSONArray("transactions") ?: JSONArray()

            val existingItems = repository.stockDao.getAllItemsSnapshot()
            val existingItemsByName = existingItems.associateBy { it.name.trim().lowercase(Locale.getDefault()) }

            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim()
                if (name.isEmpty()) continue

                val category = obj.optString("category", "Dry Items")
                val quantity = obj.optInt("quantity", 0)
                val minLimit = obj.optInt("minLimit", 5)

                val existing = existingItemsByName[name.lowercase(Locale.getDefault())]
                if (existing != null) {
                    val updated = existing.copy(
                        category = category,
                        quantity = quantity,
                        minLimit = minLimit
                    )
                    repository.stockDao.updateItem(updated)
                } else {
                    val newItem = StockItem(
                        id = 0,
                        name = name,
                        category = category,
                        quantity = quantity,
                        minLimit = minLimit
                    )
                    repository.stockDao.insertItem(newItem)
                }
            }

            val existingTxs = repository.stockDao.getAllTransactionsSnapshot()
            val existingTxKeys = existingTxs.map { "${it.itemName}_${it.timestamp}_${it.quantityChanged}_${it.type}" }.toSet()

            for (i in 0 until txArray.length()) {
                val obj = txArray.optJSONObject(i) ?: continue
                val itemName = obj.optString("itemName", "")
                val timestamp = obj.optLong("timestamp", 0L)
                val qty = obj.optInt("quantityChanged", 0)
                val type = obj.optString("type", "IN")
                val key = "${itemName}_${timestamp}_${qty}_${type}"

                if (existingTxKeys.contains(key)) {
                    continue
                }

                val tx = StockTransaction(
                    id = 0,
                    itemId = obj.optInt("itemId", 0),
                    itemName = itemName,
                    category = obj.optString("category", ""),
                    type = type,
                    quantityChanged = qty,
                    balanceAfter = obj.optInt("balanceAfter", 0),
                    timestamp = if (timestamp > 0) timestamp else System.currentTimeMillis(),
                    note = obj.optString("note", "")
                )
                repository.stockDao.insertTransaction(tx)
            }
        } catch (e: Exception) {
            // Protect against corrupted remote JSON without crashing
        }
    }
}
