import pandas as pd
import numpy as np
from datetime import datetime, timedelta

# Set random seed for reproducibility
np.random.seed(42)

# Define 180 days of hourly data
start_date = datetime(2024, 1, 1, 0, 0, 0)
end_date = start_date + timedelta(days=180) - timedelta(hours=1)  # Ends at 23:00 on day 180

# Generate hourly timestamps
timestamps = pd.date_range(start=start_date, end=end_date, freq='h')
n = len(timestamps)

# Generate temperature in Celsius: 21.1°C base, ±2.8°C daily cycle, N(0, 1.1) noise - mean 0 and standard deviation 1.1.
hours = timestamps.hour.values
temperature = 21.1 + 2.8 * np.sin(2 * np.pi * hours / 24) + np.random.normal(0, 1.1, n)

# Generate humidity (%): 50% base, ±10% daily cycle (shifted by π), N(0, 5) noise - mean 0 and standard deviation 5.
humidity = 50 + 10 * np.sin(2 * np.pi * hours / 24 + np.pi) + np.random.normal(0, 5, n)

# Generate pressure (psi): 300 psi base, N(0, 10) noise - mean 0 and standard deviation 10.
pressure = 300 + np.random.normal(0, 10, n)

# Randomly select 5 maintenance events after the first 7 days
maintenance_indices = np.random.choice(len(timestamps[24*7:]), size=5, replace=False)
maintenance_dates = timestamps[24*7:][maintenance_indices]

print('\nOver the 7 days before the event of maintenance, we create a trend to simulate deteriorating conditions where:')
print('- temperature increases by up to 5°C')
print('- humidity by up to 15%')
print('- pressure decreases by up to 100 psi')

# Apply trends with order numbers
for i, m in enumerate(maintenance_dates, start=1):
    start = m - timedelta(days=7)
    print(f'Trend {i} starts at: {start}')
    mask = (timestamps >= start) & (timestamps < m)
    time_diff = (timestamps[mask] - start).total_seconds().values / (7 * 24 * 3600)  # 0 to 1 over 7 days
    
    # Modify the arrays directly
    temperature[mask] += 5 * time_diff         # Temperature increases by 5°C
    humidity[mask] += 15 * time_diff           # Humidity increases by 15%
    pressure[mask] -= 100 * time_diff          # Pressure decreases by 100 psi

# Function to label maintenance within 7 days
def has_maintenance_within_7_days(t, maintenance_dates):
    end = t + timedelta(days=7)
    return any(t < m <= end for m in maintenance_dates)

# Generate labels
labels = [1 if has_maintenance_within_7_days(t, maintenance_dates) else 0 for t in timestamps]

# Create DataFrame
df = pd.DataFrame({
    'timestamp': timestamps,
    'temperature': temperature,
    'humidity': humidity,
    'pressure': pressure,
    'maintenance_label': labels
})

# Save to CSV
df.to_csv('hvac_sensor_data_180_days.csv', index=False)

print("\nCSV file 'hvac_sensor_data_180_days.csv' has been generated successfully!")
print("\nFirst 5 rows of the dataset:")
print(df.head())
print('\n')
