package anhiutangerine.prettiembee.ui.screens

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
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Error

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anhiutangerine.prettiembee.BuildConfig
import anhiutangerine.prettiembee.R
import anhiutangerine.prettiembee.data.model.InjectConfig
import anhiutangerine.prettiembee.ui.theme.AppThemeMode
import anhiutangerine.prettiembee.ui.theme.DarkBackground
import anhiutangerine.prettiembee.ui.theme.ErrorRed
import anhiutangerine.prettiembee.ui.theme.LightBackground
import anhiutangerine.prettiembee.ui.theme.OledBackground

import anhiutangerine.prettiembee.ui.theme.SuccessGreen
import anhiutangerine.prettiembee.ui.theme.ThemeConfig
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
|_|   |_|  \___|\__|\__|_|\___|_|  |_|____/ \___|\___|"""

    fun createInitialLogs(
        config: InjectConfig,
        currentTargetName: String,
        targetPackage: String,
        context: android.content.Context
    ): List<String> {
        val list = mutableListOf<String>()
        ASCII_BANNER.lines().forEach { list.add(it) }
        list.add("")
        list.add("- ${context.getString(R.string.flash_log_version, BuildConfig.GIT_HASH)}")
        list.add("- ${context.getString(R.string.flash_log_source, config.sourceTheme.name)}")
        list.add("- ${context.getString(R.string.flash_log_target, currentTargetName)}")
        list.add("- ${context.getString(R.string.flash_log_package, targetPackage)}")
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Prevent accidental back navigation while flashing
    BackHandler(enabled = true) {
        if (status == FlashingStatus.FLASHING) {
            Toast.makeText(context, context.getString(R.string.flash_in_progress), Toast.LENGTH_SHORT).show()
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

    val isOled = ThemeConfig.themeMode == AppThemeMode.OLED_DARK
    val isDark = when (ThemeConfig.themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.MATERIAL_DARK, AppThemeMode.OLED_DARK -> true
    }
    val baseBackground = when {
        isOled -> OledBackground
        isDark -> DarkBackground
        else -> LightBackground
    }
    val textColor = if (isDark) Color.White else Color.Black

    val fullLogText = remember(logs.size, logs.lastOrNull()) {
        logs.joinToString("\n")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBackground)
    ) {
        // Full Screen Wallpaper (KittiSU style)
        if (ThemeConfig.appBackgroundUri != null) {
            val file = remember(ThemeConfig.appBackgroundUri) {
                ThemeConfig.appBackgroundUri?.path?.let { File(it) }?.takeIf { it.exists() }
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
                    modifier = Modifier.fillMaxSize()
                )
                if (ThemeConfig.backgroundDim > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = ThemeConfig.backgroundDim))
                    )
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
                                    FlashingStatus.FLASHING -> stringResource(R.string.flash_in_progress)
                                    FlashingStatus.SUCCESS -> stringResource(R.string.flash_success_title)
                                    FlashingStatus.FAILED -> stringResource(R.string.flash_failed_title)
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
                                    Toast.makeText(context, context.getString(R.string.flash_in_progress), Toast.LENGTH_SHORT).show()
                                } else {
                                    onBack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.flash_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(fullLogText))
                                Toast.makeText(context, context.getString(R.string.flash_copied), Toast.LENGTH_SHORT).show()

                                try {
                                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    if (downloadsDir.exists() && downloadsDir.canWrite()) {
                                        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                        val file = File(downloadsDir, "PrettieMBee_install_${dateStr}.log")
                                        file.writeText(fullLogText)
                                    }
                                } catch (_: Exception) {}
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.flash_copy_log),
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
                    if (status == FlashingStatus.FAILED) {
                        ExtendedFloatingActionButton(
                            onClick = onBack,
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.action_close)
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.action_close),
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

                Spacer(modifier = Modifier.height(8.dp))

                // Log directly on app background (no fake black terminal container)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .verticalScroll(scrollState)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    SelectionContainer {
                        Text(
                            text = fullLogText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.8.sp,
                            lineHeight = 11.5.sp,
                            color = textColor,
                            softWrap = false
                        )
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
            Text(
                text = themeName.ifEmpty { stringResource(R.string.app_name) },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

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
                            text = stringResource(R.string.flash_failed_title),
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
