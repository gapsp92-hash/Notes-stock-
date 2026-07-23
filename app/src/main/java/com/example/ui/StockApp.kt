package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StockItem
import com.example.data.StockTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockApp(viewModel: StockViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val itemsInSelectedCategory by viewModel.itemsInSelectedCategory.collectAsStateWithLifecycle()
    val lowStockItems by viewModel.lowStockItems.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showAdjustStockDialog by remember { mutableStateOf<StockItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<StockItem?>(null) }
    var showShareSyncDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("inventory") } // "inventory" or "history"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = activeTab == "inventory",
                    onClick = { activeTab = "inventory" },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == "inventory") Icons.Default.Inventory else Icons.Outlined.Inventory,
                            contentDescription = "Inventory"
                        )
                    },
                    label = { Text("Inventory") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("nav_inventory")
                )
                NavigationBarItem(
                    selected = activeTab == "history",
                    onClick = { activeTab = "history" },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == "history") Icons.Default.History else Icons.Outlined.History,
                            contentDescription = "Previous Data"
                        )
                    },
                    label = { Text("History Log") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("nav_history")
                )
            }
        },
        floatingActionButton = {
            if (activeTab == "inventory") {
                FloatingActionButton(
                    onClick = { showAddItemDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("add_item_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color(0xFFEFF6FF) // blue-50 accent tint
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            // Modern, Sleek Interface custom header matching Tailwind template exactly
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = 1.dp, color = Color(0xFFEFF6FF), shape = RoundedCornerShape(0.dp)) // border-blue-50
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "INVENTORY MANAGER",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "NOTES STOCK",
                        color = Color(0xFF1E293B), // slate-800
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showShareSyncDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)) // blue-50 equivalent
                            .testTag("multi_user_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Stock Data",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val report = viewModel.generateReportData()
                            shareReport(context, report)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)) // blue-50 equivalent
                            .testTag("export_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Report",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (activeTab == "inventory") {
                // Dashboard summary for low stock with dynamic total types
                LowStockSummarySection(lowStockItems = lowStockItems, totalItemsCount = allItems.size)

                Spacer(modifier = Modifier.height(12.dp))

                // Separate categories switcher
                CategorySelectorRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.selectCategory(it) },
                    categories = viewModel.categories
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Active stock list for selected category
                CategoryItemsList(
                    items = itemsInSelectedCategory,
                    onAdjustStock = { showAdjustStockDialog = it },
                    onDelete = { showDeleteConfirmDialog = it }
                )
            } else {
                // Movement history log ("previous data")
                PreviousMovementsLogSection(transactions = allTransactions)
            }
        }
    }

    // Modal dialog to add stock items
    if (showAddItemDialog) {
        AddItemDialog(
            categories = viewModel.categories,
            defaultCategory = selectedCategory,
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, category, initialQty, warningLimit, note ->
                viewModel.addItem(name, category, initialQty, warningLimit, note)
                showAddItemDialog = false
            }
        )
    }

    // Modal dialog to do record in / move out (unified stock adjustment dialog)
    showAdjustStockDialog?.let { item ->
        AdjustStockDialog(
            item = item,
            onDismiss = { showAdjustStockDialog = null },
            onConfirm = { type, qty, note ->
                if (type == "IN") {
                    viewModel.recordIn(item.id, qty, note)
                } else {
                    viewModel.recordOut(item.id, qty, note)
                }
                showAdjustStockDialog = null
            }
        )
    }

    // Modal dialog to confirm item deletion
    showDeleteConfirmDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Stock Item") },
            text = { Text("Are you sure you want to delete ${item.name}? This will remove all inventory records for this item.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item.id)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("delete_confirm_dialog")
        )
    }

    if (showShareSyncDialog) {
        ShareSyncDialog(
            viewModel = viewModel,
            onDismiss = { showShareSyncDialog = false }
        )
    }
}

