//
//  ContentView.swift
//  IoTAI-Demo-iOS
//
//  Created by Matija Kruljac on 11.03.2025..
//

import SwiftUI
import Charts

struct ContentView: View {

    @ObservedObject var contentManager = ContentManager()

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Text("- HVAC -")
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .font(Font.system(size: 18, weight: .bold))
                .padding(.horizontal)

            if contentManager.maintenanceNeeded {
                Text("😵 MAINTENANCE NEEDED")
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                    .font(Font.system(size: 24, weight: .bold))
                    .padding(.top)
                    .padding(.horizontal)
            } else {
                Text(contentManager.currentProbability == nil ? "😴" : "🙂 System OK")
                    .foregroundColor(.green)
                    .multilineTextAlignment(.center)
                    .font(Font.system(size: 24, weight: .bold))
                    .padding(.top)
                    .padding(.horizontal)
            }

            // Display current probability
            if let probability = contentManager.currentProbability {
                Text("Current Probability: \(String(format: "%.3f", probability))")
                    .foregroundColor(.white)
                    .font(.headline)
            }

            // Live graph of probability history
            Chart {
                ForEach(Array(contentManager.probabilityHistory.enumerated()), id: \.offset) { index, probability in
                    LineMark(
                        x: .value("Step", index),
                        y: .value("Probability", probability)
                    )
                    .foregroundStyle(.clear) // Makes the default line invisible
                }
            }
            .chartOverlay { proxy in
                GeometryReader { geometry in
                    Path { path in
                        for (index, probability) in contentManager.probabilityHistory.enumerated() {
                            // Convert chart data to view coordinates
                            if let x = proxy.position(forX: index),
                               let y = proxy.position(forY: probability) {
                                let point = CGPoint(x: x, y: y)
                                if path.isEmpty {
                                    path.move(to: point)
                                } else {
                                    path.addLine(to: point)
                                }
                            }
                        }
                    }
                    .stroke(Color.blue, style: StrokeStyle(lineWidth: 4))
                }
            }
            .frame(height: 200)
            .padding()
            .chartYScale(domain: 0...1) // Probability range: 0 to 1
            .chartXAxis {
                AxisMarks(values: .automatic) { _ in
                    AxisGridLine()
                        .foregroundStyle(Color.white.opacity(0.4))
                    AxisTick()
                    AxisValueLabel()
                        .foregroundStyle(Color.white)
                }
            }
            .chartYAxis {
                AxisMarks(values: .automatic) { _ in
                    AxisGridLine()
                        .foregroundStyle(Color.white.opacity(0.4))
                    AxisTick()
                    AxisValueLabel()
                        .foregroundStyle(Color.white)
                }
            }
            .chartBackground { _ in
                Color.gray.opacity(0.3)
            }

            if !contentManager.probabilityHistory.isEmpty {
                VStack(spacing: 8) {
                    Text("- current values -")
                        .foregroundColor(.white)
                        .font(Font.system(size: 18, weight: .bold))
                    VStack(spacing: 4) {
                        Text(contentManager.stepCurrentValue)
                            .foregroundColor(.white)
                            .font(Font.system(size: 15, weight: .semibold))
                        Text(contentManager.tempCurrentValue)
                            .foregroundColor(.white)
                            .font(Font.system(size: 15, weight: .semibold))
                        Text(contentManager.humiCurrentValue)
                            .foregroundColor(.white)
                            .font(Font.system(size: 15, weight: .semibold))
                        Text(contentManager.pressCurrentValue)
                            .foregroundColor(.white)
                            .font(Font.system(size: 15, weight: .semibold))
                    }
                }
                .padding(.top, 32)
            }

            Spacer(minLength: 120)
        }
        .background(Color.black)
        .padding()
        .onAppear {
            // Start prediction loop
            contentManager.startPredicting()
        }
    }
}

#Preview {
    ContentView()
}
