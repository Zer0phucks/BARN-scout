package com.vpt.scout.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vpt.scout.ListRepository
import com.vpt.scout.ListWithProperties
import com.vpt.scout.ListsState
import com.vpt.scout.Property
import com.vpt.scout.PropertyList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    listRepository: ListRepository,
    onNavigateToList: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listsState by listRepository.listsState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        listRepository.refreshLists()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Route") },
                actions = {
                    IconButton(onClick = { scope.launch { listRepository.refreshLists() } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create route")
            }
        }
    ) { padding ->
        when (val state = listsState) {
            is ListsState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ListsState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is ListsState.Success -> {
                if (state.lists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No routes yet")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Create one from Explore or here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        itemsIndexed(state.lists, key = { _, item -> item.id }) { _, list ->
                            RouteListItem(
                                list = list,
                                onClick = { onNavigateToList(list.id) },
                                onDelete = { scope.launch { listRepository.deleteList(list.id) } }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Route") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            scope.launch {
                                listRepository.createList(name, description.takeIf { it.isNotBlank() })
                            }
                            showCreateDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RouteListItem(
    list: PropertyList,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(list.name, fontWeight = FontWeight.Medium)
                Text(
                    "${list.propertyCount} stops",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                list.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Route?") },
            text = { Text("Delete \"${list.name}\"? This only removes the queue.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    listId: Long,
    listRepository: ListRepository,
    onNavigateToScout: () -> Unit,
    onNavigateToCardSwipe: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var list by remember { mutableStateOf<ListWithProperties?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refreshList() {
        scope.launch {
            isLoading = true
            try {
                list = listRepository.getList(listId)
                error = null
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun moveProperty(fromIndex: Int, toIndex: Int) {
        val current = list ?: return
        if (toIndex !in current.properties.indices) return
        val reordered = current.properties.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        scope.launch {
            listRepository.reorderListProperties(listId, reordered.map { it.apn })
            list = current.copy(properties = reordered)
        }
    }

    LaunchedEffect(listId) {
        refreshList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(list?.name ?: "Route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshList() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val route = listRepository.getRouteUrl(listId)
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(route.url)))
                            } catch (_: Exception) {
                            }
                        }
                    }) {
                        Icon(Icons.Default.Route, contentDescription = "Open route")
                    }
                }
            )
        },
        floatingActionButton = {
            if (list?.properties?.isNotEmpty() == true) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = onNavigateToCardSwipe,
                        icon = { Icon(Icons.Default.Swipe, contentDescription = null) },
                        text = { Text("Cards") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    ExtendedFloatingActionButton(
                        onClick = onNavigateToScout,
                        icon = { Icon(Icons.Default.Navigation, contentDescription = null) },
                        text = { Text("Scout") }
                    )
                }
            }
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }

            list != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("${list!!.properties.size} stops", style = MaterialTheme.typography.titleMedium)
                                list!!.description?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(list!!.properties, key = { _, property -> property.apn }) { index, property ->
                        RouteQueueItem(
                            index = index,
                            total = list!!.properties.size,
                            property = property,
                            onMoveUp = { moveProperty(index, index - 1) },
                            onMoveDown = { moveProperty(index, index + 1) },
                            onRemove = {
                                scope.launch {
                                    listRepository.removePropertyFromList(listId, property.apn)
                                    refreshList()
                                }
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
            }
        }
    }
}

@Composable
private fun RouteQueueItem(
    index: Int,
    total: Int,
    property: Property,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onNavigate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(property.address ?: property.apn, fontWeight = FontWeight.Medium)
                Text(
                    listOfNotNull(property.city, property.apn).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown, enabled = index < total - 1) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
            }
            IconButton(onClick = onNavigate) {
                Icon(Icons.Default.Navigation, contentDescription = "Navigate")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
