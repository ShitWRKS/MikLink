package com.app.miklink.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.app.miklink.R
import com.app.miklink.core.domain.validation.MAX_SPEED_THROUGHPUT_MBPS
import com.app.miklink.ui.testing.AgentUiTags
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.expm1
import kotlin.math.ln
import kotlin.math.ln1p
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

internal data class LinkScaleMarker(
    val fraction: Float,
    val outsidePresetRange: Boolean
)

internal fun linkScaleMarker(rateMbps: Double, presetRatesMbps: List<Double>): LinkScaleMarker? {
    val sorted = presetRatesMbps.filter { it.isFinite() && it > 0.0 }.sorted()
    if (!rateMbps.isFinite() || rateMbps < 0.0 || sorted.size < 2) return null
    val minRate = sorted.first()
    val maxRate = sorted.last()
    if (rateMbps == 0.0) return LinkScaleMarker(0f, outsidePresetRange = true)
    val rawFraction = (ln(rateMbps) - ln(minRate)) / (ln(maxRate) - ln(minRate))
    return LinkScaleMarker(
        fraction = rawFraction.coerceIn(0.0, 1.0).toFloat(),
        outsidePresetRange = rateMbps < minRate || rateMbps > maxRate
    )
}

internal fun throughputDisplayMaximum(downloadMbps: Double?, uploadMbps: Double?): Double {
    val largest = listOfNotNull(downloadMbps, uploadMbps)
        .filter { it.isFinite() && it >= 0.0 }
        .maxOrNull()
        ?: 0.0
    return if (largest == 0.0) 1.0 else largest * 1.2
}

internal fun adaptiveSliderMaximum(values: List<Double?>): Double {
    val largest = values.filterNotNull()
        .filter { it.isFinite() && it >= 0.0 }
        .maxOrNull()
        ?: return 1.0
    if (largest == 0.0) return 1.0

    val magnitude = 10.0.pow(floor(log10(largest)))
    val normalized = largest / magnitude
    val nextStep = when {
        normalized <= 1.0 -> 2.0
        normalized <= 2.0 -> 5.0
        normalized <= 5.0 -> 10.0
        else -> 20.0
    }
    return (nextStep * magnitude).takeIf(Double::isFinite) ?: largest
}

internal fun sliderInputValue(value: Float): String =
    String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')

internal fun throughputToSliderFraction(valueMbps: Double): Float =
    (ln1p(valueMbps.coerceIn(0.0, MAX_SPEED_THROUGHPUT_MBPS)) /
        ln1p(MAX_SPEED_THROUGHPUT_MBPS)).toFloat()

internal fun sliderFractionToThroughput(fraction: Float): Double =
    expm1(fraction.coerceIn(0f, 1f) * ln1p(MAX_SPEED_THROUGHPUT_MBPS))
        .coerceIn(0.0, MAX_SPEED_THROUGHPUT_MBPS)

internal fun illustrativeRttSeries(maxAverageRttMs: Double?, maxRttMs: Double?): List<Double> {
    val reference = listOfNotNull(maxAverageRttMs, maxRttMs)
        .filter { it.isFinite() && it >= 0.0 }
        .maxOrNull()
        ?: return emptyList()
    val ratios = listOf(0.18, 0.31, 0.24, 0.48, 0.36, 0.57, 0.43)
    return ratios.map { ratio -> reference * ratio }
}

