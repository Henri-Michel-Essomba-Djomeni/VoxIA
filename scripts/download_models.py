#!/usr/bin/env python3
"""
VoxIA - Script de téléchargement des modèles Vosk
Usage: python scripts/download_models.py
"""

import os
import zipfile
import urllib.request
import shutil

# Modèles à télécharger
MODELS = {
    "vosk-model-small-fr": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip",
        "zip": "vosk-model-small-fr-0.22.zip",
        "extracted": "vosk-model-small-fr-0.22"
    },
    "vosk-model-small-en": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
        "zip": "vosk-model-small-en-us-0.15.zip",
        "extracted": "vosk-model-small-en-us-0.15"
    }
}

ASSETS_DIR = "app/src/main/assets"

def download_model(name, info):
    target_dir = os.path.join(ASSETS_DIR, name)

    # Déjà téléchargé ?
    if os.path.exists(target_dir):
        print(f"✅ {name} déjà présent — skip")
        return

    print(f"⬇️  Téléchargement {name}...")
    urllib.request.urlretrieve(info["url"], info["zip"])
    print(f"📦 Extraction {name}...")

    with zipfile.ZipFile(info["zip"], 'r') as z:
        z.extractall(".")

    # Renommer le dossier extrait
    shutil.move(info["extracted"], target_dir)
    os.remove(info["zip"])
    print(f"✅ {name} installé dans {target_dir}")

def main():
    os.makedirs(ASSETS_DIR, exist_ok=True)
    for name, info in MODELS.items():
        download_model(name, info)
    print("\n🎉 Tous les modèles sont prêts !")

if __name__ == "__main__":
    main()