//
//  ContentManager.swift
//  IoTAI-Demo-iOS
//
//  Created by Matija Kruljac on 11.03.2025.
//

import CoreML
import Foundation

class ContentManager: ObservableObject {

    @Published var currentProbability: Float?
    @Published var maintenanceNeeded: Bool = false
    @Published var probabilityHistory: [Float] = []

    @Published var stepCurrentValue: String = ""
    @Published var tempCurrentValue: String = ""
    @Published var humiCurrentValue: String = ""
    @Published var pressCurrentValue: String = ""

    // MARK: - Normalization Parameters

    /// Normalization constants based on actual training data statistics
    let tempMean: Float = 21.1    // Mean temperature (°C)
    let tempStd: Float = 1.1      // Standard deviation of temperature (°C)
    let humidMean: Float = 50.0   // Mean humidity (%)
    let humidStd: Float = 5.0     // Standard deviation of humidity (%)
    let pressMean: Float = 300.0  // Mean pressure (psi)
    let pressStd: Float = 10.0    // Standard deviation of pressure (psi)

    // MARK: - Prediction Logic

    /// Runs the HVAC maintenance prediction simulation
    func startPredicting() {
        DispatchQueue.global(qos: .background).async {
            do {
                // Initialize the Core ML model
                let model = try HVACModel(configuration: MLModelConfiguration())

                let numberOfSteps = 70

                // Generate a sequence of 'numberOfSteps' inputs simulating a trend
                let sequence = self.generateTrendSequence(steps: numberOfSteps)

                // Process each input with a 0.5 second delay
                for (index, input) in sequence.enumerated() {
                    let multiArray = self.hvacInputToMultiArray(input)
                    let modelInput = HVACModelInput(input_1: multiArray)
                    let prediction = try model.prediction(input: modelInput)

                    // Extract the probability from the model's output
                    let probability = prediction.Identity[0].floatValue

                    // Print sensor values and prediction result
                    print("Step \(index + 1): Temp = \(String(format: "%.2f", input.temperature))°C, " +
                          "Hum = \(String(format: "%.2f", input.humidity))%, " +
                          "Press = \(String(format: "%.2f", input.pressure)) psi -> " +
                          "Probability: \(String(format: "%.3f", probability))")

                    // Update properties on the main thread
                    DispatchQueue.main.async {
                        self.currentProbability = probability
                        self.maintenanceNeeded = probability > 0.5
                        self.probabilityHistory.append(probability)

                        self.stepCurrentValue = "Step = \(index + 1)/\(numberOfSteps)"
                        self.tempCurrentValue = "Temp = \(String(format: "%.2f", input.temperature)) °C"
                        self.humiCurrentValue = "Hum = \(String(format: "%.2f", input.humidity)) %"
                        self.pressCurrentValue = "Press = \(String(format: "%.2f", input.pressure)) psi"
                    }

                    // Simulate real-time data emission with a 0.5 second delay
                    Thread.sleep(forTimeInterval: 0.5)
                }
            } catch {
                print("Prediction failed: \(error)")
            }
        }
    }

    // MARK: - Data Structures

    /// Struct to represent HVAC input features
    struct HVACInput {
        let temperature: Float  // °C
        let humidity: Float     // %
        let pressure: Float     // psi
    }

    // MARK: - Helper Functions

    /// Generates a sequence of HVAC inputs simulating a trend toward maintenance and recovery
    /// - Parameter steps: Number of steps in the sequence (minimum 20 to accommodate post-maintenance phase)
    /// - Returns: Array of HVACInput structs
    private func generateTrendSequence(steps: Int) -> [HVACInput] {
        var sequence: [HVACInput] = []
        let totalSteps = max(steps, 20) // Ensure at least 20 steps for meaningful phases
        let postMaintenanceSteps = min(20, totalSteps / 3) // Last 15–20 steps are post-maintenance
        let stableSteps = (totalSteps - postMaintenanceSteps) / 2 // First phase: stable operation
        let deteriorationSteps = totalSteps - stableSteps - postMaintenanceSteps // Middle phase: deterioration

        // Starting values close to the training means
        let startTemp: Float = 21.1
        let startHumidity: Float = 50.0
        let startPressure: Float = 300.0

        // Deterioration values (temp up 5°C, humidity up 15%, pressure down 100 psi)
        let deterioratedTemp = startTemp + 5
        let deterioratedHumidity = startHumidity + 15
        let deterioratedPressure = startPressure - 100

        for i in 0..<totalSteps {
            if i < stableSteps {
                // Phase 1: Stable operation with small random noise
                let temp = startTemp + Float.random(in: -0.5...0.5)
                let hum = startHumidity + Float.random(in: -1...1)
                let press = startPressure + Float.random(in: -2...2)
                sequence.append(HVACInput(temperature: temp, humidity: hum, pressure: press))
            } else if i < stableSteps + deteriorationSteps {
                // Phase 2: Gradual deterioration toward deteriorated values
                let fraction = Float(i - stableSteps) / Float(deteriorationSteps - 1)
                let temp = startTemp + fraction * (deterioratedTemp - startTemp) + Float.random(in: -0.5...0.5)
                let hum = startHumidity + fraction * (deterioratedHumidity - startHumidity) + Float.random(in: -1...1)
                let press = startPressure + fraction * (deterioratedPressure - startPressure) + Float.random(in: -2...2)
                sequence.append(HVACInput(temperature: temp, humidity: hum, pressure: press))
            } else {
                // Phase 3: Post-maintenance - return to normal conditions
                let temp = startTemp + Float.random(in: -0.5...0.5)
                let hum = startHumidity + Float.random(in: -1...1)
                let press = startPressure + Float.random(in: -2...2)
                sequence.append(HVACInput(temperature: temp, humidity: hum, pressure: press))
            }
        }

        return sequence
    }

    /// Converts HVACInput to a normalized MLMultiArray for model input
    /// - Parameter input: HVACInput struct with raw sensor values
    /// - Returns: Normalized MLMultiArray
    private func hvacInputToMultiArray(_ input: HVACInput) -> MLMultiArray {
        // shape: [1, 3] => This tells MLMultiArray to create a 2D array with 1 row and 3 columns
        // Example: [ [temperature, humidity, pressure] ]
        let multiArray = try! MLMultiArray(shape: [1, 3], dataType: .float32)

        // Normalize each feature using (value - mean) / std
        let normalizedTemp = (input.temperature - tempMean) / tempStd
        let normalizedHumidity = (input.humidity - humidMean) / humidStd
        let normalizedPressure = (input.pressure - pressMean) / pressStd
        
        // Assign normalized values to the multi-array
        multiArray[0] = NSNumber(value: normalizedTemp)
        multiArray[1] = NSNumber(value: normalizedHumidity)
        multiArray[2] = NSNumber(value: normalizedPressure)

        return multiArray
    }
}
