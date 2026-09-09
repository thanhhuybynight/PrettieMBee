package anhiutangerine.prettiembee.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anhiutangerine.prettiembee.data.model.CommunityTheme
import anhiutangerine.prettiembee.ui.components.StatusCard
import anhiutangerine.prettiembee.ui.components.ThemeCard
import anhiutangerine.prettiembee.ui.theme.HoneyAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isRootGranted: Boolean,
    isMbInstalled: Boolean,
    installedThemeCount: Int,
    communityThemes: List<CommunityTheme>,
    onRefreshStatus: () -> Unit,
    onSelectTheme: (CommunityTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    val categories = remember(communityThemes) {
        listOf("Tất cả") + communityThemes.map { it.series }.distinct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PrettieMBee",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = HoneyAmber
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🐝",
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Status Card
            item {
                StatusCard(
                    isRootGranted = isRootGranted,
                    isMbInstalled = isMbInstalled,
                    installedThemeCount = installedThemeCount,
                    onRefresh = onRefreshStatus
                )
            }

            // Category Filters
            item {
                Column {
                    Text(
                        text = "Kho Theme Cộng Đồng",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = category == selectedCategory
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = {
                                    Text(
                                        text = category,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HoneyAmber,
                                    selectedLabelColor = androidx.compose.ui.graphics.Color.Black
                                )
                            )
                        }
                    }
                }
            }

            // Filtered Themes
            val filteredThemes = if (selectedCategory == "Tất cả") {
                communityThemes
            } else {
                communityThemes.filter { it.series == selectedCategory }
            }

            items(filteredThemes, key = { it.id }) { theme ->
                ThemeCard(
                    theme = theme,
                    onClick = { onSelectTheme(theme) }
                )
            }
        }
    }
}
