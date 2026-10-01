package com.example.mediaflow.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mediaflow.domain.model.DownloadStatus
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun StatusBadge(
    status: DownloadStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        DownloadStatus.COMPLETED -> StatusSuccess.copy(alpha = 0.15f) to StatusSuccess
        DownloadStatus.DOWNLOADING, DownloadStatus.PROCESSING -> StatusInfo.copy(alpha = 0.15f) to StatusInfo
        DownloadStatus.QUEUED -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.secondary
        DownloadStatus.PAUSED -> StatusWarning.copy(alpha = 0.15f) to StatusWarning
        DownloadStatus.FAILED -> StatusError.copy(alpha = 0.15f) to StatusError
        DownloadStatus.CANCELLED -> Color.Gray.copy(alpha = 0.15f) to Color.Gray
        DownloadStatus.ANALYZING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.displayName,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
