package anhiutangerine.prettiembee.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anhiutangerine.prettiembee.R
import anhiutangerine.prettiembee.ui.theme.ErrorRed
import anhiutangerine.prettiembee.ui.theme.SuccessGreen

@Composable
fun StatusCard(
    isRootGranted: Boolean,
    isMbInstalled: Boolean,
    targetPackage: String,
    installedThemeCount: Int,
    onRefresh: () -> Unit,
    onEditPackage: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Hero Status Card (KittiSU aesthetic)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cute Kitten Mascot Avatar
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_prettiembee_logo),
                    contentDescription = "PrettieMBee Cat",
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Status Headline
            Text(
                text = if (isRootGranted) "Sẵn sàng hoạt động" else "Chưa có quyền root",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isRootGranted) "Quyền SuperUser đã sẵn sàng để áp dụng theme" else "Cần cấp quyền root qua Magisk, KernelSU hoặc APatch",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    label = if (isRootGranted) "Root: Đã cấp" else "Root: Chưa có",
                    isPositive = isRootGranted
                )

                Spacer(modifier = Modifier.width(8.dp))

                StatusPill(
                    label = if (isMbInstalled) "MB Bank: Đã cài" else "MB Bank: Chưa cài",
                    isPositive = isMbInstalled
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "$installedThemeCount theme trong app",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kiểm tra lại",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }

                FilledTonalButton(
                    onClick = onEditPackage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Package",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Đổi gói MB",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    isPositive: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isPositive) SuccessGreen.copy(alpha = 0.14f) else ErrorRed.copy(alpha = 0.14f),
        border = BorderStroke(
            1.dp,
            if (isPositive) SuccessGreen.copy(alpha = 0.25f) else ErrorRed.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isPositive) SuccessGreen else ErrorRed)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = if (isPositive) SuccessGreen else ErrorRed
                )
            )
        }
    }
}
