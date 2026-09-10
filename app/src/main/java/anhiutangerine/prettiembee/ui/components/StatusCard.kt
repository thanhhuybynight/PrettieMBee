package anhiutangerine.prettiembee.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
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
                        contentDescription = "Ảnh nền theme đang cài: $appliedNewTheme",
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
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_prettiembee_logo),
                                contentDescription = "PrettieMBee Cat",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isRootGranted && isMbInstalled) "Đã cài đặt" else {
                                when {
                                    !isRootGranted -> "Chưa có quyền root"
                                    !isMbInstalled -> "Chưa cài đặt"
                                    else -> "Sẵn sàng"
                                }
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val prioSuffix = if (appliedIsPriority) " - Prio" else ""
                        Text(
                            text = "$appliedNewTheme - $appliedOriginalTheme$prioSuffix",
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
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (backgroundUri != null)
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_prettiembee_logo),
                            contentDescription = "PrettieMBee Cat",
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val statusText = when {
                        !isRootGranted -> "Chưa có quyền root"
                        !isMbInstalled -> "Chưa cài đặt"
                        isThemeInstalled -> "Đã cài đặt"
                        else -> "Sẵn sàng"
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
                        val prioSuffix = if (appliedIsPriority) " - Prio" else ""
                        val subtitleText = "$appliedNewTheme - $appliedOriginalTheme$prioSuffix"
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
