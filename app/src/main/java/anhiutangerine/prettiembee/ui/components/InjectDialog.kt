package anhiutangerine.prettiembee.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import anhiutangerine.prettiembee.data.model.InjectResult
import anhiutangerine.prettiembee.ui.theme.DarkBackground
import anhiutangerine.prettiembee.ui.theme.ErrorRed
import anhiutangerine.prettiembee.ui.theme.SakuraPink
import anhiutangerine.prettiembee.ui.theme.SuccessGreen

@Composable
fun InjectDialog(
    isRunning: Boolean,
    logs: List<String>,
    result: InjectResult?,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Dialog(onDismissRequest = { if (!isRunning) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Status Icon
                if (isRunning) {
                    CircularProgressIndicator(
                        color = SakuraPink,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                } else if (result?.isSuccess == true) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Success",
                            tint = SuccessGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ErrorRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = "Error",
                            tint = ErrorRed,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when {
                        isRunning -> "Đang áp dụng theme..."
                        result?.isSuccess == true -> "Áp dụng theme thành công"
                        else -> "Có lỗi xảy ra"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Log Window
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs) { logLine ->
                            Text(
                                text = logLine,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = when {
                                    logLine.contains("[Lỗi]") || logLine.contains("❌") -> ErrorRed
                                    logLine.contains("[Thành công]") || logLine.contains("✅") -> SuccessGreen
                                    logLine.contains("[Chuẩn bị]") || logLine.contains("[Thông tin]") -> SakuraPink
                                    else -> Color(0xFFDDDDDD)
                                },
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (!isRunning) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (result?.isSuccess == true) SuccessGreen else SakuraPink,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (result?.isSuccess == true) "Hoàn tất" else "Đóng",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