// Utility to get decorative emojis per category
fun getCategoryEmoji(category: String): String = ""

// Low Stock Dashboard Panel (Sleek Interface style)
@Composable
fun LowStockSummarySection(lowStockItems: List<StockItem>, totalItemsCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("low_stock_summary_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF1D4ED8) // slightly darker sleek blue
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Stock Summary",
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Alerts: ${String.format("%02d", lowStockItems.size)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Low Stock Items Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Low Stock Items",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = lowStockItems.size.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "items",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }

                // Month Flux / Total registered items
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Total Active",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = totalItemsCount.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "types",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }
            }

            // Detailed low stock items breakdown showing CATEGORY for each item
            if (lowStockItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Low Stock Items by Category",
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    lowStockItems.take(4).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            // Category Tag Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = item.category,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${item.quantity}/${item.minLimit}",
                                color = Color(0xFFFECACA), // soft red light
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (lowStockItems.size > 4) {
                        Text(
                            text = "+ ${lowStockItems.size - 4} more low stock items",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// Category selection tabs row
@Composable
fun CategorySelectorRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    categories: List<String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            val icon = getCategoryIcon(category)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else Color(0xFFEFF6FF), // border-blue-50/slate-100 equivalent
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .testTag("category_chip_${category.replace(" ", "_")}")
            ) {
                Text(
                    text = category,
                    color = if (isSelected) Color.White else Color(0xFF64748B), // slate-500
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// Active Item list for selected category
@Composable
fun CategoryItemsList(
    items: List<StockItem>,
    onAdjustStock: (StockItem) -> Unit,
    onDelete: (StockItem) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Items Registered",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap the '+' button below to add stock items to this category.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items, key = { it.id }) { item ->
                StockItemCard(
                    item = item,
                    onAdjustStock = { onAdjustStock(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@Composable
fun StockItemCard(
    item: StockItem,
    onAdjustStock: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = item.quantity <= item.minLimit

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stock_item_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowStock) Color(0xFFFEF2F2) else Color.White // soft red-50 or white
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isLowStock) Color(0xFFFEE2E2) else Color(0xFFEFF6FF) // light borders
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text detail parameters
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B), // slate-800
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isLowStock) "Low: ${item.quantity} units (Limit: ${item.minLimit})" else "Qty: ${item.quantity} units",
                    fontSize = 11.sp,
                    color = if (isLowStock) Color(0xFFEF4444) else Color(0xFF64748B), // red-500 or slate-500
                    fontWeight = if (isLowStock) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action section row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular delete action button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFEF2F2), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Beautiful record adjustment action button
                Button(
                    onClick = onAdjustStock,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)), // blue-200 border stroke
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "LOG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// Previous stock movements log section ("previous data")
@Composable
fun PreviousMovementsLogSection(transactions: List<StockTransaction>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        Text(
            text = "PREVIOUS DATA (MOVEMENT LOG)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Previous History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "History of stock in and out movements will be cataloged here.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRow(tx = tx)
                }
            }
        }
    }
}

@Composable
fun TransactionRow(tx: StockTransaction) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val isStockIn = tx.type == "IN"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Directional arrow icon indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isStockIn) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isStockIn) Icons.Default.Add else Icons.Default.Remove,
                    contentDescription = if (isStockIn) "Stock In" else "Stock Out",
                    tint = if (isStockIn) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tx.itemName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = tx.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sdf.format(Date(tx.timestamp)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                if (tx.note.isNotEmpty()) {
                    Text(
                        text = "Note: ${tx.note}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${if (isStockIn) "+" else "-"}${tx.quantityChanged}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (isStockIn) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Text(
                    text = "Bal: ${tx.balanceAfter}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Add Item Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    categories: List<String>,
    defaultCategory: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, initialQty: Int, warnLimit: Int, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(defaultCategory) }
    var initialQtyStr by remember { mutableStateOf("") }
    var warnLimitStr by remember { mutableStateOf("5") }
    var note by remember { mutableStateOf("") }

    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_item_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Stock Item",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_item_name"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = initialQtyStr,
                        onValueChange = { initialQtyStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Initial Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_item_qty"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = warnLimitStr,
                        onValueChange = { warnLimitStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Warning Lmt") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_item_limit"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Initial Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val qty = initialQtyStr.toIntOrNull() ?: 0
                                val limit = warnLimitStr.toIntOrNull() ?: 5
                                onConfirm(name.trim(), category, qty, limit, note.trim())
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Item")
                    }
                }
            }
        }
    }
}

