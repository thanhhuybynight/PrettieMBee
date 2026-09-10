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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import anhiutangerine.prettiembee.R
import anhiutangerine.prettiembee.ui.theme.ThemeConfig

@Composable
fun StatusCard(
    isRootGranted: Boolean,
    isMbInstalled: Boolean,
    appliedNewTheme: String? = ThemeConfig.appliedNewThemeName,
    appliedOriginalTheme: String? = ThemeConfig.appliedOriginalThemeName,
    appliedIsPriority: Boolean = ThemeConfig.appliedIsPriority,
    backgroundUri: Uri? = ThemeConfig.statusCardBackgroundUri,
    cardAlpha: Float = ThemeConfig.cardAlpha,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = cardAlpha),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Status Card Custom Background (KittiSU style)
            if (backgroundUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(backgroundUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(24.dp)),
                    alpha = 0.85f
                )
                // Subtle darkening overlay so text remains readable
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cute Kitten Mascot Avatar
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

                // Status determination: "Chưa có quyền root", "Chưa cài đặt", "Đã cài đặt", "Sẵn sàng"
                val isThemeInstalled = !appliedNewTheme.isNullOrBlank() && !appliedOriginalTheme.isNullOrBlank()
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
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Subtitle nếu đã cài đặt: "Tên theme mới - Tên theme gốc [- Prio]"
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

