#!/usr/bin/env python3
"""
train_classifier.py - Entraînement du Intent Classifier TFLite bilingue VOXIA
==============================================================================

Génère intent_classifier.tflite à partir d'un dataset d'exemples FR/EN.

Usage:
    python data/nlp/scripts/train_classifier.py

Prérequis:
    pip install tensorflow numpy
"""

import json
import os
import sys
import numpy as np

try:
    import tensorflow as tf
    from tensorflow import keras
except ImportError:
    print("Installation de TensorFlow nécessaire...")
    os.system(f"{sys.executable} -m pip install tensorflow numpy")
    import tensorflow as tf
    from tensorflow import keras

# === CONFIGURATION ===
OUTPUT_DIR = os.path.join("app", "src", "main", "assets")
os.makedirs(OUTPUT_DIR, exist_ok=True)

INTENTS = [
    "identify_object", "read_document", "call_contact",
    "switch_to_english", "switch_to_french", "set_reminder",
    "set_alarm", "tell_story", "tell_joke", "describe_surroundings",
    "read_notification", "open_app", "calculate",
    "what_time", "what_date", "battery_status",
    "volume_up", "volume_down", "greeting",
    "repeat", "stop", "help",
    "tell_motivational", "who_are_you", "fallback"
]

# Dataset d'entraînement bilingue
TRAINING_DATA = [
    # F1 - Identification d'objets
    ("qu'est-ce que je tiens", "identify_object", "fr"),
    ("qu'est-ce que je tiens dans ma main", "identify_object", "fr"),
    ("qu'est-ce que c'est", "identify_object", "fr"),
    ("dis moi ce que tu vois", "identify_object", "fr"),
    ("identifie cet objet", "identify_object", "fr"),
    ("what am i holding", "identify_object", "en"),
    ("what is this", "identify_object", "en"),
    ("tell me what you see", "identify_object", "en"),
    ("identify this object", "identify_object", "en"),
    ("what am i holding in my hand", "identify_object", "en"),

    # F2 - Lecture de document
    ("lis ce document", "read_document", "fr"),
    ("lis cette lettre", "read_document", "fr"),
    ("lis ce que tu vois", "read_document", "fr"),
    ("aide moi à lire ce texte", "read_document", "fr"),
    ("lis à voix haute", "read_document", "fr"),
    ("read this document", "read_document", "en"),
    ("read this letter", "read_document", "en"),
    ("read what you see", "read_document", "en"),
    ("help me read this text", "read_document", "en"),
    ("read aloud", "read_document", "en"),

    # F3 - Appel
    ("appelle maman", "call_contact", "fr"),
    ("appelle papa", "call_contact", "fr"),
    ("appelle mon frere", "call_contact", "fr"),
    ("appelle ma soeur", "call_contact", "fr"),
    ("passe un appel", "call_contact", "fr"),
    ("call mom", "call_contact", "en"),
    ("call dad", "call_contact", "en"),
    ("call my brother", "call_contact", "en"),
    ("call my sister", "call_contact", "en"),
    ("make a call", "call_contact", "en"),

    # Changement de langue
    ("passe en anglais", "switch_to_english", "fr"),
    ("parle anglais", "switch_to_english", "fr"),
    ("switch to english", "switch_to_english", "en"),
    ("speak english", "switch_to_english", "en"),
    ("passe en francais", "switch_to_french", "fr"),
    ("parle francais", "switch_to_french", "fr"),
    ("switch to french", "switch_to_french", "en"),
    ("speak french", "switch_to_french", "en"),

    # Alarme et rappel
    ("reveille moi à 7 heures", "set_alarm", "fr"),
    ("mets une alarme", "set_alarm", "fr"),
    ("set alarm for 7 am", "set_alarm", "en"),
    ("wake me up at 7", "set_alarm", "en"),
    ("rappelle moi de prendre mon medicament", "set_reminder", "fr"),
    ("mets un rappel", "set_reminder", "fr"),
    ("remind me to take medicine", "set_reminder", "en"),
    ("set a reminder", "set_reminder", "en"),

    # Histoire et blague
    ("raconte moi une histoire", "tell_story", "fr"),
    ("raconte", "tell_story", "fr"),
    ("tell me a story", "tell_story", "en"),
    ("tell a story", "tell_story", "en"),
    ("raconte une blague", "tell_joke", "fr"),
    ("fais moi rire", "tell_joke", "fr"),
    ("tell a joke", "tell_joke", "en"),
    ("make me laugh", "tell_joke", "en"),

    # Description environnement
    ("decris ce qui m entoure", "describe_surroundings", "fr"),
    ("decris l environnement", "describe_surroundings", "fr"),
    ("describe my surroundings", "describe_surroundings", "en"),
    ("what is around me", "describe_surroundings", "en"),

    # Notification
    ("lis mes notifications", "read_notification", "fr"),
    ("quelles sont mes notifications", "read_notification", "fr"),
    ("read my notifications", "read_notification", "en"),
    ("check notifications", "read_notification", "en"),

    # Ouvrir application
    ("ouvre whatsapp", "open_app", "fr"),
    ("lance youtube", "open_app", "fr"),
    ("open whatsapp", "open_app", "en"),
    ("launch youtube", "open_app", "en"),

    # Calcul
    ("calcule 2 plus 2", "calculate", "fr"),
    ("combien font 5 fois 3", "calculate", "fr"),
    ("calculate 2 plus 2", "calculate", "en"),
    ("what is 5 times 3", "calculate", "en"),

    # Heure et date
    ("quelle heure est il", "what_time", "fr"),
    ("donne moi l heure", "what_time", "fr"),
    ("what time is it", "what_time", "en"),
    ("tell me the time", "what_time", "en"),
    ("quelle date sommes nous", "what_date", "fr"),
    ("quel jour sommes nous", "what_date", "fr"),
    ("what date is it", "what_date", "en"),
    ("what day is it", "what_date", "en"),

    # Batterie
    ("quel est le niveau de batterie", "battery_status", "fr"),
    ("batterie", "battery_status", "fr"),
    ("battery level", "battery_status", "en"),
    ("check battery", "battery_status", "en"),

    # Volume
    ("monte le volume", "volume_up", "fr"),
    ("augmente le son", "volume_up", "fr"),
    ("plus fort", "volume_up", "fr"),
    ("volume up", "volume_up", "en"),
    ("increase volume", "volume_up", "en"),
    ("louder", "volume_up", "en"),
    ("baisse le volume", "volume_down", "fr"),
    ("diminue le son", "volume_down", "fr"),
    ("moins fort", "volume_down", "fr"),
    ("volume down", "volume_down", "en"),
    ("decrease volume", "volume_down", "en"),
    ("quieter", "volume_down", "en"),

    # Salutation
    ("bonjour voxiA", "greeting", "fr"),
    ("salut", "greeting", "fr"),
    ("hello voxiA", "greeting", "en"),
    ("hi", "greeting", "en"),
    ("bonsoir", "greeting", "fr"),
    ("good evening", "greeting", "en"),

    # Répéter
    ("repete", "repeat", "fr"),
    ("repete s il te plait", "repeat", "fr"),
    ("repeat", "repeat", "en"),
    ("say that again", "repeat", "en"),
    ("encore", "repeat", "fr"),

    # Arrêter
    ("arrete", "stop", "fr"),
    ("tais toi", "stop", "fr"),
    ("stop", "stop", "en"),
    ("be quiet", "stop", "en"),

    # Aide
    ("aide moi", "help", "fr"),
    ("que peux tu faire", "help", "fr"),
    ("help me", "help", "en"),
    ("what can you do", "help", "en"),

    # Motivation
    ("motive moi", "tell_motivational", "fr"),
    ("donne moi de la motivation", "tell_motivational", "fr"),
    ("motivate me", "tell_motivational", "en"),
    ("give me motivation", "tell_motivational", "en"),

    # Présentation
    ("qui es tu", "who_are_you", "fr"),
    ("tu es qui", "who_are_you", "fr"),
    ("who are you", "who_are_you", "en"),
    ("what are you", "who_are_you", "en"),

    # Fallback
    ("je ne sais pas", "fallback", "fr"),
    ("peut etre", "fallback", "fr"),
    ("i don t know", "fallback", "en"),
    ("maybe", "fallback", "en"),
]

