package anhiutangerine.prettiembee.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import anhiutangerine.prettiembee.R
import anhiutangerine.prettiembee.ui.theme.ThemeConfig
import java.io.File

@Composable
fun StatusCard(
    isRootGranted: Boolean,
    isMbInstalled: Boolean,
    appliedNewTheme: String? = ThemeConfig.appliedNewThemeName,
    appliedOriginalTheme: String? = ThemeConfig.appliedOriginalThemeName,
    appliedIsPriority: Boolean = ThemeConfig.appliedIsPriority,
    appliedThemeId: String? = ThemeConfig.appliedThemeId,
    backgroundUri: Uri? = ThemeConfig.statusCardBackgroundUri,
    cardAlpha: Float = ThemeConfig.cardAlpha,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isThemeInstalled = !appliedNewTheme.isNullOrBlank() && !appliedOriginalTheme.isNullOrBlank()

    // Theme preview: login_bg_main from the currently applied package (local download)
    val themeLoginBg = remember(appliedThemeId) {
        appliedThemeId
            ?.takeIf { it.isNotBlank() }
            ?.let { id ->
                File(File(context.filesDir, "themes"), "$id/images/login_bg_main.png")
                    .takeIf { it.isFile }
            }
    }
    val showThemePreview = backgroundUri == null && isThemeInstalled && themeLoginBg != null

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = cardAlpha),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Status Card Custom Background (full-bleed when user picked one)
            if (backgroundUri != null) {
                val file = remember(backgroundUri) {
                    backgroundUri.path?.let { File(it) }?.takeIf { it.exists() }
                }
                if (file != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(file)
                            .allowHardware(false)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(24.dp)),
                        alpha = 0.85f
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                    )
                }
            }

            // Installed theme preview: 16:9 crop of login_bg_main when no custom status bg
            if (showThemePreview && themeLoginBg != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(themeLoginBg)
                            .allowHardware(false)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.status_theme_preview_desc, appliedNewTheme.orEmpty()),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isRootGranted && isMbInstalled) {
                                stringResource(R.string.status_theme_installed)
                            } else {
                                when {
                                    !isRootGranted -> stringResource(R.string.status_root_missing)
                                    !isMbInstalled -> stringResource(R.string.status_mb_missing)
                                    else -> stringResource(R.string.status_ready)
                                }
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (appliedIsPriority) {
                                stringResource(R.string.status_applied_theme_prio, appliedNewTheme.orEmpty(), appliedOriginalTheme.orEmpty())
                            } else {
                                stringResource(R.string.status_applied_theme_label, appliedNewTheme.orEmpty(), appliedOriginalTheme.orEmpty())
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (!showThemePreview) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val statusText = when {
                        !isRootGranted -> stringResource(R.string.status_root_missing)
                        !isMbInstalled -> stringResource(R.string.status_mb_missing)
                        isThemeInstalled -> stringResource(R.string.status_theme_installed)
                        else -> stringResource(R.string.status_ready)
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = if (backgroundUri != null) Color.White
                        else MaterialTheme.colorScheme.onSurface
                    )

                    if (isThemeInstalled && isRootGranted && isMbInstalled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val subtitleText = if (appliedIsPriority) {
                            stringResource(R.string.status_applied_theme_prio, appliedNewTheme.orEmpty(), appliedOriginalTheme.orEmpty())
                        } else {
                            stringResource(R.string.status_applied_theme_label, appliedNewTheme.orEmpty(), appliedOriginalTheme.orEmpty())
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (backgroundUri != null) Color.White.copy(alpha = 0.9f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}
