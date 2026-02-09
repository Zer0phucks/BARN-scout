package com.vpt.scout.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpt.scout.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesScreen(
    propertyRepository: PropertyRepository,
    listRepository: ListRepository,
    onNavigateToScout: (city: String?, vptOnly: Boolean, listId: Long?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var propertiesResponse by remember { mutableStateOf<PropertiesResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Filters
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var vptOnly by remember { mutableStateOf(false) }
    var showUnscoutedOnly by remember { mutableStateOf(false) }
    var selectedListId by remember { mutableStateOf<Long?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    
    // Selection
    var selectedApns by remember { mutableStateOf(setOf<String>()) }
    var selectAll by remember { mutableStateOf(false) }
    
    // Lists for filter dropdown
    var lists by remember { mutableStateOf<List<PropertyList>>(emptyList()) }
    var showFiltersExpanded by remember { mutableStateOf(false) }
    var showAddToListDialog by remember { mutableStateOf(false) }
    
    // Load properties
    fun loadProperties(page: Int = 1) {
        scope.launch {
            isLoading = true
            error = null
            try {
                propertiesResponse = propertyRepository.loadProperties(
                    page = page,
                    perPage = 50,
                    city = selectedCity,
                    query = searchQuery.takeIf { it.isNotBlank() },
                    vptOnly = vptOnly,
                    scouted = if (showUnscoutedOnly) false else null,
                    listId = selectedListId
                )
                currentPage = page
            } catch (e: Exception) {
                error = e.message ?: "Failed to load properties"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Load lists
    fun loadLists() {
        scope.launch {
            try {
                lists = listRepository.refreshLists()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Initial load
    LaunchedEffect(Unit) {
        loadProperties()
        loadLists()
    }
    
    // Reload when filters change
    LaunchedEffect(selectedCity, vptOnly, showUnscoutedOnly, selectedListId) {
        loadProperties(1)
        selectedApns = emptySet()
        selectAll = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Properties") },
                actions = {
                    // Start Scout Mode button
                    IconButton(
                        onClick = { 
                            onNavigateToScout(selectedCity, vptOnly, selectedListId)
                        }
                    ) {
                        Icon(Icons.Default.DirectionsWalk, "Scout Mode")
                    }
                    // Refresh button
                    IconButton(onClick = { loadProperties(currentPage) }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            // Floating action bar when items selected
            if (selectedApns.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddToListDialog = true },
                    icon = { Icon(Icons.Default.PlaylistAdd, "Add to List") },
                    text = { Text("${selectedApns.size} selected") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter bar
            FilterBar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onSearch = { loadProperties(1) },
                selectedCity = selectedCity,
                onCityChange = { selectedCity = it },
                vptOnly = vptOnly,
                onVptOnlyChange = { vptOnly = it },
                showUnscoutedOnly = showUnscoutedOnly,
                onUnscoutedChange = { showUnscoutedOnly = it },
                lists = lists,
                selectedListId = selectedListId,
                onListChange = { selectedListId = it },
                expanded = showFiltersExpanded,
                onExpandToggle = { showFiltersExpanded = !showFiltersExpanded }
            )
            
            // Results summary
            propertiesResponse?.let { response ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${response.total} properties",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectAll,
                            onCheckedChange = { checked ->
                                selectAll = checked
                                if (checked) {
                                    selectedApns = response.properties.map { it.apn }.toSet()
                                } else {
                                    selectedApns = emptySet()
                                }
                            }
                        )
                        Text("Select all", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { loadProperties(currentPage) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {
                    propertiesResponse?.let { response ->
                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(response.properties, key = { it.apn }) { property ->
                                PropertyRow(
                                    property = property,
                                    isSelected = selectedApns.contains(property.apn),
                                    onToggleSelect = { selected ->
                                        selectedApns = if (selected) {
                                            selectedApns + property.apn
                                        } else {
                                            selectedApns - property.apn
                                        }
                                        if (!selected) selectAll = false
                                    },
                                    onNavigate = {
                                        property.latitude?.let { lat ->
                                            property.longitude?.let { lng ->
                                                val uri = Uri.parse("google.navigation:q=$lat,$lng")
                                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                                intent.setPackage("com.google.android.apps.maps")
                                                context.startActivity(intent)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        
                        // Pagination
                        if (response.totalPages > 1) {
                            PaginationBar(
                                currentPage = response.page,
                                totalPages = response.totalPages,
                                onPageChange = { loadProperties(it) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Add to List Dialog
    if (showAddToListDialog) {
        AddToListDialog(
            lists = lists,
            onDismiss = { showAddToListDialog = false },
            onCreateList = { name ->
                scope.launch {
                    try {
                        val newList = listRepository.createList(name)
                        listRepository.addPropertiesToList(newList.id, selectedApns.toList())
                        selectedApns = emptySet()
                        selectAll = false
                        loadLists()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                showAddToListDialog = false
            },
            onAddToExisting = { listId ->
                scope.launch {
                    try {
                        listRepository.addPropertiesToList(listId, selectedApns.toList())
                        selectedApns = emptySet()
                        selectAll = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                showAddToListDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    selectedCity: String?,
    onCityChange: (String?) -> Unit,
    vptOnly: Boolean,
    onVptOnlyChange: (Boolean) -> Unit,
    showUnscoutedOnly: Boolean,
    onUnscoutedChange: (Boolean) -> Unit,
    lists: List<PropertyList>,
    selectedListId: Long?,
    onListChange: (Long?) -> Unit,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search address or APN") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, "Search")
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onExpandToggle) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    "Toggle filters"
                )
            }
        }
        
        // Expanded filters
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // City filter
                var cityDropdownExpanded by remember { mutableStateOf(false) }
                val cities = listOf("ALAMEDA", "ALBANY", "BERKELEY", "EMERYVILLE", "OAKLAND")
                
                ExposedDropdownMenuBox(
                    expanded = cityDropdownExpanded,
                    onExpandedChange = { cityDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedCity ?: "All Cities",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityDropdownExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = cityDropdownExpanded,
                        onDismissRequest = { cityDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Cities") },
                            onClick = {
                                onCityChange(null)
                                cityDropdownExpanded = false
                            }
                        )
                        cities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city) },
                                onClick = {
                                    onCityChange(city)
                                    cityDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // List filter
                if (lists.isNotEmpty()) {
                    var listDropdownExpanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = listDropdownExpanded,
                        onExpandedChange = { listDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = lists.find { it.id == selectedListId }?.name ?: "All Lists",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = listDropdownExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = listDropdownExpanded,
                            onDismissRequest = { listDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Lists") },
                                onClick = {
                                    onListChange(null)
                                    listDropdownExpanded = false
                                }
                            )
                            lists.forEach { list ->
                                DropdownMenuItem(
                                    text = { Text("${list.name} (${list.propertyCount})") },
                                    onClick = {
                                        onListChange(list.id)
                                        listDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vptOnly, onCheckedChange = onVptOnlyChange)
                    Text("VPT Only", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showUnscoutedOnly, onCheckedChange = onUnscoutedChange)
                    Text("Unscouted", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(
    property: Property,
    isSelected: Boolean,
    onToggleSelect: (Boolean) -> Unit,
    onNavigate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onToggleSelect(!isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = onToggleSelect
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Property info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    property.address ?: property.apn,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        property.city ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (property.hasVpt) {
                        Badge(containerColor = Color(0xFFE53935)) {
                            Text("VPT", fontSize = 10.sp)
                        }
                    }
                    property.conditionScore?.let { score ->
                        val color = when {
                            score >= 7 -> Color(0xFF4CAF50)
                            score >= 4 -> Color(0xFFFFC107)
                            else -> Color(0xFFE53935)
                        }
                        Badge(containerColor = color) {
                            Text("${"%.0f".format(score)}/10", fontSize = 10.sp)
                        }
                    }
                    if (property.isScouted) {
                        Badge(containerColor = Color(0xFF9E9E9E)) {
                            Text("Scouted", fontSize = 10.sp)
                        }
                    }
                }
            }
            
            // Navigate button
            IconButton(onClick = onNavigate) {
                Icon(Icons.Default.Navigation, "Navigate")
            }
        }
    }
}

@Composable
private fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
            enabled = currentPage > 1
        ) {
            Icon(Icons.Default.ChevronLeft, "Previous")
        }
        
        Text(
            "Page $currentPage of $totalPages",
            style = MaterialTheme.typography.bodyMedium
        )
        
        IconButton(
            onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
            enabled = currentPage < totalPages
        ) {
            Icon(Icons.Default.ChevronRight, "Next")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToListDialog(
    lists: List<PropertyList>,
    onDismiss: () -> Unit,
    onCreateList: (String) -> Unit,
    onAddToExisting: (Long) -> Unit
) {
    var newListName by remember { mutableStateOf("") }
    var showCreateNew by remember { mutableStateOf(lists.isEmpty()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to List") },
        text = {
            Column {
                if (showCreateNew) {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("New list name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Choose a list:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    lists.forEach { list ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onAddToExisting(list.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(list.name, modifier = Modifier.weight(1f))
                                Text(
                                    "${list.propertyCount} properties",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showCreateNew = true }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Create new list")
                    }
                }
            }
        },
        confirmButton = {
            if (showCreateNew) {
                TextButton(
                    onClick = { if (newListName.isNotBlank()) onCreateList(newListName) },
                    enabled = newListName.isNotBlank()
                ) {
                    Text("Create & Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
