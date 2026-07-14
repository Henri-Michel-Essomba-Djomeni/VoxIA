import json
import os
from pathlib import Path

# Chemins
annotations_file = r"C:\Users\User\OneDrive\Desktop\VoxIA\data\vision\datasets\annotations\instances_val2017.json"
output_dir = r"C:\Users\User\OneDrive\Desktop\VoxIA\data\vision\datasets\yolo_ready\labels\val"

Path(output_dir).mkdir(parents=True, exist_ok=True)

print("Chargement des annotations...")
with open(annotations_file, "r") as f:
    coco = json.load(f)

# Index images
images = {img["id"]: img for img in coco["images"]}

# Index catégories → id YOLO (0 à 79)
categories = sorted(coco["categories"], key=lambda x: x["id"])
cat_to_yolo = {cat["id"]: i for i, cat in enumerate(categories)}

print(f"{len(coco['annotations'])} annotations trouvées")
count = 0

for ann in coco["annotations"]:
    if "bbox" not in ann:
        continue
    
    img = images[ann["image_id"]]
    w, h = img["width"], img["height"]
    
    x, y, bw, bh = ann["bbox"]
    cx = (x + bw / 2) / w
    cy = (y + bh / 2) / h
    bw /= w
    bh /= h
    
    class_id = cat_to_yolo[ann["category_id"]]
    
    filename = Path(img["file_name"]).stem + ".txt"
    with open(os.path.join(output_dir, filename), "a") as f:
        f.write(f"{class_id} {cx:.6f} {cy:.6f} {bw:.6f} {bh:.6f}\n")
    count += 1

print(f"[OK] {count} annotations converties dans {output_dir}")
import json
import os
from pathlib import Path

# Chemins
annotations_file = r"C:\Users\User\OneDrive\Desktop\VoxIA\data\vision\datasets\annotations\instances_val2017.json"
output_dir = r"C:\Users\User\OneDrive\Desktop\VoxIA\data\vision\datasets\yolo_ready\labels\val"

Path(output_dir).mkdir(parents=True, exist_ok=True)

print("Chargement des annotations...")
with open(annotations_file, "r") as f:
    coco = json.load(f)

# Index images
images = {img["id"]: img for img in coco["images"]}

# Index catégories → id YOLO (0 à 79)
categories = sorted(coco["categories"], key=lambda x: x["id"])
cat_to_yolo = {cat["id"]: i for i, cat in enumerate(categories)}

print(f"{len(coco['annotations'])} annotations trouvées")
count = 0

for ann in coco["annotations"]:
    if "bbox" not in ann:
        continue
    
    img = images[ann["image_id"]]
    w, h = img["width"], img["height"]
    
    x, y, bw, bh = ann["bbox"]
    cx = (x + bw / 2) / w
    cy = (y + bh / 2) / h
    bw /= w
    bh /= h
    
    class_id = cat_to_yolo[ann["category_id"]]
    
    filename = Path(img["file_name"]).stem + ".txt"
    with open(os.path.join(output_dir, filename), "a") as f:
        f.write(f"{class_id} {cx:.6f} {cy:.6f} {bw:.6f} {bh:.6f}\n")
    count += 1

print(f"[OK] {count} annotations converties dans {output_dir}")