package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StockItem
import com.example.data.StockTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CategoryTheme(
    val backgroundColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val badgeBgColor: Color,
    val iconColor: Color
)

fun getCategoryTheme(category: String): CategoryTheme {
    return when {
        category.contains("Dry", ignoreCase = true) -> CategoryTheme(
            backgroundColor = Color(0xFFFEF3C7), // warm amber-100
            borderColor = Color(0xFFFDE68A),     // amber-200
            textColor = Color(0xFF92400E),       // amber-800
            badgeBgColor = Color(0xFFF59E0B),    // amber-500
            iconColor = Color(0xFFD97706)        // amber-600
        )
        category.contains("Chilled", ignoreCase = true) -> CategoryTheme(
            backgroundColor = Color(0xFFE0F2FE), // cyan/sky-100
            borderColor = Color(0xFFBAE6FD),     // sky-200
            textColor = Color(0xFF0369A1),       // sky-800
            badgeBgColor = Color(0xFF0EA5E9),    // sky-500
            iconColor = Color(0xFF0284C7)        // sky-600
        )
        category.contains("Frozen", ignoreCase = true) -> CategoryTheme(
            backgroundColor = Color(0xFFE0E7FF), // indigo-100
            borderColor = Color(0xFFC7D2FE),     // indigo-200
            textColor = Color(0xFF3730A3),       // indigo-800
            badgeBgColor = Color(0xFF6366F1),    // indigo-500
            iconColor = Color(0xFF4F46E5)        // indigo-600
        )
        category.contains("Bar", ignoreCase = true) -> CategoryTheme(
            backgroundColor = Color(0xFFFFE4E6), // rose/wine-100
            borderColor = Color(0xFFFECDD3),     // rose-200
            textColor = Color(0xFF9F1239),       // rose-800
            badgeBgColor = Color(0xFFF43F5E),    // rose-500
            iconColor = Color(0xFFE11D48)        // rose-600
        )
        category.contains("Packaging", ignoreCase = true) -> CategoryTheme(
            backgroundColor = Color(0xFFD1FAE5), // emerald/green-100
            borderColor = Color(0xFFA7F3D0),     // emerald-200
            textColor = Color(0xFF065F46),       // emerald-800
            badgeBgColor = Color(0xFF10B981),    // emerald-500
            iconColor = Color(0xFF059669)        // emerald-600
        )
        category.contains("Chemical", ignoreCase = true) -> CategoryTheme(
            backgroundColor = Color(0xFFF3E8FF), // purple-100
            borderColor = Color(0xFFE9D5FF),     // purple-200
            textColor = Color(0xFF6B21A8),       // purple-800
            badgeBgColor = Color(0xFFA855F7),    // purple-500
            iconColor = Color(0xFF9333EA)        // purple-600
        )
        else -> CategoryTheme(
            backgroundColor = Color(0xFFF1F5F9), // slate-100
            borderColor = Color(0xFFE2E8F0),     // slate-200
            textColor = Color(0xFF334155),       // slate-700
            badgeBgColor = Color(0xFF64748B),    // slate-500
            iconColor = Color(0xFF475569)        // slate-600
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockApp(viewModel: StockViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val itemsInSelectedCategory by viewModel.itemsInSelectedCategory.collectAsStateWithLifecycle()
    val lowStockItems by viewModel.lowStockItems.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val companyCode by viewModel.companyCode.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showAdjustStockDialog by remember { mutableStateOf<StockItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<StockItem?>(null) }
    var showShareSyncDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("home") } // "home", "inventory" or "history"
    var selectedCategoryForDetail by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = activeTab == "home",
                    onClick = {
                        activeTab = "home"
                    },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == "home") Icons.Default.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
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
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.notes_app_logo),
                        contentDescription = "NOTES Logo",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "INVENTORY MANAGER",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "NOTES STOCK",
                            color = Color(0xFF1E293B), // slate-800
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        )
                    }
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

            // Live Multi-User Company Sync Indicator Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showShareSyncDialog = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFECFDF5), // emerald-50
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)) // emerald-200
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981)) // emerald-500 live dot
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "LIVE SYNC: $companyCode",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "5 Company Devices Linked • Auto Updating",
                                fontSize = 10.sp,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD1FAE5))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIVE",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                }
            }

            when (activeTab) {
                "home" -> {
                    HomeScreen(
                        lowStockItems = lowStockItems,
                        totalItemsCount = allItems.size,
                        allTransactions = allTransactions,
                        categories = viewModel.categories,
                        allItems = allItems,
                        onSelectCategory = { cat ->
                            viewModel.selectCategory(cat)
                            selectedCategoryForDetail = cat
                            activeTab = "inventory"
                        },
                        onViewAllTransactions = {
                            activeTab = "history"
                        }
                    )
                }
                "inventory" -> {
                    if (selectedCategoryForDetail == null) {
                        CategoryCardList(
                            categories = viewModel.categories,
                            allItems = allItems,
                            onCategoryClick = { category ->
                                viewModel.selectCategory(category)
                                selectedCategoryForDetail = category
                            }
                        )
                    } else {
                        val currentCategory = selectedCategoryForDetail!!
                        CategoryDetailView(
                            category = currentCategory,
                            items = itemsInSelectedCategory,
                            onBack = { selectedCategoryForDetail = null },
                            onAdjustStock = { showAdjustStockDialog = it },
                            onDelete = { showDeleteConfirmDialog = it },
                            onAddItem = { showAddItemDialog = true }
                        )
                    }
                }
                "history" -> {
                    PreviousMovementsLogSection(transactions = allTransactions)
                }
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

// Home Screen showing overview dashboard, low stock summary & recent movements
@Composable
fun HomeScreen(
    lowStockItems: List<StockItem>,
    totalItemsCount: Int,
    allTransactions: List<StockTransaction>,
    categories: List<String>,
    allItems: List<StockItem>,
    onSelectCategory: (String) -> Unit,
    onViewAllTransactions: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Low Stock Alert Dashboard Panel
        item {
            LowStockSummarySection(
                lowStockItems = lowStockItems,
                totalItemsCount = totalItemsCount
            )
        }

        // Daily Movements & Activity Log Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DAILY MOVEMENTS",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Recent Stock In & Out Activity",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    TextButton(onClick = onViewAllTransactions) {
                        Text("View All", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (allTransactions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEFF6FF))
                    ) {
                        Text(
                            text = "No recent stock movements recorded today.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(20.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allTransactions.take(5).forEach { tx ->
                            TransactionRow(tx = tx)
                        }
                    }
                }
            }
        }
    }
}

