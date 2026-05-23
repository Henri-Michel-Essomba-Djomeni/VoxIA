import urllib.request
import zipfile
import os

print("Téléchargement SNIPS depuis GitHub...")

url = "https://github.com/sonos/nlu-benchmark/archive/refs/heads/master.zip"
zip_path = "data/nlp/datasets/snips_raw.zip"
extract_path = "data/nlp/datasets/snips_en"

# Télécharger
urllib.request.urlretrieve(url, zip_path)
print("Téléchargement terminé !")

# Extraire
os.makedirs(extract_path, exist_ok=True)
with zipfile.ZipFile(zip_path, 'r') as zip_ref:
    zip_ref.extractall(extract_path)
print("Extraction terminée !")

# Supprimer le zip
os.remove(zip_path)
print("SNIPS EN prêt dans data/nlp/datasets/snips_en")