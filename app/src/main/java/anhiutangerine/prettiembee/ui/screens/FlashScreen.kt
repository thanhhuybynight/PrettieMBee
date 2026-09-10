package anhiutangerine.prettiembee.ui.screens

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anhiutangerine.prettiembee.BuildConfig
import anhiutangerine.prettiembee.data.model.InjectConfig
import anhiutangerine.prettiembee.ui.theme.DarkBackground
import anhiutangerine.prettiembee.ui.theme.ErrorRed
import anhiutangerine.prettiembee.ui.theme.LightBackground
import anhiutangerine.prettiembee.ui.theme.OledBackground
import anhiutangerine.prettiembee.ui.theme.SakuraPink
import anhiutangerine.prettiembee.ui.theme.SuccessGreen
import anhiutangerine.prettiembee.ui.theme.ThemeConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FlashingStatus {
    FLASHING,
    SUCCESS,
    FAILED
}

object FlashScreenConstants {
    const val ASCII_BANNER = """ ____           _   _   _      __  __ ____             
|  _ \ _ __ ___| |_| |_(_) ___|  \/  | __ )  ___  ___  
| |_) | '__/ _ \ __| __| |/ _ \ |\/| |  _ \ / _ \/ _ \ 
|  __/| | |  __/ |_| |_| |  __/ |  | | |_) |  __/  __/ 
|_|   |_|  \___|\__|\__|_|\___|_|  |_|____/ \___|\___| 
=======================================================
*             PRETTIEMBEE THEME INSTALLER             *
*              Inspired by KittiSU Style              *
======================================================="""

