import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.dates as mdates

# Load the data from the CSV file
data = pd.read_csv('hvac_sensor_data_180_days.csv', parse_dates=['timestamp'])

# Set up the figure with three subplots
fig, (ax1, ax2, ax3) = plt.subplots(3, 1, figsize=(12, 8), sharex=True)

# Plot temperature on the first subplot
ax1.plot(data['timestamp'], data['temperature'], label='Temperature (°C)', color='red')
ax1.set_ylabel('Temperature (°C)')

# Plot humidity on the second subplot
ax2.plot(data['timestamp'], data['humidity'], label='Humidity (%)', color='blue')
ax2.set_ylabel('Humidity (%)')

# Plot pressure on the third subplot
ax3.plot(data['timestamp'], data['pressure'], label='Pressure (psi)', color='green')
ax3.set_ylabel('Pressure (psi)')

# Highlight maintenance periods where maintenance_label is 1
for ax in [ax1, ax2, ax3]:
    # Identify transitions in maintenance_label (0 to 1 and 1 to 0)
    starts = data['timestamp'][data['maintenance_label'].diff() == 1]
    ends = data['timestamp'][data['maintenance_label'].diff() == -1]
    # If maintenance period extends to the end, include the last timestamp
    if len(starts) > len(ends):
        ends = pd.concat([ends, pd.Series([data['timestamp'].iloc[-1]])])
    # Shade the regions
    for start, end in zip(starts, ends):
        ax.axvspan(start, end, color='yellow', alpha=0.3)

# Format the x-axis to display dates
ax3.xaxis.set_major_locator(mdates.DayLocator(interval=7))  # Show every 7 days
ax3.xaxis.set_major_formatter(mdates.DateFormatter('%Y-%m-%d'))
plt.xticks(rotation=45)

# Add legends and a title
for ax in [ax1, ax2, ax3]:
    ax.legend()
plt.suptitle('HVAC Sensor Data with Maintenance Periods Highlighted')

# Adjust layout and display the plot
plt.tight_layout()
plt.show()
