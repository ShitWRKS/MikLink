package com.app.miklink.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.miklink.R
import com.app.miklink.ui.testing.AgentUiTags

@Composable
internal fun TestProfileSpeedTab(
    runSpeedTest: Boolean,
    onRunSpeedTestChange: (Boolean) -> Unit,
    speedMaxPing: String,
    onSpeedMaxPingChange: (String) -> Unit,
    speedMaxPingIsError: Boolean,
    speedMaxJitter: String,
    onSpeedMaxJitterChange: (String) -> Unit,
    speedMaxJitterIsError: Boolean,
    speedMaxLoss: String,
    onSpeedMaxLossChange: (String) -> Unit,
    speedMaxLossIsError: Boolean,
    speedMinDownload: String,
    onSpeedMinDownloadChange: (String) -> Unit,
    speedMinDownloadIsError: Boolean,
    speedMinUpload: String,
    onSpeedMinUploadChange: (String) -> Unit,
    speedMinUploadIsError: Boolean,
    effectiveMaxPing: Double?,
    effectiveMaxJitter: Double?,
    effectiveMaxLoss: Double?,
    effectiveMinDownload: Double?,
    effectiveMinUpload: Double?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileSection(title = stringResource(R.string.profile_edit_speed_section_title)) {
                ProfileSwitchItem(
                    checked = runSpeedTest,
                    onCheckedChange = onRunSpeedTestChange,
                    headlineText = stringResource(R.string.profile_edit_run_speed_title),
                    supportingText = stringResource(R.string.profile_edit_run_speed_support),
                    modifier = Modifier.testTag(AgentUiTags.Profile.RUN_SPEED)
                )
                ThresholdRow(
                    leftLabel = stringResource(R.string.profile_edit_threshold_ping),
                    leftUnit = "ms",
                    leftValue = speedMaxPing,
                    onLeftChange = onSpeedMaxPingChange,
                    leftIsError = speedMaxPingIsError,
                    rightLabel = stringResource(R.string.profile_edit_threshold_jitter),
                    rightUnit = "ms",
                    rightValue = speedMaxJitter,
                    onRightChange = onSpeedMaxJitterChange,
                    rightIsError = speedMaxJitterIsError
                )
                ThresholdRow(
                    leftLabel = stringResource(R.string.profile_edit_threshold_loss),
                    leftUnit = "%",
                    leftValue = speedMaxLoss,
                    onLeftChange = onSpeedMaxLossChange,
                    leftIsError = speedMaxLossIsError,
                    rightLabel = stringResource(R.string.profile_edit_download_minimum),
                    rightUnit = "Mbps",
                    rightValue = speedMinDownload,
                    onRightChange = onSpeedMinDownloadChange,
                    rightIsError = speedMinDownloadIsError,
                    rightErrorMessage = stringResource(R.string.profile_edit_threshold_invalid_throughput),
                    rightTag = AgentUiTags.Profile.SPEED_MIN_DOWNLOAD
                )
                ThresholdRow(
                    leftLabel = stringResource(R.string.profile_edit_upload_minimum),
                    leftUnit = "Mbps",
                    leftValue = speedMinUpload,
                    onLeftChange = onSpeedMinUploadChange,
                    leftIsError = speedMinUploadIsError,
                    leftErrorMessage = stringResource(R.string.profile_edit_threshold_invalid_throughput)
                )
                SpeedThresholdPreview(
                    downloadMbps = effectiveMinDownload,
                    uploadMbps = effectiveMinUpload,
                    maxPingMs = effectiveMaxPing,
                    maxJitterMs = effectiveMaxJitter,
                    maxLossPercent = effectiveMaxLoss,
                    onDownloadChange = onSpeedMinDownloadChange,
                    onUploadChange = onSpeedMinUploadChange
                )
            }
        }
    }
}
