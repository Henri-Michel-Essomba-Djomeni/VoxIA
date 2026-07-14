#!/usr/bin/env python3
"""
VoxIA - Script de collecte audio pour fine-tuning Vosk
Enregistre des phrases dans les deux langues avec accents locaux
Usage: python scripts/collect_audio.py
"""

import os
import time
import json
import datetime
import sounddevice as sd
import numpy as np
from scipy.io.wavfile import write as wav_write

# ─── CONFIGURATION ────────────────────────────────────
SAMPLE_RATE = 16000      # Vosk fonctionne à 16kHz
CHANNELS = 1             # Mono
RECORD_SECONDS = 4       # Durée max par phrase

OUTPUT_DIR_FR = "data/speech/fr_accents"
OUTPUT_DIR_EN = "data/speech/en_accents"
METADATA_FILE = "data/speech/metadata.json"

# ─── PHRASES À ENREGISTRER ────────────────────────────
PHRASES_FR = [
    "Qu'est-ce que je tiens ?",
    "Qu'est-ce que je tiens dans ma main ?",
    "Lis ce document",
    "Lis cette lettre",
    "Appelle maman",
    "Appelle papa",
    "Appelle mon frère",
    "Appelle ma sœur",
    "Passe en anglais",
    "Qu'est-ce que c'est ?",
    "Dis-moi ce que tu vois",
    "Aide-moi à lire ce texte",
    "VOXIA",
    "VOXIA aide-moi",
    "Arrête",
    "Répète",
    "Plus lentement",
    "Merci",
]

PHRASES_EN = [
    "What am I holding?",
    "What am I holding in my hand?",
    "Read this document",
    "Read this letter",
    "Call mom",
    "Call dad",
    "Call my brother",
    "Call my sister",
    "Switch to French",
    "What is this?",
    "Tell me what you see",
    "Help me read this text",
    "VOXIA",
    "VOXIA help me",
    "Stop",
    "Repeat",
    "Slower",
    "Thank you",
]

def record_phrase(phrase, output_path, duration=RECORD_SECONDS):
    """Enregistre une phrase et la sauvegarde en WAV"""
    print(f"\n📢 Dites : \"{phrase}\"")
    time.sleep(1)
    print("⏺  GO !")

    # Enregistrement
    audio_data = sd.rec(
        int(duration * SAMPLE_RATE),
        samplerate=SAMPLE_RATE,
        channels=CHANNELS,
        dtype='int16'
    )
    sd.wait()  # Attendre la fin de l'enregistrement
    print("⏹  Terminé !")

    # Sauvegarder en WAV
    wav_write(output_path, SAMPLE_RATE, audio_data)
    return output_path

def collect_session(language: str, speaker_id: str, accent: str):
    """Session complète de collecte pour un locuteur"""

    phrases = PHRASES_FR if language == "fr" else PHRASES_EN
    output_dir = OUTPUT_DIR_FR if language == "fr" else OUTPUT_DIR_EN
    metadata = []

    print(f"\n{'='*50}")
    print(f"SESSION DE COLLECTE VOXIA")
    print(f"Langue   : {language.upper()}")
    print(f"Locuteur : {speaker_id}")
    print(f"Accent   : {accent}")
    print(f"Phrases  : {len(phrases)}")
    print(f"{'='*50}\n")

    os.makedirs(output_dir, exist_ok=True)

    for i, phrase in enumerate(phrases):
        print(f"\n[{i+1}/{len(phrases)}]")

        # Nom du fichier
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"{language}_{speaker_id}_{accent}_{i:03d}_{timestamp}.wav"
        filepath = os.path.join(output_dir, filename)

        input("Appuyez sur ENTRÉE quand vous êtes prêt...")

        try:
            record_phrase(phrase, filepath)

            # Métadonnées
            metadata.append({
                "file": filename,
                "phrase": phrase,
                "language": language,
                "speaker": speaker_id,
                "accent": accent,
                "duration": RECORD_SECONDS,
                "sample_rate": SAMPLE_RATE,
                "timestamp": timestamp
            })

            print(f"✅ Sauvegardé : {filename}")

        except KeyboardInterrupt:
            print(f"\n⚠️  Phrase {i+1} ignorée")
            continue

    # Sauvegarder métadonnées
    save_metadata(metadata)
    print(f"\n🎉 Session terminée ! {len(metadata)} phrases enregistrées.")
    return metadata

def save_metadata(new_entries):
    """Ajoute les nouvelles entrées aux métadonnées existantes"""
    existing = []

    if os.path.exists(METADATA_FILE):
        with open(METADATA_FILE, 'r', encoding='utf-8') as f:
            existing = json.load(f)

    existing.extend(new_entries)

    os.makedirs(os.path.dirname(METADATA_FILE), exist_ok=True)
    with open(METADATA_FILE, 'w', encoding='utf-8') as f:
        json.dump(existing, f, ensure_ascii=False, indent=2)

    print(f"📝 Métadonnées : {len(existing)} entrées totales")

def show_stats():
    """Affiche les statistiques de collecte"""
    if not os.path.exists(METADATA_FILE):
        print("Aucune donnée collectée.")
        return

    with open(METADATA_FILE, 'r', encoding='utf-8') as f:
        data = json.load(f)

    fr_count = sum(1 for d in data if d['language'] == 'fr')
    en_count = sum(1 for d in data if d['language'] == 'en')
    accents = set(d['accent'] for d in data)
    speakers = set(d['speaker'] for d in data)

    print(f"\n{'='*40}")
    print(f"STATISTIQUES COLLECTE VOXIA")
    print(f"{'='*40}")
    print(f"Total phrases : {len(data)}")
    print(f"Français      : {fr_count}")
    print(f"Anglais       : {en_count}")
    print(f"Locuteurs     : {len(speakers)}")
    print(f"Accents       : {', '.join(accents)}")
    print(f"{'='*40}\n")

def main():
    print("\n🎙️  VOXIA — Collecte Audio pour Fine-tuning Vosk")
    print("="*50)
    print("1. Nouvelle session FR")
    print("2. Nouvelle session EN")
    print("3. Voir statistiques")
    print("4. Quitter")

    choice = input("\nVotre choix : ").strip()

    if choice == "1":
        speaker_id = input("ID locuteur (ex: CM001) : ").strip()
        print("Accents disponibles : camerounais, ivoirien, senegalais, autre")
        accent = input("Accent : ").strip()
        collect_session("fr", speaker_id, accent)

    elif choice == "2":
        speaker_id = input("Speaker ID (ex: NG001) : ").strip()
        print("Accents: nigerian, ghanaian, cameroonian, other")
        accent = input("Accent: ").strip()
        collect_session("en", speaker_id, accent)

    elif choice == "3":
        show_stats()

    elif choice == "4":
        print("Au revoir !")
        return

    else:
        print("Choix invalide")

if __name__ == "__main__":
    main()