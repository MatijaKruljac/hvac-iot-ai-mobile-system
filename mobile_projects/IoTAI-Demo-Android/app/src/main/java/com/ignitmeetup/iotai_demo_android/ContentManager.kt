package com.ignitmeetup.iotai_demo_android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ContentManager(
    private val interpreter: Interpreter
) : ViewModel() {

    private val _currentProbability = MutableStateFlow<Float?>(null)
    val currentProbability: StateFlow<Float?> = _currentProbability

    private val _maintenanceNeeded = MutableStateFlow(false)
    val maintenanceNeeded: StateFlow<Boolean> = _maintenanceNeeded

    private val _probabilityHistory = MutableStateFlow<List<Float>>(emptyList())
    val probabilityHistory: StateFlow<List<Float>> = _probabilityHistory

    private val _stepCurrentValue = MutableStateFlow("")
    val stepCurrentValue: StateFlow<String> = _stepCurrentValue

    private val _tempCurrentValue = MutableStateFlow("")
    val tempCurrentValue: StateFlow<String> = _tempCurrentValue

    private val _humiCurrentValue = MutableStateFlow("")
    val humiCurrentValue: StateFlow<String> = _humiCurrentValue

    private val _pressCurrentValue = MutableStateFlow("")
    val pressCurrentValue: StateFlow<String> = _pressCurrentValue

    // Normalization constants
    private val tempMean: Float = 21.1f    // Mean temperature (°C)
    private val tempStd: Float = 1.1f      // Standard deviation of temperature (°C)
    private val humidMean: Float = 50.0f   // Mean humidity (%)
    private val humidStd: Float = 5.0f     // Standard deviation of humidity (%)
    private val pressMean: Float = 300.0f  // Mean pressure (psi)
    private val pressStd: Float = 10.0f    // Standard deviation of pressure (psi)

    // HVAC input data class
    data class HVACInput(
        val temperature: Float,  // °C
        val humidity: Float,     // %
        val pressure: Float      // psi
    )

    // Start the prediction simulation
    fun startPredicting() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val numberOfSteps = 70

                val sequence = generateTrendSequence(numberOfSteps)

                for ((index, input) in sequence.withIndex()) {
                    // Normalize input and prepare TFLite input buffer
                    val inputBuffer = normalizeInput(input) // Input preparation logic
                    val output = Array(1) { FloatArray(1) } // Matches [1, 1] shape
                    interpreter.run(inputBuffer, output)
                    val probability = output[0][0].coerceIn(0f, 1f) // Ensure value is between 0 and 1

                    // Log for debugging
                    println(
                        "Step ${index + 1}: Temp = %.2f°C, Hum = %.2f%%, Press = %.2f psi -> Probability: %.3f"
                            .format(input.temperature, input.humidity, input.pressure, probability)
                    )

                    // Update UI state on the main thread
                    withContext(Dispatchers.Main) {
                        _currentProbability.value = probability
                        _maintenanceNeeded.value = probability > 0.5f
                        _probabilityHistory.value += probability

                        _stepCurrentValue.value = "Step = ${index + 1} / $numberOfSteps"
                        _tempCurrentValue.value = "Temp = %.2f °C".format(input.temperature)
                        _humiCurrentValue.value = "Hum = %.2f %%".format(input.humidity)
                        _pressCurrentValue.value = "Press = %.2f psi".format(input.pressure)
                    }

                    delay(500) // 0.5 second delay to simulate real-time data
                }
            } catch (e: Exception) {
                println("Prediction failed: $e")
            }
        }
    }

    // Generate a sequence of HVAC inputs
    private fun generateTrendSequence(steps: Int): List<HVACInput> {
        val sequence = mutableListOf<HVACInput>()
        val totalSteps = maxOf(steps, 20)
        val postMaintenanceSteps = minOf(20, totalSteps / 3)
        val stableSteps = (totalSteps - postMaintenanceSteps) / 2
        val deteriorationSteps = totalSteps - stableSteps - postMaintenanceSteps

        val startTemp = 21.1f
        val startHumidity = 50.0f
        val startPressure = 300.0f

        val deterioratedTemp = startTemp + 5f
        val deterioratedHumidity = startHumidity + 15f
        val deterioratedPressure = startPressure - 100f

        for (i in 0 until totalSteps) {
            when {
                i < stableSteps -> {
                    val temp = startTemp + (-0.5f..0.5f).random()
                    val hum = startHumidity + (-1f..1f).random()
                    val press = startPressure + (-2f..2f).random()
                    sequence.add(HVACInput(temp, hum, press))
                }

                i < stableSteps + deteriorationSteps -> {
                    val fraction = (i - stableSteps).toFloat() / (deteriorationSteps - 1)
                    val temp =
                        startTemp + fraction * (deterioratedTemp - startTemp) + (-0.5f..0.5f).random()
                    val hum =
                        startHumidity + fraction * (deterioratedHumidity - startHumidity) + (-1f..1f).random()
                    val press =
                        startPressure + fraction * (deterioratedPressure - startPressure) + (-2f..2f).random()
                    sequence.add(HVACInput(temp, hum, press))
                }

                else -> {
                    val temp = startTemp + (-0.5f..0.5f).random()
                    val hum = startHumidity + (-1f..1f).random()
                    val press = startPressure + (-2f..2f).random()
                    sequence.add(HVACInput(temp, hum, press))
                }
            }
        }
        return sequence
    }

    // Normalize input for TFLite model
    private fun normalizeInput(input: HVACInput): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(3 * 4) // 3 floats, 4 bytes each
        buffer.order(ByteOrder.nativeOrder())
        buffer.putFloat((input.temperature - tempMean) / tempStd)
        buffer.putFloat((input.humidity - humidMean) / humidStd)
        buffer.putFloat((input.pressure - pressMean) / pressStd)
        buffer.rewind()
        return buffer
    }

    // Extension function to generate random Float in range
    private fun ClosedRange<Float>.random(): Float =
        (endInclusive - start) * kotlin.random.Random.nextFloat() + start
}
