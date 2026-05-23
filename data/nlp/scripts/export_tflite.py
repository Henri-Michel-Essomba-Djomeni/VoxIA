import tensorflow as tf

print("Chargement du modèle...")
model = tf.keras.models.load_model("data/nlp/models/intent_classifier.keras")

print("Conversion en TFLite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

output_path = "app/src/main/assets/intent_classifier.tflite"
with open(output_path, "wb") as f:
    f.write(tflite_model)

print(f"Modèle TFLite sauvegardé dans {output_path}")