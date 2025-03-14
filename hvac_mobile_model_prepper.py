import pandas as pd
import numpy as np
import tensorflow as tf
import coremltools as ct
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from tensorflow.keras.callbacks import EarlyStopping
import os

print("Step 1: Loading the HVAC sensor data...")
try:
    df = pd.read_csv('hvac_sensor_data_180_days.csv')
except FileNotFoundError:
    print("Error: 'hvac_sensor_data_180_days.csv' not found. Please ensure the file is in the current directory.")
    exit(1)

print("Step 2: Preprocessing the data...")
# Drop 'timestamp' column if it exists
if 'timestamp' in df.columns:
    df = df.drop('timestamp', axis=1)
else:
    print("Warning: 'timestamp' column not found in the dataset.")

# Handle missing values
if df.isnull().values.any():
    print("Warning: Missing values detected. Dropping rows with missing values.")
    df = df.dropna()

# Validate binary labels in 'maintenance_label'
if not df['maintenance_label'].isin([0, 1]).all():
    print("Error: 'maintenance_label' contains non-binary values. Please check the dataset.")
    exit(1)

# Define features (X) and labels (y)
X = df[['temperature', 'humidity', 'pressure']].values
y = df['maintenance_label'].values

# Standardize the features
scaler = StandardScaler()
X = scaler.fit_transform(X)

# Split into training and test sets
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

print("Step 3: Defining the model for binary classification...")
model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(3,)),            # Input layer for 3 features
    tf.keras.layers.Dense(20, activation='relu'), # First hidden layer with 20 neurons
    tf.keras.layers.Dense(10, activation='relu'), # Second hidden layer with 10 neurons
    tf.keras.layers.Dense(1, activation='sigmoid') # Output layer for binary classification
])

# Compile the model with a custom learning rate
optimizer = tf.keras.optimizers.Adam(learning_rate=0.001)
model.compile(optimizer=optimizer, loss='binary_crossentropy', metrics=['accuracy'])

print("Step 4: Training the model...")
early_stopping = EarlyStopping(monitor='val_loss', patience=5, restore_best_weights=True)
model.fit(X_train, y_train, epochs=50, batch_size=64, validation_split=0.2, callbacks=[early_stopping])

print("Step 5: Evaluating the model on the test set...")
test_loss, test_accuracy = model.evaluate(X_test, y_test)
print(f'Test accuracy: {test_accuracy:.4f}')

print("Step 6: Creating directory 'models' if it does not exist...")
os.makedirs('models', exist_ok=True)

print("Step 7: Saving the trained model in native Keras format...")
model.save('models/trained_model.keras')

print("Step 8: Converting to Core ML format for iOS...")
coreml_model = ct.convert(
    model,
    source='tensorflow',
    inputs=[ct.TensorType(shape=(1, 3))]
)
coreml_model.save('models/HVACModel.mlpackage')

print("Step 9: Converting to TensorFlow Lite format for Android...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()
with open('models/HVACModel.tflite', 'wb') as f:
    f.write(tflite_model)

print("Step 10: Conversion complete!")
print("Models have been successfully converted and saved as 'HVACModel.mlpackage' for iOS and 'HVACModel.tflite' for Android.")
