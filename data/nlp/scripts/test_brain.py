import json
import numpy as np
from sklearn.preprocessing import LabelEncoder
from sklearn.feature_extraction.text import TfidfVectorizer
import tensorflow as tf
import pickle

# ── Recharger le modèle et les outils ──────────────────
print("Chargement du modèle...")

# Recharger les données pour reconstruire vectorizer et encoder
with open("data/nlp/datasets/intentions_voxia.json", "r", encoding="utf-8") as f:
    data = json.load(f)

texts = []
labels = []
for item in data:
    texts.append(item["fr"])
    labels.append(item["intention"])
    texts.append(item["en"])
    labels.append(item["intention"])

# Reconstruire vectorizer et encoder
vectorizer = TfidfVectorizer()
X = vectorizer.fit_transform(texts).toarray()

encoder = LabelEncoder()
encoder.fit_transform(labels)

# Charger le modèle TFLite
interpreter = tf.lite.Interpreter(
    model_path="app/src/main/assets/intent_classifier.tflite"
)
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# ── Fonction de prédiction ─────────────────────────────
def predict(text):
    # Vectoriser le texte
    X_input = vectorizer.transform([text]).toarray().astype(np.float32)

    # Inférence TFLite
    interpreter.set_tensor(input_details[0]['index'], X_input)
    interpreter.invoke()
    output = interpreter.get_tensor(output_details[0]['index'])

    # Résultat
    predicted_index = np.argmax(output[0])
    confidence = output[0][predicted_index]
    intention = encoder.classes_[predicted_index]

    return intention, confidence

# ── Détection de langue simple ─────────────────────────
def detect_language(text):
    french_words = ["lis", "quoi", "est", "que", "appelle", "dis",
                   "bonjour", "salut", "heure", "raconte", "répète"]
    english_words = ["read", "what", "call", "tell", "hello", "hey",
                    "time", "repeat", "identify", "describe"]
    
    words = text.lower().split()
    fr_score = sum(1 for w in words if w in french_words)
    en_score = sum(1 for w in words if w in english_words)
    
    if fr_score > en_score:
        return "FRENCH"
    elif en_score > fr_score:
        return "ENGLISH"
    else:
        return "UNKNOWN"

# ── Tests ──────────────────────────────────────────────
print("\n========== TESTS BRAIN VOXIA ==========\n")

test_phrases = [
    # Français
    ("Qu'est-ce que je tiens ?", "identify_object", "FRENCH"),
    ("Lis ce document", "read_document", "FRENCH"),
    ("Appelle maman", "call_contact", "FRENCH"),
    ("Passe en anglais", "switch_to_english", "FRENCH"),
    ("Quelle heure est-il ?", "what_time", "FRENCH"),
    ("Raconte-moi une histoire", "tell_story", "FRENCH"),
    ("Dis-moi une blague", "tell_joke", "FRENCH"),
    ("Aide-moi", "help", "FRENCH"),
    ("Arrête", "stop", "FRENCH"),
    ("Répète", "repeat", "FRENCH"),
    # Anglais
    ("What am I holding ?", "identify_object", "ENGLISH"),
    ("Read this document", "read_document", "ENGLISH"),
    ("Call mom", "call_contact", "ENGLISH"),
    ("Switch to French", "switch_to_french", "ENGLISH"),
    ("What time is it ?", "what_time", "ENGLISH"),
    ("Tell me a story", "tell_story", "ENGLISH"),
    ("Tell me a joke", "tell_joke", "ENGLISH"),
    ("Help me", "help", "ENGLISH"),
    ("Stop", "stop", "ENGLISH"),
    ("Repeat", "repeat", "ENGLISH"),
]

correct = 0
total = len(test_phrases)

for phrase, expected_intent, expected_lang in test_phrases:
    intention, confidence = predict(phrase)
    language = detect_language(phrase)
    
    intent_ok = intention == expected_intent
    lang_ok = language == expected_lang
    
    status = "✅" if intent_ok else "❌"
    lang_status = "✅" if lang_ok else "❌"
    
    if intent_ok:
        correct += 1
    
    print(f"{status} [{language}] {lang_status}")
    print(f"   Phrase    : {phrase}")
    print(f"   Attendu   : {expected_intent}")
    print(f"   Prédit    : {intention} ({confidence*100:.1f}%)")
    print()

print("========================================")
print(f"Score final : {correct}/{total} ({correct/total*100:.1f}%)")
print("========================================")