    fun createInitialLogs(
        config: InjectConfig,
        currentTargetName: String,
        targetPackage: String
    ): List<String> {
        val list = mutableListOf<String>()
        ASCII_BANNER.lines().forEach { list.add(it) }
        list.add("- Phiên bản: v1.0 (${BuildConfig.GIT_HASH})")
        list.add("- Nguồn theme: ${config.sourceTheme.name} (${config.sourceTheme.series})")
        list.add("- Tác giả: ${config.sourceTheme.author}")
        list.add("- Vị trí áp dụng: $currentTargetName")
        list.add("- Mã định danh UUID: ${config.targetUuid}")
        list.add("- Chế độ Priority: ${if (config.usePriorityVariant) "BẬT" else "TẮT"}")
        list.add("- Gói đích MB Bank: $targetPackage")
        list.add("=======================================================")
        list.add("")
        return list
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashScreen(
    config: InjectConfig,
    currentTargetName: String,
    logs: List<String>,
    status: FlashingStatus,
    failedReason: String? = null,
    onBack: () -> Unit,
    onLaunchMb: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Prevent accidental back navigation while flashing
    BackHandler(enabled = true) {
        if (status == FlashingStatus.FLASHING) {
            Toast.makeText(context, "Đang nạp theme, vui lòng không thoát...", Toast.LENGTH_SHORT).show()
        } else {
            onBack()
        }
    }

    // Auto-scroll terminal log as new lines arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val statusColor = when (status) {
        FlashingStatus.FLASHING -> MaterialTheme.colorScheme.primary
        FlashingStatus.SUCCESS -> SuccessGreen
        FlashingStatus.FAILED -> ErrorRed
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = SakuraPink

    val annotatedTerminalText = remember(logs.size, logs.lastOrNull(), status) {
        buildAnnotatedString {
            logs.forEachIndexed { index, line ->
                val isBanner = line.contains("PRETTIEMBEE") ||
                        line.contains("Inspired by") ||
                        line.startsWith("===") ||
                        line.startsWith("|") ||
                        line.startsWith(" _") ||
                        line.startsWith("|_")
                val isSuccess = line.contains("[Thành công]") ||
                        line.contains("✅") ||
                        line.startsWith("* ") ||
                        line.contains("HOÀN TẤT")
                val isError = line.contains("[Lỗi]") ||
                        line.contains("❌") ||
                        line.startsWith("! ") ||
                        line.contains("THẤT BẠI")
                val isWarning = line.contains("[Cảnh báo]") || line.contains("⚠️")
                val isInfo = line.contains("[Chuẩn bị]") ||
                        line.contains("[Tiến trình]") ||
                        line.contains("[Hành động]") ||
                        line.contains("[Thông tin]") ||
                        line.contains("[Tải về]") ||
                        line.startsWith("- ")

                val textColor = when {
                    isError -> ErrorRed
                    isSuccess -> SuccessGreen
                    isWarning -> Color(0xFFFFA726)
                    isBanner -> accentColor
                    isInfo -> primaryColor
                    else -> Color(0xFFE2E8F0)
                }

                val textWeight = when {
                    isBanner || isSuccess || isError -> FontWeight.Bold
                    isInfo -> FontWeight.SemiBold
                    else -> FontWeight.Normal
                }

                withStyle(SpanStyle(color = textColor, fontWeight = textWeight)) {
                    append(line)
                }
                if (index < logs.size - 1) {
                    append("\n")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (status) {
                                FlashingStatus.FLASHING -> "Đang cài đặt theme"
                                FlashingStatus.SUCCESS -> "Cài đặt thành công"
                                FlashingStatus.FAILED -> "Cài đặt thất bại"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                        Text(
                            text = "${config.sourceTheme.name} → $currentTargetName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (status == FlashingStatus.FLASHING) {
                                Toast.makeText(context, "Đang nạp theme, vui lòng không thoát...", Toast.LENGTH_SHORT).show()
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val fullLog = logs.joinToString("\n")
                            clipboardManager.setText(AnnotatedString(fullLog))
                            Toast.makeText(context, "Đã sao chép toàn bộ nhật ký cài đặt!", Toast.LENGTH_SHORT).show()

                            // Try saving log file in public Downloads directory
                            try {
                                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                if (downloadsDir.exists() && downloadsDir.canWrite()) {
                                    val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    val file = File(downloadsDir, "PrettieMBee_install_${dateStr}.log")
                                    file.writeText(fullLog)
                                }
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Sao chép nhật ký",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = ThemeConfig.cardAlpha),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = ThemeConfig.cardAlpha)
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = status != FlashingStatus.FLASHING,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                if (status == FlashingStatus.SUCCESS) {
                    ExtendedFloatingActionButton(
                        onClick = onLaunchMb,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.RocketLaunch,
                                contentDescription = "Mở MB Bank"
                            )
                        },
                        text = {
                            Text(
                                text = "Mở MB Bank",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        containerColor = SakuraPink,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else if (status == FlashingStatus.FAILED) {
                    ExtendedFloatingActionButton(
                        onClick = onBack,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Đóng"
                            )
                        },
                        text = {
                            Text(
                                text = "Đóng",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Module progress status bar (KittiSU style)
            FlashProgressBarCard(
                themeName = config.sourceTheme.name,
                status = status,
                failedReason = failedReason
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Terminal Console
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0F1117),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Terminal Mini Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161922))
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 3 Terminal Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                        }

                        Text(
                            text = "terminal - root shell",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "${logs.size} lines",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Terminal Logs View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = annotatedTerminalText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashProgressBarCard(
    themeName: String,
    status: FlashingStatus,
    failedReason: String?
) {
    val progressColor by animateColorAsState(
        targetValue = when (status) {
            FlashingStatus.FLASHING -> MaterialTheme.colorScheme.primary
            FlashingStatus.SUCCESS -> SuccessGreen
            FlashingStatus.FAILED -> ErrorRed
        },
        label = "ProgressColor"
    )

    val targetProgress = when (status) {
        FlashingStatus.FLASHING -> 0.75f
        FlashingStatus.SUCCESS -> 1.0f
        FlashingStatus.FAILED -> 1.0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 600),
        label = "ProgressValue"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = ThemeConfig.cardAlpha)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = themeName.ifEmpty { "MB Bank Theme" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = when (status) {
                        FlashingStatus.FLASHING -> "1/1"
                        FlashingStatus.SUCCESS -> "Hoàn tất"
                        FlashingStatus.FAILED -> "Lỗi"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (status == FlashingStatus.FLASHING) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.2f)
                )
            } else {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.2f)
                )
            }

            // Failure details box
            AnimatedVisibility(
                visible = status == FlashingStatus.FAILED && !failedReason.isNullOrBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chi tiết lỗi",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ErrorRed
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = ErrorRed.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = failedReason ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