@Composable
internal fun LinkThresholdPreview(
    configuredValue: String,
    effectiveRateMbps: Double?,
    presetLabels: List<String>,
    presetRatesMbps: List<Double>,
    onConfiguredValueChange: (String) -> Unit
) {
    val marker = effectiveRateMbps?.let { linkScaleMarker(it, presetRatesMbps) }
    PreviewCard(title = stringResource(R.string.profile_edit_threshold_preview_title)) {
        Text(
            text = stringResource(R.string.profile_edit_preview_illustrative),
            style = MaterialTheme.typography.bodySmall
        )
        if (marker == null || presetLabels.size < 2) {
            Text(
                text = stringResource(R.string.profile_edit_preview_unavailable),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            return@PreviewCard
        }

        val axisColor = MaterialTheme.colorScheme.outline
        val markerColor = MaterialTheme.colorScheme.primary
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val startX = 8.dp.toPx()
            val endX = size.width - 8.dp.toPx()
            val y = size.height / 2f
            drawLine(axisColor, Offset(startX, y), Offset(endX, y), strokeWidth = 2.dp.toPx())
            presetRatesMbps.forEach { preset ->
                linkScaleMarker(preset, presetRatesMbps)?.let { presetMarker ->
                    val x = startX + (endX - startX) * presetMarker.fraction
                    drawCircle(axisColor, radius = 2.dp.toPx(), center = Offset(x, y))
                }
            }
            val indicatorX = startX + (endX - startX) * marker.fraction
            drawLine(
                color = markerColor,
                start = Offset(indicatorX, 4.dp.toPx()),
                end = Offset(indicatorX, size.height - 4.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Slider(
            value = marker.fraction,
            onValueChange = { fraction ->
                presetRatesMbps.indices.minByOrNull { index ->
                    abs((linkScaleMarker(presetRatesMbps[index], presetRatesMbps)?.fraction ?: 0f) - fraction)
                }?.let { index -> onConfiguredValueChange(presetLabels[index]) }
            },
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AgentUiTags.Profile.LINK_MIN_RATE_SLIDER)
                .semantics {
                    contentDescription = configuredValue
                }
        )
        Text(
            text = stringResource(
                R.string.profile_edit_link_scale_range,
                presetLabels.first(),
                presetLabels.last()
            ),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = stringResource(R.string.profile_edit_link_effective_rate, configuredValue),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(R.string.profile_edit_link_pass_rule, configuredValue),
            style = MaterialTheme.typography.bodyMedium
        )
        if (marker.outsidePresetRange) {
            Text(
                text = stringResource(R.string.profile_edit_link_custom_outside_scale),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun PingThresholdPreview(
    maxAverageRttMs: Double?,
    maxRttMs: Double?,
    onMaxAverageRttChange: (String) -> Unit,
    onMaxRttChange: (String) -> Unit,
    maxAverageRttSliderTag: String,
    maxRttSliderTag: String
) {
    val series = illustrativeRttSeries(maxAverageRttMs, maxRttMs)
    PreviewCard(title = stringResource(R.string.profile_edit_threshold_preview_title)) {
        Text(
            text = stringResource(R.string.profile_edit_preview_illustrative),
            style = MaterialTheme.typography.bodySmall
        )
        if (series.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_edit_preview_unavailable),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            val averageColor = MaterialTheme.colorScheme.primary
            val maximumColor = MaterialTheme.colorScheme.tertiary
            val seriesColor = MaterialTheme.colorScheme.onSurfaceVariant
            val displayMax = max(
                series.maxOrNull() ?: 0.0,
                max(maxAverageRttMs ?: 0.0, maxRttMs ?: 0.0)
            ).let { if (it == 0.0) 1.0 else it * 1.1 }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
            ) {
                fun yFor(value: Double): Float =
                    size.height - (value / displayMax).coerceIn(0.0, 1.0).toFloat() * size.height

                val path = Path()
                series.forEachIndexed { index, value ->
                    val x = if (series.size == 1) 0f else size.width * index / (series.size - 1)
                    val y = yFor(value)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = seriesColor, style = Stroke(width = 2.dp.toPx()))
                maxAverageRttMs?.let { average ->
                    drawLine(
                        color = averageColor,
                        start = Offset(0f, yFor(average)),
                        end = Offset(size.width, yFor(average)),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                maxRttMs?.let { maximum ->
                    drawLine(
                        color = maximumColor,
                        start = Offset(0f, yFor(maximum)),
                        end = Offset(size.width, yFor(maximum)),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            maxAverageRttMs?.let {
                PreviewLegend(
                    color = averageColor,
                    text = stringResource(R.string.profile_edit_preview_max_avg_rtt, formatPreviewNumber(it))
                )
            }
            maxRttMs?.let {
                PreviewLegend(
                    color = maximumColor,
                    text = stringResource(R.string.profile_edit_preview_max_rtt, formatPreviewNumber(it))
                )
            }
            val sliderMaximum = rememberExpandableSliderMaximum(listOf(maxAverageRttMs, maxRttMs))
            EditableThresholdSlider(
                label = stringResource(R.string.profile_edit_threshold_avg_rtt),
                unit = "ms",
                value = maxAverageRttMs,
                rangeMaximum = sliderMaximum,
                onValueChange = onMaxAverageRttChange,
                semanticTag = maxAverageRttSliderTag
            )
            EditableThresholdSlider(
                label = stringResource(R.string.profile_edit_threshold_max_rtt),
                unit = "ms",
                value = maxRttMs,
                rangeMaximum = sliderMaximum,
                onValueChange = onMaxRttChange,
                semanticTag = maxRttSliderTag
            )
        }
    }
}

@Composable
internal fun SpeedThresholdPreview(
    downloadMbps: Double?,
    uploadMbps: Double?,
    maxPingMs: Double?,
    maxJitterMs: Double?,
    maxLossPercent: Double?,
    onDownloadChange: (String) -> Unit,
    onUploadChange: (String) -> Unit
) {
    val displayMaximum = throughputDisplayMaximum(downloadMbps, uploadMbps)
    PreviewCard(title = stringResource(R.string.profile_edit_speed_throughput_preview_title)) {
        Text(
            text = stringResource(R.string.profile_edit_preview_illustrative),
            style = MaterialTheme.typography.bodySmall
        )
        ThroughputBar(
            label = stringResource(R.string.profile_edit_download_minimum),
            value = downloadMbps,
            displayMaximum = displayMaximum,
            onValueChange = onDownloadChange,
            semanticTag = AgentUiTags.Profile.SPEED_MIN_DOWNLOAD_SLIDER
        )
        ThroughputBar(
            label = stringResource(R.string.profile_edit_upload_minimum),
            value = uploadMbps,
            displayMaximum = displayMaximum,
            onValueChange = onUploadChange,
            semanticTag = AgentUiTags.Profile.SPEED_MIN_UPLOAD_SLIDER
        )
        Text(
            text = stringResource(R.string.profile_edit_speed_slider_range),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = stringResource(R.string.profile_edit_speed_quality_limits_title),
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactMetric(
                label = stringResource(R.string.profile_edit_threshold_ping),
                value = maxPingMs,
                unit = "ms",
                modifier = Modifier.weight(1f)
            )
            CompactMetric(
                label = stringResource(R.string.profile_edit_threshold_jitter),
                value = maxJitterMs,
                unit = "ms",
                modifier = Modifier.weight(1f)
            )
            CompactMetric(
                label = stringResource(R.string.profile_edit_threshold_loss),
                value = maxLossPercent,
                unit = "%",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PreviewCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun PreviewLegend(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = color) {}
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ThroughputBar(
    label: String,
    value: Double?,
    displayMaximum: Double,
    onValueChange: (String) -> Unit,
    semanticTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (value == null) {
                label
            } else {
                stringResource(
                    R.string.profile_edit_metric_value,
                    label,
                    formatPreviewNumber(value),
                    "Mbps"
                )
            },
            style = MaterialTheme.typography.bodyMedium
        )
        if (value == null) {
            Text(
                text = stringResource(R.string.profile_edit_preview_unavailable),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            LinearProgressIndicator(
                progress = { (value / displayMaximum).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            ThroughputThresholdSlider(
                label = label,
                value = value,
                onValueChange = onValueChange,
                semanticTag = semanticTag
            )
        }
    }
}

@Composable
private fun ThroughputThresholdSlider(
    label: String,
    value: Double,
    onValueChange: (String) -> Unit,
    semanticTag: String
) {
    Slider(
        value = throughputToSliderFraction(value),
        onValueChange = { fraction ->
            onValueChange(sliderInputValue(sliderFractionToThroughput(fraction).toFloat()))
        },
        valueRange = 0f..1f,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(semanticTag)
            .semantics { contentDescription = label }
    )
}

@Composable
private fun rememberExpandableSliderMaximum(values: List<Double?>): Float {
    val requiredMaximum = adaptiveSliderMaximum(values)
        .coerceAtMost(Float.MAX_VALUE.toDouble())
        .toFloat()
    var maximum by remember { mutableFloatStateOf(requiredMaximum) }
    LaunchedEffect(requiredMaximum) {
        if (requiredMaximum > maximum) maximum = requiredMaximum
    }
    return maximum.coerceAtLeast(1f)
}

@Composable
private fun EditableThresholdSlider(
    label: String,
    unit: String,
    value: Double?,
    rangeMaximum: Float,
    onValueChange: (String) -> Unit,
    semanticTag: String,
    showValueLabel: Boolean = true
) {
    val sliderValue = value
        ?.takeIf { it.isFinite() && it >= 0.0 && it <= Float.MAX_VALUE.toDouble() }
        ?.toFloat()
        ?: return
    val effectiveMaximum = rangeMaximum.coerceAtLeast(sliderValue).coerceAtLeast(1f)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (showValueLabel) {
            Text(
                text = stringResource(
                    R.string.profile_edit_metric_value,
                    label,
                    formatPreviewNumber(value),
                    unit
                ),
                style = MaterialTheme.typography.labelSmall
            )
        }
        Slider(
            value = sliderValue.coerceIn(0f, effectiveMaximum),
            onValueChange = { onValueChange(sliderInputValue(it)) },
            valueRange = 0f..effectiveMaximum,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(semanticTag)
                .semantics { contentDescription = label }
        )
    }
}

@Composable
private fun CompactMetric(label: String, value: Double?, unit: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (value == null) {
                    label
                } else {
                    stringResource(
                        R.string.profile_edit_metric_value,
                        label,
                        formatPreviewNumber(value),
                        unit
                    )
                },
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun formatPreviewNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.ROOT, "%.1f", value)