// Unified Stock Adjustment Dialog (Stock In / Move Out)
@Composable
fun AdjustStockDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onConfirm: (type: String, qty: Int, note: String) -> Unit
) {
    var type by remember { mutableStateOf("IN") } // "IN" or "OUT"
    var qtyStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("adjust_stock_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Record Movement: ${item.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Beautiful Toggle Button for IN / OUT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(22.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (type == "IN") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { type = "IN" }
                            .testTag("toggle_in"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Stock In (+)",
                            color = if (type == "IN") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (type == "OUT") Color(0xFFD32F2F) else Color.Transparent)
                            .clickable { type = "OUT" }
                            .testTag("toggle_out"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Move Out (-)",
                            color = if (type == "OUT") Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it.filter { char -> char.isDigit() } },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adjust_qty_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Reason (Optional)") },
                    placeholder = { Text(if (type == "IN") "e.g. Shipment restocking" else "e.g. Sold / Waste") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = qtyStr.toIntOrNull() ?: 0
                            if (qty > 0) {
                                onConfirm(type, qty, note.trim())
                            }
                        },
                        enabled = (qtyStr.toIntOrNull() ?: 0) > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "IN") MaterialTheme.colorScheme.primary else Color(0xFFD32F2F)
                        )
                    ) {
                        Text("Apply Adjustment")
                    }
                }
            }
        }
    }
}

// Modal Dialog for Multi-User Stock Synchronization & Sharing
@Composable
fun ShareSyncDialog(
    viewModel: StockViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var importJsonText by remember { mutableStateOf("") }
    var syncResultMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("share_sync_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Multi-User Stock Sync",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Share & sync inventory across devices",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Export & Share
                Text(
                    text = "1. Export & Share Stock Data",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val json = viewModel.exportStockToJson()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "NOTES STOCK Data Sync")
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Stock JSON Data"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Stock JSON Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFEFF6FF))
                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Import & Sync Data
                Text(
                    text = "2. Import / Sync Stock from Teammate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    placeholder = { Text("Paste JSON stock data received from another user...", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                )

                syncResultMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = if (msg.contains("Successfully")) Color(0xFF16A34A) else Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.importStockFromJson(importJsonText) { success, count ->
                            if (success) {
                                syncResultMsg = "Successfully synced $count items!"
                                importJsonText = ""
                            } else {
                                syncResultMsg = "Invalid JSON data. Please check format."
                            }
                        }
                    },
                    enabled = importJsonText.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync & Update Inventory", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Utility to assign gorgeous icons per category
fun getCategoryIcon(category: String): ImageVector {
    return when {
        category.contains("Dry", ignoreCase = true) -> Icons.Default.Inventory
        category.contains("Chilled", ignoreCase = true) -> Icons.Default.Kitchen
        category.contains("Frozen", ignoreCase = true) -> Icons.Default.SevereCold
        category.contains("Bar", ignoreCase = true) -> Icons.Default.LocalBar
        else -> Icons.Default.Inventory
    }
}

// Global function to trigger a share intent for the monthly inventory report
fun shareReport(context: Context, reportText: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "NOTES STOCK - Inventory Report")
        putExtra(Intent.EXTRA_TEXT, reportText)
    }
    context.startActivity(Intent.createChooser(intent, "Export Inventory Report"))
}
