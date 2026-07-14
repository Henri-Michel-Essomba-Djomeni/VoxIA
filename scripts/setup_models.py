#!/usr/bin/env python3
"""
setup_models.py - Script complet de configuration des modèles VOXIA
==============================================================================

Usage:
    python scripts/setup_models.py              # Tout installer
    python scripts/setup_models.py --yolo-only  # YOLO uniquement
    python scripts/setup_models.py --intent-only # Intent classifier uniquement

Prérequis:
    pip install ultralytics tensorflow numpy tflite
"""

import argparse
import os
import sys
import urllib.request
import zipfile
import shutil
import json

ASSETS_DIR = os.path.join("app", "src", "main", "assets")
os.makedirs(ASSETS_DIR, exist_ok=True)

YOLO_URLS = [
    "https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n_int8.tflite",
    "https://github.com/ultralytics/ultralytics/releases/download/v8.2.96/yolov8n_int8.tflite",
]


def download_file(url, dest, timeout=120):
    """Télécharge un fichier avec gestion d'erreur."""
    print(f"⬇️  Téléchargement: {os.path.basename(dest)}...")
    try:
        urllib.request.urlretrieve(url, dest)
        size_mb = os.path.getsize(dest) / (1024 * 1024)
        print(f"   ✅ {size_mb:.1f} Mo")
        return True
    except Exception as e:
        print(f"   ❌ Échec: {e}")
        return False


def download_yolo():
    """Télécharge YOLOv8n TFLite INT8 pré-entraîné."""
    dest = os.path.join(ASSETS_DIR, "yolov8n_int8.tflite")

    if os.path.exists(dest):
        size_mb = os.path.getsize(dest) / (1024 * 1024)
        print(f"✅ YOLOv8n déjà présent ({size_mb:.1f} Mo)")
        return True

    for url in YOLO_URLS:
        if download_file(url, dest):
            return True

    print("\n⚠️  Impossible de télécharger YOLOv8n automatiquement.")
    print("   Téléchargez-le manuellement depuis:")
    print("   https://github.com/ultralytics/assets/releases")
    print(f"   Placez le fichier dans: {ASSETS_DIR}/")
    return False


def generate_intent_classifier():
    """Génère le classifieur d'intents TFLite."""
    script = os.path.join("data", "nlp", "scripts", "train_classifier.py")
    if os.path.exists(script):
        print("\n🧠 Génération du Intent Classifier...")
        ret = os.system(f"{sys.executable} {script}")
        if ret == 0:
            print("✅ Intent Classifier généré")
            return True
    print("⚠️  Script train_classifier.py non trouvé")
    return False


def check_models():
    """Vérifie quels modèles sont présents."""
    models = {
        "yolov8n_int8.tflite": "YOLOv8n Vision",
        "intent_classifier.tflite": "Intent Classifier NLP",
        "vosk-model-small-fr": "Vosk STT Français",
        "vosk-model-small-en": "Vosk STT Anglais",
    }

    print("\n" + "=" * 50)
    print("VÉRIFICATION DES MODÈLES VOXIA")
    print("=" * 50)

    all_ok = True
    for filename, label in models.items():
        path = os.path.join(ASSETS_DIR, filename)
        if os.path.exists(path):
            size = "dossier" if os.path.isdir(path) else f"{os.path.getsize(path) / (1024 * 1024):.1f} Mo"
            print(f"  ✅ {label:<25} {size:>10}")
        else:
            print(f"  ❌ {label:<25} {'MANQUANT':>10}")
            all_ok = False

    print("=" * 50)
    if all_ok:
        print("✅ Tous les modèles sont présents !")
    else:
        print("⚠️  Certains modèles sont manquants.")
    print("=" * 50 + "\n")

    return all_ok


def main():
    parser = argparse.ArgumentParser(description="Setup des modèles VOXIA")
    parser.add_argument("--yolo-only", action="store_true")
    parser.add_argument("--intent-only", action="store_true")
    parser.add_argument("--check", action="store_true", help="Vérifier seulement")
    args = parser.parse_args()

    if args.check:
        check_models()
        return

    if args.intent_only:
        generate_intent_classifier()
    elif args.yolo_only:
        download_yolo()
    else:
        print("\n" + "=" * 60)
        print("  SETUP COMPLET DES MODÈLES VOXIA")
        print("=" * 60)

        check_models()

        print("\n📦 Téléchargement YOLOv8n...")
        download_yolo()

        print("\n🧠 Génération Intent Classifier...")
        generate_intent_classifier()

        print("\n📋 Vérification finale...")
        check_models()

        print("\n🎉 Configuration terminée !")
        print("   Les modèles sont dans:", ASSETS_DIR)


if __name__ == "__main__":
    main()
