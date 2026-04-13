package com.vpt.scout.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vpt.scout.*
import kotlinx.coroutines.launch

// ============================================================================
// Card Swipe (Carousel) Screen for browsing list properties one at a time
// ============================================================================

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CardSwipeScreen(
    listId: Long,
    listRepository: ListRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var listData by remember { mutableStateOf<ListWithProperties?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load list details on launch
    LaunchedEffect(listId) {
        try {
            listData = listRepository.getList(listId)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load list"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            listData?.name ?: "Properties",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        listData?.let {
                            Text(
                                "${it.properties.size} properties",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading properties…")
                    }
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Error,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            isLoading = true
                            error = null
                            scope.launch {
                                try {
                                    listData = listRepository.getList(listId)
                                } catch (e: Exception) {
                                    error = e.message
                                } finally {
                                    isLoading = false
                                }
                            }
                        }) { Text("Retry") }
                    }
                }
            }

            listData != null && listData!!.properties.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inbox,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No properties in this list")
                    }
                }
            }

            listData != null -> {
                val properties = listData!!.properties
                val pagerState = rememberPagerState(pageCount = { properties.size })

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Counter indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${pagerState.currentPage + 1} of ${properties.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Swipeable card pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        pageSpacing = 16.dp
                    ) { page ->
                        PropertyCard(
                            property = properties[page],
                            onOpenMaps = { property ->
                                val address = property.address ?: return@PropertyCard
                                val encodedAddress =
                                    Uri.encode(address)
                                val uri =
                                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedAddress")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            onNavigate = { property ->
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

                    // Dot indicator row
                    if (properties.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val maxDots = 7
                            val totalPages = properties.size
                            val currentPage = pagerState.currentPage

                            // Show a sliding window of dots around the current page
                            val startDot = (currentPage - maxDots / 2).coerceIn(0, (totalPages - maxDots).coerceAtLeast(0))
                            val endDot = (startDot + maxDots).coerceAtMost(totalPages)

                            for (i in startDot until endDot) {
                                val isSelected = i == currentPage
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(if (isSelected) 10.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ============================================================================
// Individual Property Card
// ============================================================================

@Composable
private fun PropertyCard(
    property: Property,
    onOpenMaps: (Property) -> Unit,
    onNavigate: (Property) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Image section with overlaid badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
            ) {
                // Street view image or map fallback
                val imageUrl = property.streetviewImagePath
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Street view of ${property.address}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Map,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                startY = 100f
                            )
                        )
                )

                // Badge row at bottom of image
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Defaulted badge
                    Badge(
                        text = "Defaulted",
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    )

                    // Power status badge
                    when (property.powerStatus) {
                        "on" -> Badge(
                            text = "⚡ On",
                            containerColor = Color(0xFF22C55E),
                            contentColor = Color.White
                        )
                        "off" -> Badge(
                            text = "⚡ Off",
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    }

                    // Out of state star
                    if (property.isOutOfState) {
                        Badge(
                            text = "⭐ Out of State",
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color.White
                        )
                    }

                    // Deceased badge
                    if (property.deceasedCount != null && property.deceasedCount > 0) {
                        Badge(
                            text = "D",
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    }
                }
            }

            // Info section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Address
                    Text(
                        property.address ?: "Unknown Address",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // City
                    property.city?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detail rows
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // APN
                    DetailRow(
                        icon = Icons.Default.Badge,
                        label = "APN",
                        value = property.apn
                    )

                    // Last sale date
                    property.lastSaleDate?.let {
                        DetailRow(
                            icon = Icons.Default.CalendarMonth,
                            label = "Last Sale",
                            value = it
                        )
                    }

                    // Mailing address
                    property.mailingAddress?.let {
                        DetailRow(
                            icon = Icons.Default.Mail,
                            label = "Mailing",
                            value = it,
                            highlight = property.isOutOfState
                        )
                    }

                    // Power status
                    property.powerStatus?.let { power ->
                        DetailRow(
                            icon = Icons.Default.ElectricBolt,
                            label = "Power",
                            value = power.replaceFirstChar { it.uppercase() },
                            valueColor = when (power) {
                                "on" -> Color(0xFF22C55E)
                                "off" -> Color(0xFFEF4444)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    // Condition score
                    property.conditionScore?.let {
                        DetailRow(
                            icon = Icons.Default.Star,
                            label = "Condition",
                            value = "%.1f / 10".format(it)
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onOpenMaps(property) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Map,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Map")
                    }

                    Button(
                        onClick = { onNavigate(property) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Navigate")
                    }
                }
            }
        }
    }
}

// ============================================================================
// Helper composables
// ============================================================================

@Composable
private fun Badge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        shadowElevation = 2.dp
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    highlight: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(18.dp),
            tint = if (highlight) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
