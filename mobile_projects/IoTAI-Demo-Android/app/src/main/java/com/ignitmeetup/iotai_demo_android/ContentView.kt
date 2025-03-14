package com.ignitmeetup.iotai_demo_android

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContentView(contentManager: ContentManager, modifier: Modifier = Modifier) {

    val currentProbability = contentManager.currentProbability.collectAsState().value
    val maintenanceNeeded = contentManager.maintenanceNeeded.collectAsState().value
    val probabilityHistory = contentManager.probabilityHistory.collectAsState().value
    val stepCurrentValue = contentManager.stepCurrentValue.collectAsState().value
    val tempCurrentValue = contentManager.tempCurrentValue.collectAsState().value
    val humiCurrentValue = contentManager.humiCurrentValue.collectAsState().value
    val pressCurrentValue = contentManager.pressCurrentValue.collectAsState().value

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // HVAC Title
        Text(
            text = "- HVAC -",
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Status Message
        Text(
            text = when {
                maintenanceNeeded -> "😵 MAINTENANCE NEEDED"
                currentProbability == null -> "😴"
                else -> "🙂 System OK"
            },
            color = if (maintenanceNeeded) Color.Red else Color.Green,
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp)
        )

        // Current Probability
        currentProbability?.let { probability ->
            Text(
                text = "Current Probability: %.3f".format(probability),
                color = Color.White,
                fontSize = 16.sp, // Headline approximates to ~16sp
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Probability Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Gray.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            ProbabilityChart(probabilityHistory)
        }

        // Current Values
        if (probabilityHistory.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth() // Ensure the Column takes full width
                    .padding(top = 32.dp)
            ) {
                Text(
                    text = "- current values -",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth() // Optional, ensures text spans full width
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, // Explicitly set for clarity
                    modifier = Modifier.fillMaxWidth() // Ensure nested Column takes full width
                ) {
                    Text(
                        text = stepCurrentValue,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth() // Optional
                    )
                    Text(
                        text = tempCurrentValue,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth() // Optional
                    )
                    Text(
                        text = humiCurrentValue,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth() // Optional
                    )
                    Text(
                        text = pressCurrentValue,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth() // Optional
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(120.dp)) // Minimum length from the bottom
    }

    // Start prediction on composition
    LaunchedEffect(Unit) {
        contentManager.startPredicting()
    }
}

// Custom Probability Chart
@Composable
fun ProbabilityChart(probabilities: List<Float>) {
    val maxPoints = 70
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val points = probabilities.takeLast(maxPoints)
        if (points.isEmpty()) return@Canvas

        // Y-axis grid lines (0, 0.2, 0.4, 0.6, 0.8, 1.0)
        for (i in 0..5) {
            val y = height - (i * 0.2f) * height
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            // Y-axis labels
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 12.sp.toPx()
                }
                canvas.nativeCanvas.drawText("%.1f".format(i * 0.2f), 0f, y - 5f, paint)
            }
        }

        // X-axis grid lines (every 10 steps)
        val stepX = width / (maxPoints - 1)
        for (i in 0 until maxPoints step 10) {
            val x = i * stepX
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            // X-axis labels
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 12.sp.toPx()
                }
                canvas.nativeCanvas.drawText(i.toString(), x, height + 15f, paint)
            }
        }

        // Draw probability line
        val path = Path()
        path.moveTo(0f, height - points[0] * height)
        for (i in 1 until points.size) {
            path.lineTo(i * stepX, height - points[i] * height)
        }
        drawPath(
            path = path,
            color = Color(0xFF42A4F5),
            style = Stroke(width = 4.dp.toPx())
        )
    }
}