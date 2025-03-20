import pandas as pd # For handling data in tabular format
import numpy as np  # For numerical operations and array handling
import tensorflow as tf
import coremltools as ct
from sklearn.model_selection import train_test_split # scikit-learn lib
from sklearn.preprocessing import StandardScaler     # scikit-learn lib
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
# Define a sequential neural network model
model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(3,)),             # Input layer for 3 features
    tf.keras.layers.Dense(20, activation='relu'),  # First hidden layer with 20 neurons
    tf.keras.layers.Dense(10, activation='relu'),  # Second hidden layer with 10 neurons
    tf.keras.layers.Dense(1, activation='sigmoid') # Output layer for binary classification
])

# Compile the model with a custom learning rate
## Adam (Adaptive Moment Estimation) is a popular optimization algorithm that adjusts the model’s weights during training to minimize the loss.
## binary_crossentropy - This measures the difference between the predicted probabilities (from the sigmoid output) and the true binary labels (0 or 1). It’s the standard loss for binary classification.
optimizer = tf.keras.optimizers.Adam(learning_rate=0.001)
model.compile(optimizer=optimizer, loss='binary_crossentropy', metrics=['accuracy'])

print("Step 4: Training the model...")
# Define an EarlyStopping callback to monitor validation loss and stop training if it doesn't improve
early_stopping = EarlyStopping(
    monitor='val_loss',          # Metric to monitor (validation loss)
    patience=5,                  # Number of epochs to wait for improvement before stopping
    restore_best_weights=True    # Restore the model weights from the epoch with the best validation loss
)

# Train the neural network model using the training data
model.fit(
    X_train,                     # Training features (input data)
    y_train,                     # Training labels (target data)
    epochs=50,                   # Maximum number of times to iterate over the entire dataset
    batch_size=64,               # Number of samples processed before updating the model's weights
    validation_split=0.2,        # Fraction of training data (20%) to use for validation
    callbacks=[early_stopping]   # List of callbacks to apply during training (here, EarlyStopping)
)

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


### Key Terms Explained on printout

#### What is an Epoch?
# - An epoch is one complete pass through your entire training dataset. => epochs=10 - meaning the model processes all the training data 10 times.
# - Why it matters: Each epoch gives the model a chance to learn from the data. More epochs can improve learning, but too many might cause the model to overfit (memorizing the training data instead of generalizing to new data).

#### What is Loss?
# - Loss measures how far off the model’s predictions are from the actual labels. Since we are doing binary classification (predicting 0 or 1 for `maintenance_label`) - model uses "binary_crossentropy" as the loss function. This calculates the error between the predicted probabilities (e.g., 0.7) and the true labels (e.g., 1).
# - Why it matters: A lower loss means the model’s predictions are closer to the truth. During training, you want the "loss" to decrease over time.

#### What is Accuracy?
# - Accuracy is the percentage of correct predictions. In our case, it’s the proportion of HVAC sensor samples where the model correctly predicts whether maintenance is needed (0 or 1).
# - Why it matters: Higher accuracy means more correct predictions, making it an easy way to judge how well the model is performing.

#### What is us/step?
# - us/step stands for microseconds per step, where a "step" is the time it takes to process one batch of data during training.
# - Why it matters: This is about training speed, not model quality. Faster steps (lower us/step) mean quicker training.