MAX_SEQ_LEN = 20
VOCAB_SIZE = 2000
EMBEDDING_DIM = 32


def build_tokenizer(data):
    all_words = set()
    for text, _, _ in data:
        for word in text.lower().split():
            all_words.add(word)
    word_to_idx = {w: i + 2 for i, w in enumerate(sorted(all_words)[:VOCAB_SIZE - 2])}
    word_to_idx["<PAD>"] = 0
    word_to_idx["<OOV>"] = 1
    return word_to_idx


def tokenize(text, word_to_idx, max_len=MAX_SEQ_LEN):
    tokens = [word_to_idx.get(w, word_to_idx["<OOV>"]) for w in text.lower().split()[:max_len]]
    tokens += [word_to_idx["<PAD>"]] * (max_len - len(tokens))
    return tokens


def main():
    print("=" * 60)
    print("VOXIA - Entraînement Intent Classifier TFLite")
    print("=" * 60)

    texts = [item[0] for item in TRAINING_DATA]
    labels_str = [item[1] for item in TRAINING_DATA]
    langs = [item[2] for item in TRAINING_DATA]

    label_to_idx = {l: i for i, l in enumerate(INTENTS)}
    labels = [label_to_idx[l] for l in labels_str]
    lang_to_idx = {"fr": 0, "en": 1}
    lang_features = [lang_to_idx[l] for l in langs]

    word_to_idx = build_tokenizer(TRAINING_DATA)
    X = np.array([tokenize(t, word_to_idx) for t in texts], dtype=np.int32)
    y = np.array(labels, dtype=np.int32)

    print(f"Échantillons: {len(X)}")
    print(f"Classes: {len(INTENTS)}")
    print(f"Vocabulaire: {len(word_to_idx)} mots")

    model = keras.Sequential([
        keras.layers.Embedding(VOCAB_SIZE, EMBEDDING_DIM, input_length=MAX_SEQ_LEN),
        keras.layers.GlobalAveragePooling1D(),
        keras.layers.Dense(64, activation="relu"),
        keras.layers.Dropout(0.3),
        keras.layers.Dense(len(INTENTS), activation="softmax"),
    ])

    model.compile(
        optimizer="adam",
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"]
    )

    model.summary()

    history = model.fit(
        X, y,
        epochs=100,
        batch_size=8,
        validation_split=0.2,
        verbose=1
    )

    final_acc = history.history["accuracy"][-1]
    final_val_acc = history.history["val_accuracy"][-1]
    print(f"\nPrécision finale: {final_acc:.2%}")
    print(f"Précision validation: {final_val_acc:.2%}")

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_model = converter.convert()

    output_path = os.path.join(OUTPUT_DIR, "intent_classifier.tflite")
    with open(output_path, "wb") as f:
        f.write(tflite_model)

    size_mb = len(tflite_model) / (1024 * 1024)
    print(f"\n✅ Modèle TFLite généré: {output_path} ({size_mb:.2f} Mo)")

    vocab_path = os.path.join(OUTPUT_DIR, "intent_vocab.json")
    vocab_data = {
        "word_to_idx": word_to_idx,
        "intents": INTENTS,
        "max_seq_len": MAX_SEQ_LEN
    }
    with open(vocab_path, "w", encoding="utf-8") as f:
        json.dump(vocab_data, f, ensure_ascii=False, indent=2)
    print(f"✅ Vocabulaire sauvegardé: {vocab_path}")

    print(f"\nPrécision globale: {final_val_acc:.2%}")
    target = "OK" if final_val_acc >= 0.70 else "⚠️ CIBLE: > 70%"
    print(f"Cible > 70%: {target}")


if __name__ == "__main__":
    main()
