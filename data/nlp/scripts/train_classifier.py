import json
import numpy as np
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
import tensorflow as tf
import os

print("Chargement des intentions...")

# Charger le dataset
with open("data/nlp/datasets/intentions_voxia.json", "r", encoding="utf-8") as f:
    data = json.load(f)

# Préparer les données FR + EN ensemble
texts = []
labels = []

for item in data:
    texts.append(item["fr"])
    labels.append(item["intention"])
    texts.append(item["en"])
    labels.append(item["intention"])

print(f"Total exemples : {len(texts)}")
print(f"Intentions uniques : {len(set(labels))}")

# Encoder les labels
encoder = LabelEncoder()
labels_encoded = encoder.fit_transform(labels)

# Vectoriser le texte
vectorizer = TfidfVectorizer()
X = vectorizer.fit_transform(texts).toarray()

print(f"Taille du vocabulaire : {X.shape[1]}")

# Séparer train/test
X_train, X_test, y_train, y_test = train_test_split(
    X, labels_encoded, test_size=0.2, random_state=42
)

print("Entraînement du modèle...")

# Créer le modèle
model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(X_train.shape[1],)),
    tf.keras.layers.Dense(128, activation="relu"),
    tf.keras.layers.Dropout(0.3),
    tf.keras.layers.Dense(64, activation="relu"),
    tf.keras.layers.Dense(len(set(labels)), activation="softmax")
])

model.compile(
    optimizer="adam",
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"]
)

model.fit(
    X_train, y_train,
    epochs=50,
    batch_size=16,
    validation_split=0.1
)

# Evaluer
loss, accuracy = model.evaluate(X_test, y_test)
print(f"Précision : {accuracy * 100:.2f}%")

# Sauvegarder
os.makedirs("data/nlp/models", exist_ok=True)
model.save("data/nlp/models/intent_classifier.keras")
print("Modèle sauvegardé dans data/nlp/models/")