// Category selection list with clean vertical color box cards
@Composable
fun CategoryCardList(
    categories: List<String>,
    allItems: List<StockItem>,
    onCategoryClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "INVENTORY CATEGORIES",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Select Category to View Items",
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }

        items(categories) { category ->
            val theme = getCategoryTheme(category)
            val icon = getCategoryIcon(category)
            val catItems = allItems.filter { it.category.equals(category, ignoreCase = true) }
            val lowStockCount = catItems.count { it.quantity <= it.minLimit }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoryClick(category) }
                    .testTag("category_card_${category.replace(" ", "_")}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
                border = BorderStroke(1.dp, theme.borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(theme.badgeBgColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = theme.iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = category,
                                color = theme.textColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${catItems.size} items registered",
                                color = theme.textColor.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (lowStockCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF4444))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$lowStockCount Low",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open $category",
                            tint = theme.textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// Category Detail View showing item names & inventory inside selected category
@Composable
fun CategoryDetailView(
    category: String,
    items: List<StockItem>,
    onBack: () -> Unit,
    onAdjustStock: (StockItem) -> Unit,
    onDelete: (StockItem) -> Unit,
    onAddItem: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = items.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Back Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Categories",
                        tint = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = category,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${items.size} registered items",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Button(
                onClick = onAddItem,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search $category items...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true
        )

        // Item List
        CategoryItemsList(
            items = filteredItems,
            onAdjustStock = onAdjustStock,
            onDelete = onDelete
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

// Modal Dialog for Multi-User Live Stock Synchronization & Sharing
@Composable
fun ShareSyncDialog(
    viewModel: StockViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val companyCode by viewModel.companyCode.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    var inputCode by remember { mutableStateOf(companyCode) }
    var codeSavedToast by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("share_sync_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFECFDF5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Live Company Sync",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Shared live stock for 5 company users",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Sync Status Indicator Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF0FDF4))
                        .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Auto Sync Active (5 Users)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "Last updated: ${syncStatus.lastSyncFormatted}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.triggerManualSync() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Now",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Company Code Setup Field
                Text(
                    text = "COMPANY SYNC CODE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = inputCode,
                    onValueChange = { inputCode = it.uppercase(Locale.getDefault()) },
                    placeholder = { Text("e.g. LANKA-FOODS-01", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.updateCompanyCode(inputCode)
                            Toast.makeText(context, "Company Code Updated & Connected!", Toast.LENGTH_SHORT).show()
                            codeSavedToast = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save Code",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Copy Code and Share Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Company Code", companyCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Company Code copied: $companyCode", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val shareMessage = "Join our company live stock inventory on NOTES STOCK App!\n\n" +
                                    "🔑 Company Sync Code: $companyCode\n\n" +
                                    "Enter this code in your app to view live stock movements automatically across all 5 company phones!"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "NOTES STOCK Live Company Code")
                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Code with 5 Teammates"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // English Instructions Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "How 5 Users Connect Live:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. All 5 users in your company enter this same Company Code ($companyCode) in their app settings.\n" +
                                    "2. Whenever anyone adds, removes, or updates stock items, all 5 company devices synchronize automatically in real-time!",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.updateCompanyCode(inputCode)
                        viewModel.triggerManualSync()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect & Start Live Sync", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
        category.contains("Packaging", ignoreCase = true) -> Icons.Default.AllInbox
        category.contains("Chemical", ignoreCase = true) -> Icons.Default.Science
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
