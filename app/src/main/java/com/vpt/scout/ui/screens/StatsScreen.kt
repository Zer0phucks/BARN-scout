package com.vpt.scout.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpt.scout.ScoutRepository
import com.vpt.scout.ScoutStats
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    scoutRepository: ScoutRepository
) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<ScoutStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Load stats
    LaunchedEffect(Unit) {
        try {
            stats = scoutRepository.getStats()
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scout Stats") },
                actions = {
                    IconButton(onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                stats = scoutRepository.getStats()
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                isLoading = false
                            }
                        }
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            isLoading = true
                            error = null
                            scope.launch {
                                try {
                                    stats = scoutRepository.getStats()
                                } catch (e: Exception) {
                                    error = e.message
                                } finally {
                                    isLoading = false
                                }
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                stats != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Big number card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "${stats!!.uniqueProperties}",
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Properties Scouted",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        
                        // Stats grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Total Visits",
                                value = stats!!.totalVisits.toString(),
                                icon = Icons.Default.DirectionsWalk,
                                color = Color(0xFF2196F3)
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Follow-ups",
                                value = stats!!.followUps.toString(),
                                icon = Icons.Default.Flag,
                                color = Color(0xFFF44336)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Flyered",
                                value = stats!!.flyered.toString(),
                                icon = Icons.Default.Description,
                                color = Color(0xFF4CAF50)
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Conversion",
                                value = if (stats!!.uniqueProperties > 0) {
                                    "${"%.0f".format(stats!!.followUps.toFloat() / stats!!.uniqueProperties * 100)}%"
                                } else "0%",
                                icon = Icons.Default.TrendingUp,
                                color = Color(0xFF9C27B0)
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Sync button
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    scoutRepository.syncPendingResults()
                                    stats = scoutRepository.getStats()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Sync, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sync Pending Results")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
