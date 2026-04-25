package com.vpt.scout.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vpt.scout.ListRepository
import com.vpt.scout.PropertiesResponse
import com.vpt.scout.Property
import com.vpt.scout.PropertyList
import com.vpt.scout.PropertyRepository
import com.vpt.scout.proximity.ProximityAlertPreferences
import kotlinx.coroutines.launch

private enum class ExploreMode { LIST, GALLERY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesScreen(
    propertyRepository: PropertyRepository,
    listRepository: ListRepository,
    proximityAlertPreferences: ProximityAlertPreferences,
    requestProximityPermissions: ((Boolean) -> Unit) -> Unit,
    onNavigateToScout: (city: String?, vptOnly: Boolean, listId: Long?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var propertiesResponse by remember { mutableStateOf<PropertiesResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var exploreMode by remember { mutableStateOf(ExploreMode.LIST) }
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var zipFilter by remember { mutableStateOf("") }
    var powerFilter by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var vptOnly by remember { mutableStateOf(false) }
    var delinquentOnly by remember { mutableStateOf(false) }
    var conditionFilter by remember { mutableStateOf("") }
    var outOfStateOnly by remember { mutableStateOf(false) }
    var researchFilter by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("location_of_property") }
    var sortOrder by remember { mutableStateOf("asc") }
    var showUnscoutedOnly by remember { mutableStateOf(false) }
    var selectedListId by remember { mutableStateOf<Long?>(null) }
    var currentPage by remember { mutableStateOf(1) }

    var selectedApns by remember { mutableStateOf(setOf<String>()) }
    var selectAll by remember { mutableStateOf(false) }

    var lists by remember { mutableStateOf<List<PropertyList>>(emptyList()) }
    var showFiltersExpanded by remember { mutableStateOf(false) }
    var showAddToListDialog by remember { mutableStateOf(false) }

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
                    zip = zipFilter.takeIf { it.isNotBlank() },
                    power = powerFilter.takeIf { it.isNotBlank() },
                    favoritesOnly = favoritesOnly,
                    vptOnly = vptOnly,
                    delinquentOnly = delinquentOnly,
                    condition = conditionFilter.takeIf { it.isNotBlank() },
                    outOfStateOnly = outOfStateOnly,
                    research = researchFilter.takeIf { it.isNotBlank() },
                    ownerName = ownerName.takeIf { it.isNotBlank() },
                    sort = sortBy,
                    order = sortOrder,
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

    fun loadLists() {
        scope.launch {
            try {
                lists = listRepository.refreshLists()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(Unit) {
        loadProperties()
        loadLists()
    }

    LaunchedEffect(
        selectedCity,
        zipFilter,
        powerFilter,
        favoritesOnly,
        vptOnly,
        delinquentOnly,
        conditionFilter,
        outOfStateOnly,
        researchFilter,
        ownerName,
        sortBy,
        sortOrder,
        showUnscoutedOnly,
        selectedListId
    ) {
        loadProperties(1)
        selectedApns = emptySet()
        selectAll = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore") },
                actions = {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = exploreMode == ExploreMode.LIST,
                            onClick = { exploreMode = ExploreMode.LIST },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("List")
                        }
                        SegmentedButton(
                            selected = exploreMode == ExploreMode.GALLERY,
                            onClick = { exploreMode = ExploreMode.GALLERY },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Gallery")
                        }
                    }
                    IconButton(onClick = { loadProperties(currentPage) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedApns.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddToListDialog = true },
                    icon = { Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to route") },
                    text = { Text("${selectedApns.size} selected") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ProximityAlertsSection(
                proximityAlertPreferences = proximityAlertPreferences,
                requestProximityPermissions = requestProximityPermissions,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            FilterBar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onSearch = { loadProperties(1) },
                selectedCity = selectedCity,
                onCityChange = { selectedCity = it },
                zipFilter = zipFilter,
                onZipFilterChange = { zipFilter = it },
                powerFilter = powerFilter,
                onPowerFilterChange = { powerFilter = it },
                favoritesOnly = favoritesOnly,
                onFavoritesOnlyChange = { favoritesOnly = it },
                vptOnly = vptOnly,
                onVptOnlyChange = { vptOnly = it },
                delinquentOnly = delinquentOnly,
                onDelinquentOnlyChange = { delinquentOnly = it },
                conditionFilter = conditionFilter,
                onConditionFilterChange = { conditionFilter = it },
                outOfStateOnly = outOfStateOnly,
                onOutOfStateOnlyChange = { outOfStateOnly = it },
                researchFilter = researchFilter,
                onResearchFilterChange = { researchFilter = it },
                ownerName = ownerName,
                onOwnerNameChange = { ownerName = it },
                sortBy = sortBy,
                onSortByChange = { sortBy = it },
                sortOrder = sortOrder,
                onSortOrderChange = { sortOrder = it },
                showUnscoutedOnly = showUnscoutedOnly,
                onUnscoutedChange = { showUnscoutedOnly = it },
                lists = lists,
                selectedListId = selectedListId,
                onListChange = { selectedListId = it },
                expanded = showFiltersExpanded,
                onExpandToggle = { showFiltersExpanded = !showFiltersExpanded }
            )

            ScoutActionButtons(
                selectedListId = selectedListId,
                favoritesOnly = favoritesOnly,
                onScout = { onNavigateToScout(selectedCity, vptOnly, null) },
                onScoutByList = {
                    selectedListId?.let { listId ->
                        onNavigateToScout(selectedCity, vptOnly, listId)
                    }
                }
            )

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
                                    scope.launch {
                                        try {
                                            selectedApns = propertyRepository.loadAllPropertyApns(
                                                city = selectedCity,
                                                query = searchQuery.takeIf { it.isNotBlank() },
                                                zip = zipFilter.takeIf { it.isNotBlank() },
                                                power = powerFilter.takeIf { it.isNotBlank() },
                                                favoritesOnly = favoritesOnly,
                                                vptOnly = vptOnly,
                                                delinquentOnly = delinquentOnly,
                                                condition = conditionFilter.takeIf { it.isNotBlank() },
                                                outOfStateOnly = outOfStateOnly,
                                                research = researchFilter.takeIf { it.isNotBlank() },
                                                ownerName = ownerName.takeIf { it.isNotBlank() },
                                                sort = sortBy,
                                                order = sortOrder,
                                                scouted = if (showUnscoutedOnly) false else null,
                                                listId = selectedListId
                                            )
                                        } catch (_: Exception) {
                                            selectedApns = response.properties.map { it.apn }.toSet()
                                        }
                                    }
                                } else {
                                    selectedApns = emptySet()
                                }
                            }
                        )
                        Text("Select all (${response.total})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(response.properties, key = { it.apn }) { property ->
                                val onToggle: (Boolean) -> Unit = { selected ->
                                    selectedApns = if (selected) selectedApns + property.apn else selectedApns - property.apn
                                    if (!selected) selectAll = false
                                }
                                val onNavigate: () -> Unit = {
                                    property.latitude?.let { lat ->
                                        property.longitude?.let { lng ->
                                            val uri = Uri.parse("google.navigation:q=$lat,$lng")
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            intent.setPackage("com.google.android.apps.maps")
                                            context.startActivity(intent)
                                        }
                                    }
                                }

                                if (exploreMode == ExploreMode.LIST) {
                                    PropertyRow(
                                        property = property,
                                        isSelected = selectedApns.contains(property.apn),
                                        onToggleSelect = onToggle,
                                        onNavigate = onNavigate
                                    )
                                } else {
                                    GalleryPropertyCard(
                                        property = property,
                                        isSelected = selectedApns.contains(property.apn),
                                        onToggleSelect = onToggle,
                                        onNavigate = onNavigate
                                    )
                                }
                            }
                        }

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

    if (showAddToListDialog) {
        AddToListDialog(
            lists = lists,
            onDismiss = { showAddToListDialog = false },
            onCreateList = { name ->
                scope.launch {
                    try {
                        val list = listRepository.createList(name)
                        listRepository.addPropertiesToList(list.id, selectedApns.toList())
                        loadLists()
                    } finally {
                        selectedApns = emptySet()
                        selectAll = false
                    }
                }
                showAddToListDialog = false
            },
            onAddToExisting = { listId ->
                scope.launch {
                    try {
                        listRepository.addPropertiesToList(listId, selectedApns.toList())
                    } finally {
                        selectedApns = emptySet()
                        selectAll = false
                    }
                }
                showAddToListDialog = false
            }
        )
    }
}

@Composable
private fun ScoutActionButtons(
    selectedListId: Long?,
    favoritesOnly: Boolean,
    onScout: () -> Unit,
    onScoutByList: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onScout,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scout")
            }

            Button(
                onClick = onScoutByList,
                enabled = selectedListId != null,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Route, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scout Route")
            }
        }

        Text(
            text = if (favoritesOnly) "Scout uses the favorites filter." else "Scout uses the current filters.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (selectedListId == null) {
            Text(
                text = "Select a route to enable route scouting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    selectedCity: String?,
    onCityChange: (String?) -> Unit,
    zipFilter: String,
    onZipFilterChange: (String) -> Unit,
    powerFilter: String,
    onPowerFilterChange: (String) -> Unit,
    favoritesOnly: Boolean,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    vptOnly: Boolean,
    onVptOnlyChange: (Boolean) -> Unit,
    delinquentOnly: Boolean,
    onDelinquentOnlyChange: (Boolean) -> Unit,
    conditionFilter: String,
    onConditionFilterChange: (String) -> Unit,
    outOfStateOnly: Boolean,
    onOutOfStateOnlyChange: (Boolean) -> Unit,
    researchFilter: String,
    onResearchFilterChange: (String) -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    sortOrder: String,
    onSortOrderChange: (String) -> Unit,
    showUnscoutedOnly: Boolean,
    onUnscoutedChange: (Boolean) -> Unit,
    lists: List<PropertyList>,
    selectedListId: Long?,
    onListChange: (Long?) -> Unit,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    val cities = listOf("ALAMEDA", "ALBANY", "BERKELEY", "EMERYVILLE", "OAKLAND")
    val sortOptions = listOf(
        "location_of_property" to "Address",
        "condition_score" to "Condition",
        "city" to "City",
        "added_at" to "Added"
    )
    val conditionOptions = listOf("" to "Any", "good" to "Good", "fair" to "Fair", "poor" to "Poor")
    val researchOptions = listOf("" to "Any", "completed" to "Complete", "unchecked" to "Unchecked")
    val powerOptions = listOf("" to "Any", "on" to "On", "off" to "Off", "unknown" to "Unknown")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search address or APN") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onExpandToggle) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle filters"
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SimpleDropdownField(
                    label = "City",
                    selectedLabel = selectedCity ?: "All cities",
                    options = listOf("" to "All cities") + cities.map { it to it },
                    onSelected = { onCityChange(it.takeIf(String::isNotBlank)) },
                    modifier = Modifier.width(170.dp)
                )
                OutlinedTextField(
                    value = zipFilter,
                    onValueChange = onZipFilterChange,
                    label = { Text("Zip") },
                    singleLine = true,
                    modifier = Modifier.width(120.dp)
                )
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = onOwnerNameChange,
                    label = { Text("Owner") },
                    singleLine = true,
                    modifier = Modifier.width(150.dp)
                )
                SimpleDropdownField(
                    label = "Power",
                    selectedLabel = powerOptions.firstOrNull { it.first == powerFilter }?.second ?: "Any",
                    options = powerOptions,
                    onSelected = onPowerFilterChange,
                    modifier = Modifier.width(120.dp)
                )
                SimpleDropdownField(
                    label = "Condition",
                    selectedLabel = conditionOptions.firstOrNull { it.first == conditionFilter }?.second ?: "Any",
                    options = conditionOptions,
                    onSelected = onConditionFilterChange,
                    modifier = Modifier.width(130.dp)
                )
                SimpleDropdownField(
                    label = "Research",
                    selectedLabel = researchOptions.firstOrNull { it.first == researchFilter }?.second ?: "Any",
                    options = researchOptions,
                    onSelected = onResearchFilterChange,
                    modifier = Modifier.width(130.dp)
                )
                SimpleDropdownField(
                    label = "Sort",
                    selectedLabel = sortOptions.firstOrNull { it.first == sortBy }?.second ?: "Address",
                    options = sortOptions,
                    onSelected = onSortByChange,
                    modifier = Modifier.width(130.dp)
                )
                SimpleDropdownField(
                    label = "Order",
                    selectedLabel = if (sortOrder == "desc") "Desc" else "Asc",
                    options = listOf("asc" to "Asc", "desc" to "Desc"),
                    onSelected = onSortOrderChange,
                    modifier = Modifier.width(110.dp)
                )
                if (lists.isNotEmpty()) {
                    SimpleDropdownField(
                        label = "Route",
                        selectedLabel = lists.find { it.id == selectedListId }?.name ?: "All routes",
                        options = listOf("" to "All routes") + lists.map { it.id.toString() to it.name },
                        onSelected = { value -> onListChange(value.toLongOrNull()) },
                        modifier = Modifier.width(180.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { onFavoritesOnlyChange(!favoritesOnly) },
                    label = { Text("Favorites") }
                )
                FilterChip(
                    selected = vptOnly,
                    onClick = { onVptOnlyChange(!vptOnly) },
                    label = { Text("VPT") }
                )
                FilterChip(
                    selected = delinquentOnly,
                    onClick = { onDelinquentOnlyChange(!delinquentOnly) },
                    label = { Text("Delinquent") }
                )
                FilterChip(
                    selected = outOfStateOnly,
                    onClick = { onOutOfStateOnlyChange(!outOfStateOnly) },
                    label = { Text("Out of State") }
                )
                FilterChip(
                    selected = showUnscoutedOnly,
                    onClick = { onUnscoutedChange(!showUnscoutedOnly) },
                    label = { Text("Unscouted") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdownField(
    label: String,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
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
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = onToggleSelect)
            Spacer(modifier = Modifier.width(8.dp))
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
                    RouteBadges(property)
                }
            }
            IconButton(onClick = onNavigate) {
                Icon(Icons.Default.Navigation, contentDescription = "Navigate")
            }
        }
    }
}

@Composable
private fun GalleryPropertyCard(
    property: Property,
    isSelected: Boolean,
    onToggleSelect: (Boolean) -> Unit,
    onNavigate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onToggleSelect(!isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            if (!property.streetviewImagePath.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(property.streetviewImagePath)
                        .crossfade(true)
                        .build(),
                    contentDescription = property.address,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isSelected, onCheckedChange = onToggleSelect)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        property.address ?: property.apn,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOfNotNull(property.city, property.apn).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    RouteBadges(property)
                }
                IconButton(onClick = onNavigate) {
                    Icon(Icons.Default.Navigation, contentDescription = "Navigate")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteBadges(property: Property) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (property.hasVpt) {
            Badge(containerColor = Color(0xFFE53935)) {
                Text("VPT", fontSize = 10.sp)
            }
        }
        property.powerStatus?.takeIf { it.isNotBlank() }?.let { power ->
            Badge(containerColor = if (power.equals("off", ignoreCase = true)) Color(0xFFE67E22) else Color(0xFF2E7D32)) {
                Text(power.uppercase(), fontSize = 10.sp)
            }
        }
        property.conditionScore?.let { score ->
            val color = when {
                score >= 7f -> Color(0xFFE53935)
                score >= 4f -> Color(0xFFFFC107)
                else -> Color(0xFF4CAF50)
            }
            Badge(containerColor = color) {
                Text("${"%.0f".format(score)}/10", fontSize = 10.sp)
            }
        }
        if (property.isOutOfState) {
            Badge(containerColor = Color(0xFF7B1FA2)) {
                Text("OOS", fontSize = 10.sp)
            }
        }
        if (property.isScouted) {
            Badge(containerColor = Color(0xFF546E7A)) {
                Text("Scouted", fontSize = 10.sp)
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
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
        }
        Text("Page $currentPage of $totalPages")
        IconButton(
            onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
            enabled = currentPage < totalPages
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
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
        title = { Text("Add to Route") },
        text = {
            Column {
                if (showCreateNew) {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("New route name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Choose a route:")
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
                                    "${list.propertyCount} stops",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showCreateNew = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create new route")
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
