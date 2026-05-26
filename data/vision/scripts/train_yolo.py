#!/usr/bin/env python3
"""
train_yolo.py - Entraînement + Fine-tuning YOLOv8n pour VOXIA
=============================================================

Pipeline complet :
  1. Préparation du dataset (COCO + OpenImages + Mini VOXIA Africa)
  2. Fine-tuning YOLOv8n.pt sur les 50 classes VOXIA
  3. Évaluation : mAP50, mAP50-95, PR curve
  4. Export TFLite INT8 pour déploiement Android
  5. Rapport de résultats

Usage :
  # Entraînement complet
  python train_yolo.py --mode train --epochs 100 --batch 16

  # Fine-tuning sur dataset africain uniquement
  python train_yolo.py --mode finetune --weights runs/train/best.pt --epochs 30

  # Évaluation du modèle exporté
  python train_yolo.py --mode evaluate --weights runs/train/best.pt

  # Export TFLite INT8
  python train_yolo.py --mode export --weights runs/train/best.pt

Prérequis :
  pip install ultralytics torch torchvision pandas matplotlib seaborn
"""

import argparse
import os
import sys
import json
import shutil
import time
from pathlib import Path
from datetime import datetime

import torch
import yaml
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from ultralytics import YOLO


# ============================================================
# CONFIGURATION GLOBALE
# ============================================================

# 50 classes VOXIA (30 COCO sélectionnées + 20 OpenImages)
VOXIA_CLASSES = [
    # COCO 30 classes (indices originaux conservés pour transfer learning)
    "personne", "velo", "voiture", "moto", "avion",
    "bus", "train", "camion", "bateau", "feux_circulation",
    "bouteille", "verre", "tasse", "fourchette", "couteau",
    "cuillere", "bol", "banane", "pomme", "sandwich",
    "chaise", "canape", "ecran", "ordinateur_portable", "souris",
    "telephone", "livre", "horloge", "sac", "valise",
    # OpenImages 20 classes (spécifiques Afrique)
    "billet_banque", "piece_monnaie", "cle", "lunettes", "chapeau",
    "panier", "jerrican", "seau", "machette", "lanterne",
    "sachet", "bougie", "pile", "stylo", "carnet",
    "masque", "gant", "ceinture", "portefeuille", "carte_sim"
]

# Hyperparamètres optimisés pour YOLOv8n sur mobile
HYPERPARAMS = {
    "imgsz": 416,          # résolution optimale RAM/précision sur mobile
    "batch": 16,           # batch size (réduire à 8 si OOM sur CPU)
    "epochs": 100,
    "lr0": 0.01,           # learning rate initial
    "lrf": 0.01,           # learning rate final (lr0 * lrf)
    "momentum": 0.937,
    "weight_decay": 0.0005,
    "warmup_epochs": 3,
    "conf": 0.45,          # seuil de confiance pour validation
    "iou": 0.45,           # seuil IoU NMS
    "augment": True,
    "hsv_h": 0.015,        # augmentation couleur
    "hsv_s": 0.7,
    "hsv_v": 0.4,
    "flipud": 0.0,         # pas de flip vertical (objets ont une orientation)
    "fliplr": 0.5,
    "mosaic": 1.0,
    "mixup": 0.0,
    "device": "cuda" if torch.cuda.is_available() else "cpu"
}


# ============================================================
# GÉNÉRATION DU FICHIER DE CONFIGURATION YAML
# ============================================================

def generate_dataset_yaml(data_dir: str, output_path: str = "voxia_dataset.yaml") -> str:
    """
    Génère le fichier YAML de configuration du dataset pour Ultralytics.
    
    Structure du dossier attendue :
      data_dir/
        images/
          train/  *.jpg
          val/    *.jpg
          test/   *.jpg (optionnel)
        labels/
          train/  *.txt (format YOLO)
          val/    *.txt
    """
    data_dir = Path(data_dir).resolve()
    
    config = {
        "path": str(data_dir),
        "train": "images/train",
        "val": "images/val",
        "test": "images/test",
        "nc": len(VOXIA_CLASSES),
        "names": {i: name for i, name in enumerate(VOXIA_CLASSES)}
    }
    
    with open(output_path, "w", encoding="utf-8") as f:
        yaml.dump(config, f, allow_unicode=True, default_flow_style=False)
    
    print(f"[CONFIG] Dataset YAML généré : {output_path}")
    print(f"[CONFIG] {len(VOXIA_CLASSES)} classes, data: {data_dir}")
    return output_path


# ============================================================
# ENTRAÎNEMENT (FINE-TUNING)
# ============================================================

def train(
    data_yaml: str,
    weights: str = "yolov8n.pt",
    output_dir: str = "runs/train",
    epochs: int = 100,
    batch: int = 16,
    resume: bool = False
) -> str:
    """
    Lance l'entraînement / fine-tuning YOLOv8n.
    
    Args:
        data_yaml: Chemin vers le fichier YAML du dataset
        weights: Poids de départ ('yolov8n.pt' pour transfer learning depuis COCO)
        output_dir: Dossier de sortie des résultats
        epochs: Nombre d'epochs d'entraînement
        batch: Taille du batch
        resume: Reprendre depuis le dernier checkpoint
    
    Returns:
        Chemin vers les meilleurs poids (best.pt)
    """
    print(f"\n{'='*60}")
    print(f"VOXIA - Entraînement YOLOv8n")
    print(f"{'='*60}")
    print(f"Poids de départ : {weights}")
    print(f"Dataset : {data_yaml}")
    print(f"Epochs : {epochs} | Batch : {batch}")
    print(f"Device : {HYPERPARAMS['device']}")
    print(f"{'='*60}\n")
    
    model = YOLO(weights)
    
    results = model.train(
        data=data_yaml,
        epochs=epochs,
        imgsz=HYPERPARAMS["imgsz"],
        batch=batch,
        lr0=HYPERPARAMS["lr0"],
        lrf=HYPERPARAMS["lrf"],
        momentum=HYPERPARAMS["momentum"],
        weight_decay=HYPERPARAMS["weight_decay"],
        warmup_epochs=HYPERPARAMS["warmup_epochs"],
        augment=HYPERPARAMS["augment"],
        hsv_h=HYPERPARAMS["hsv_h"],
        hsv_s=HYPERPARAMS["hsv_s"],
        hsv_v=HYPERPARAMS["hsv_v"],
        flipud=HYPERPARAMS["flipud"],
        fliplr=HYPERPARAMS["fliplr"],
        mosaic=HYPERPARAMS["mosaic"],
        device=HYPERPARAMS["device"],
        project=output_dir,
        name="voxia_yolov8n",
        exist_ok=True,
        resume=resume,
        verbose=True,
        # Métriques à logger
        plots=True,
        save=True,
        save_period=10,   # sauvegarder tous les 10 epochs
    )
    
    best_weights = Path(output_dir) / "voxia_yolov8n" / "weights" / "best.pt"
    print(f"\n[OK] Entraînement terminé. Meilleurs poids : {best_weights}")
    return str(best_weights)


def finetune_on_african_data(
    base_weights: str,
    african_data_yaml: str,
    output_dir: str = "runs/finetune",
    epochs: int = 30,
    batch: int = 8
) -> str:
    """
    Fine-tuning spécifique sur le Mini Dataset VOXIA Africa.
    
    Paramètres adaptés pour le fine-tuning :
    - Learning rate réduit (éviter l'oubli catastrophique)
    - Moins d'augmentation (données africaines spécifiques)
    - Batch plus petit (dataset réduit)
    """
    print(f"\n[FINETUNE] Démarrage fine-tuning sur données africaines")
    print(f"[FINETUNE] Base : {base_weights} | Data : {african_data_yaml}")
    
    model = YOLO(base_weights)
    
    results = model.train(
        data=african_data_yaml,
        epochs=epochs,
        imgsz=HYPERPARAMS["imgsz"],
        batch=batch,
        lr0=0.001,          # LR réduit pour fine-tuning
        lrf=0.001,
        momentum=HYPERPARAMS["momentum"],
        weight_decay=HYPERPARAMS["weight_decay"],
        warmup_epochs=1,
        augment=True,
        mosaic=0.5,         # Moins de mosaic sur petit dataset
        device=HYPERPARAMS["device"],
        project=output_dir,
        name="voxia_africa_finetune",
        exist_ok=True,
        freeze=10,          # Geler les 10 premières couches (backbone)
        plots=True,
        verbose=True,
    )
    
    best_weights = Path(output_dir) / "voxia_africa_finetune" / "weights" / "best.pt"
    print(f"[FINETUNE] Terminé. Meilleurs poids : {best_weights}")
    return str(best_weights)


# ============================================================
# ÉVALUATION
# ============================================================

def evaluate(
    weights: str,
    data_yaml: str,
    output_dir: str = "runs/evaluate",
    conf_threshold: float = 0.45
) -> dict:
    """
    Évalue le modèle sur le jeu de validation et génère un rapport complet.
    
    Métriques calculées :
    - mAP50 (Mean Average Precision @ IoU=0.50) → cible > 75%
    - mAP50-95 (mAP sur IoU 0.50 à 0.95)
    - Précision, Rappel, F1 par classe
    - Temps d'inférence moyen
    
    Args:
        weights: Chemin vers les poids du modèle
        data_yaml: Dataset de validation
        output_dir: Dossier de sortie du rapport
        conf_threshold: Seuil de confiance pour la détection
    
    Returns:
        Dictionnaire des métriques
    """
    print(f"\n{'='*60}")
    print(f"VOXIA - Évaluation YOLOv8n")
    print(f"{'='*60}")
    
    model = YOLO(weights)
    
    results = model.val(
        data=data_yaml,
        imgsz=HYPERPARAMS["imgsz"],
        conf=conf_threshold,
        iou=HYPERPARAMS["iou"],
        device=HYPERPARAMS["device"],
        project=output_dir,
        name="voxia_eval",
        exist_ok=True,
        plots=True,
        save_json=True,
        verbose=True,
    )
    
    # Extraire les métriques clés
    metrics = {
        "mAP50": float(results.box.map50),
        "mAP50_95": float(results.box.map),
        "precision": float(results.box.mp),
        "recall": float(results.box.mr),
        "f1": 2 * float(results.box.mp) * float(results.box.mr) / 
              max(float(results.box.mp) + float(results.box.mr), 1e-6),
    }
    
    # Métriques par classe
    per_class_metrics = {}
    if hasattr(results.box, 'ap_class_index'):
        for i, class_idx in enumerate(results.box.ap_class_index):
            class_name = VOXIA_CLASSES[class_idx] if class_idx < len(VOXIA_CLASSES) else f"class_{class_idx}"
            per_class_metrics[class_name] = {
                "AP50": float(results.box.ap50[i]) if i < len(results.box.ap50) else 0.0
            }
    
    metrics["per_class"] = per_class_metrics
    
    # Affichage résumé
    print(f"\n{'='*40}")
    print(f"RÉSULTATS D'ÉVALUATION VOXIA")
    print(f"{'='*40}")
    print(f"mAP50        : {metrics['mAP50']:.4f} {'✅' if metrics['mAP50'] > 0.75 else '⚠️ (cible > 0.75)'}")
    print(f"mAP50-95     : {metrics['mAP50_95']:.4f}")
    print(f"Précision    : {metrics['precision']:.4f}")
    print(f"Rappel       : {metrics['recall']:.4f}")
    print(f"F1-Score     : {metrics['f1']:.4f}")
    
    if per_class_metrics:
        print(f"\nTop 10 classes (AP50):")
        sorted_classes = sorted(per_class_metrics.items(), key=lambda x: x[1]["AP50"], reverse=True)
        for name, m in sorted_classes[:10]:
            bar = "█" * int(m["AP50"] * 20)
            print(f"  {name:<20} : {m['AP50']:.3f} {bar}")
    
    # Sauvegarder le rapport JSON
    report_path = Path(output_dir) / "voxia_eval" / "metrics_report.json"
    report_path.parent.mkdir(parents=True, exist_ok=True)
    with open(report_path, "w") as f:
        json.dump(metrics, f, indent=2)
    print(f"\n[RAPPORT] Sauvegardé : {report_path}")
    
    # Générer les graphiques
    generate_evaluation_plots(metrics, per_class_metrics, output_dir)
    
    return metrics


def benchmark_inference(weights: str, num_runs: int = 100) -> dict:
    """
    Benchmark du temps d'inférence sur CPU (simule un Redmi 9A).
    
    Args:
        weights: Chemin vers les poids
        num_runs: Nombre de passages pour la moyenne
    
    Returns:
        Statistiques de latence
    """
    print(f"\n[BENCHMARK] Test d'inférence sur CPU ({num_runs} runs)")
    
    model = YOLO(weights)
    dummy_input = np.random.randint(0, 255, (HYPERPARAMS["imgsz"], HYPERPARAMS["imgsz"], 3), dtype=np.uint8)
    
    # Warm-up
    for _ in range(5):
        model.predict(dummy_input, verbose=False)
    
    # Benchmark
    times = []
    for i in range(num_runs):
        start = time.perf_counter()
        model.predict(dummy_input, conf=HYPERPARAMS["conf"], verbose=False)
        end = time.perf_counter()
        times.append((end - start) * 1000)  # en ms
    
    stats = {
        "mean_ms": np.mean(times),
        "std_ms": np.std(times),
        "min_ms": np.min(times),
        "max_ms": np.max(times),
        "p95_ms": np.percentile(times, 95),
        "fps_estimate": 1000 / np.mean(times)
    }
    
    print(f"  Latence moyenne : {stats['mean_ms']:.1f} ms ± {stats['std_ms']:.1f}")
    print(f"  P95             : {stats['p95_ms']:.1f} ms")
    print(f"  FPS estimé      : {stats['fps_estimate']:.1f}")
    target = "✅" if stats["mean_ms"] < 500 else "⚠️"
    print(f"  Cible < 500ms   : {target}")
    
    return stats


# ============================================================
# EXPORT TFLite INT8
# ============================================================

def export_tflite_int8(
    weights: str,
    calibration_data_dir: str = None,
    output_name: str = "yolov8n_int8.tflite",
    assets_dir: str = "../../app/src/main/assets"
) -> str:
    """
    Exporte le modèle YOLOv8n entraîné en TFLite INT8 pour Android.
    
    La quantisation INT8 réduit la taille de ~75% et accélère l'inférence
    sur les processeurs mobiles sans GPU dédié (Helio G85, Snapdragon 460...).
    
    Si calibration_data_dir est fourni, utilise la quantisation post-entraînement
    avec données de calibration pour de meilleures performances.
    
    Args:
        weights: Chemin vers best.pt
        calibration_data_dir: Dossier d'images pour calibration INT8 (optionnel)
        output_name: Nom du fichier .tflite de sortie
        assets_dir: Dossier assets Android pour copie automatique
    
    Returns:
        Chemin vers le fichier .tflite généré
    """
    print(f"\n{'='*60}")
    print(f"VOXIA - Export TFLite INT8")
    print(f"{'='*60}")
    print(f"Poids source : {weights}")
    
    model = YOLO(weights)
    
    # Export via Ultralytics (gère la conversion PyTorch → ONNX → TFLite)
    export_path = model.export(
        format="tflite",
        imgsz=HYPERPARAMS["imgsz"],
        int8=True,
        data=calibration_data_dir,  # données de calibration pour INT8
        simplify=True,              # simplification ONNX
        opset=17,
        nms=False,                  # NMS géré côté Android
    )
    
    if export_path:
        # Renommer et copier dans les assets Android
        export_file = Path(str(export_path))
        output_path = Path(output_name)
        
        if export_file.exists():
            shutil.copy(export_file, output_path)
            size_mb = output_path.stat().st_size / (1024 * 1024)
            print(f"\n[OK] TFLite INT8 exporté : {output_path} ({size_mb:.1f} Mo)")
            
            # Copier dans les assets Android
            assets_path = Path(assets_dir)
            if assets_path.exists():
                dest = assets_path / output_name
                shutil.copy(output_path, dest)
                print(f"[OK] Copié dans les assets Android : {dest}")
            else:
                print(f"[INFO] Dossier assets non trouvé : {assets_dir}")
                print(f"       Copier manuellement : cp {output_path} <android_project>/app/src/main/assets/")
            
            # Validation du modèle exporté
            validate_tflite(str(output_path))
            
            return str(output_path)
    
    print("[ERREUR] Échec de l'export TFLite")
    return ""


def validate_tflite(tflite_path: str):
    """
    Valide le modèle TFLite exporté : tenseurs, formes, inférence test.
    """
    try:
        import tensorflow as tf
        
        interpreter = tf.lite.Interpreter(model_path=tflite_path)
        interpreter.allocate_tensors()
        
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        print(f"\n[VALIDATION TFLite]")
        print(f"  Entrée : shape={input_details[0]['shape']}, dtype={input_details[0]['dtype'].__name__}")
        print(f"  Sortie : shape={output_details[0]['shape']}, dtype={output_details[0]['dtype'].__name__}")
        
        # Test d'inférence avec une image aléatoire
        input_shape = input_details[0]['shape']
        if input_details[0]['dtype'] == np.int8:
            dummy = np.random.randint(-128, 127, input_shape, dtype=np.int8)
        else:
            dummy = np.random.rand(*input_shape).astype(np.float32)
        
        interpreter.set_tensor(input_details[0]['index'], dummy)
        start = time.perf_counter()
        interpreter.invoke()
        elapsed = (time.perf_counter() - start) * 1000
        
        output = interpreter.get_tensor(output_details[0]['index'])
        print(f"  Inférence test : {elapsed:.1f} ms → output shape: {output.shape}")
        print(f"  [OK] Modèle TFLite valide !")
        
    except ImportError:
        print("[INFO] TensorFlow non installé. Validation TFLite ignorée.")
        print("       pip install tensorflow pour valider.")
    except Exception as e:
        print(f"[ERREUR] Validation TFLite: {e}")


# ============================================================
# GRAPHIQUES ET VISUALISATION
# ============================================================

def generate_evaluation_plots(
    metrics: dict,
    per_class_metrics: dict,
    output_dir: str
):
    """Génère les graphiques d'évaluation : AP par classe, distribution des scores."""
    
    if not per_class_metrics:
        return
    
    fig, axes = plt.subplots(1, 2, figsize=(18, 8))
    fig.suptitle("VOXIA YOLOv8n - Résultats d'Évaluation", fontsize=14, fontweight="bold")
    
    # Graphique 1 : AP50 par classe
    classes = list(per_class_metrics.keys())
    ap50_values = [per_class_metrics[c]["AP50"] for c in classes]
    sorted_pairs = sorted(zip(classes, ap50_values), key=lambda x: x[1], reverse=True)
    classes_sorted, ap50_sorted = zip(*sorted_pairs) if sorted_pairs else ([], [])
    
    colors = ["#2ecc71" if v > 0.75 else "#e74c3c" if v < 0.5 else "#f39c12"
              for v in ap50_sorted]
    
    axes[0].barh(range(len(classes_sorted)), ap50_sorted, color=colors)
    axes[0].set_yticks(range(len(classes_sorted)))
    axes[0].set_yticklabels(classes_sorted, fontsize=7)
    axes[0].set_xlabel("AP50")
    axes[0].set_title("AP50 par classe")
    axes[0].axvline(x=0.75, color="green", linestyle="--", alpha=0.7, label="Cible 75%")
    axes[0].legend()
    axes[0].set_xlim(0, 1)
    
    # Graphique 2 : Résumé métriques globales
    global_metrics = {
        "mAP50": metrics.get("mAP50", 0),
        "mAP50-95": metrics.get("mAP50_95", 0),
        "Précision": metrics.get("precision", 0),
        "Rappel": metrics.get("recall", 0),
        "F1-Score": metrics.get("f1", 0)
    }
    
    metric_names = list(global_metrics.keys())
    metric_values = list(global_metrics.values())
    
    bars = axes[1].bar(metric_names, metric_values,
                       color=["#3498db", "#9b59b6", "#e67e22", "#1abc9c", "#e74c3c"])
    axes[1].set_ylim(0, 1.1)
    axes[1].set_title("Métriques globales")
    axes[1].set_ylabel("Score")
    axes[1].axhline(y=0.75, color="green", linestyle="--", alpha=0.7, label="Cible 75%")
    axes[1].legend()
    
    for bar, val in zip(bars, metric_values):
        axes[1].text(bar.get_x() + bar.get_width()/2., bar.get_height() + 0.01,
                     f"{val:.3f}", ha="center", va="bottom", fontsize=10, fontweight="bold")
    
    plt.tight_layout()
    
    plot_path = Path(output_dir) / "voxia_eval" / "evaluation_summary.png"
    plot_path.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(plot_path, dpi=150, bbox_inches="tight")
    print(f"[GRAPHIQUE] Sauvegardé : {plot_path}")
    plt.close()


# ============================================================
# PRÉPARATION DES DONNÉES (utilitaire)
# ============================================================

def prepare_dataset_structure(base_dir: str = "data/vision/datasets/yolo_ready"):
    """
    Crée la structure de dossiers attendue par Ultralytics YOLOv8.
    
    Structure générée :
      yolo_ready/
        images/
          train/  ← images d'entraînement (*.jpg)
          val/    ← images de validation (*.jpg)
          test/   ← images de test (*.jpg)
        labels/
          train/  ← annotations YOLO (*.txt)
          val/
          test/
    """
    base = Path(base_dir)
    for split in ["train", "val", "test"]:
        (base / "images" / split).mkdir(parents=True, exist_ok=True)
        (base / "labels" / split).mkdir(parents=True, exist_ok=True)
    
    # Générer un fichier classes.txt
    classes_path = base / "classes.txt"
    with open(classes_path, "w", encoding="utf-8") as f:
        f.write("\n".join(VOXIA_CLASSES))
    
    print(f"[OK] Structure dataset créée dans : {base}")
    print(f"[OK] {len(VOXIA_CLASSES)} classes dans : {classes_path}")
    
    # Générer le YAML
    yaml_path = str(base / "voxia_dataset.yaml")
    generate_dataset_yaml(str(base), yaml_path)
    
    return yaml_path


# ============================================================
# POINT D'ENTRÉE
# ============================================================

def parse_args():
    parser = argparse.ArgumentParser(
        description="VOXIA - Entraînement et export YOLOv8n"
    )
    parser.add_argument("--mode", choices=["train", "finetune", "evaluate", "export", "benchmark", "prepare"],
                        default="train", help="Mode d'exécution")
    parser.add_argument("--weights", default="yolov8n.pt",
                        help="Poids de départ ou chemin vers best.pt")
    parser.add_argument("--data", default="data/vision/datasets/yolo_ready/voxia_dataset.yaml",
                        help="Chemin vers le fichier YAML du dataset")
    parser.add_argument("--african-data", default="data/vision/datasets/mini_voxia_africa/voxia_africa.yaml",
                        help="Dataset africain pour fine-tuning")
    parser.add_argument("--epochs", type=int, default=100)
    parser.add_argument("--batch", type=int, default=16)
    parser.add_argument("--output", default="runs", help="Dossier de sortie")
    parser.add_argument("--conf", type=float, default=0.45)
    parser.add_argument("--calibration-data", default=None,
                        help="Dossier d'images pour calibration INT8")
    parser.add_argument("--assets-dir", default="../../app/src/main/assets",
                        help="Dossier assets Android")
    return parser.parse_args()


def main():
    args = parse_args()
    
    print(f"\n{'='*60}")
    print(f"  VOXIA AI - Module Vision - Entraînement YOLOv8n")
    print(f"  {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"  Mode : {args.mode.upper()}")
    print(f"{'='*60}\n")
    
    if args.mode == "prepare":
        yaml_path = prepare_dataset_structure()
        print(f"\n[PRÊT] Copiez vos images dans les dossiers images/train et images/val")
        print(f"       Copiez les annotations YOLO dans labels/train et labels/val")
        print(f"       Format annotation : <class_id> <cx> <cy> <w> <h> (normalisés 0-1)")
    
    elif args.mode == "train":
        best_weights = train(
            data_yaml=args.data,
            weights=args.weights,
            output_dir=f"{args.output}/train",
            epochs=args.epochs,
            batch=args.batch
        )
        print(f"\n[SUIVANT] Pour évaluer : python train_yolo.py --mode evaluate --weights {best_weights}")
        print(f"[SUIVANT] Pour exporter : python train_yolo.py --mode export --weights {best_weights}")
    
    elif args.mode == "finetune":
        best_weights = finetune_on_african_data(
            base_weights=args.weights,
            african_data_yaml=args.african_data,
            output_dir=f"{args.output}/finetune",
            epochs=args.epochs,
            batch=args.batch
        )
        print(f"\n[SUIVANT] Évaluer avec : python train_yolo.py --mode evaluate --weights {best_weights}")
    
    elif args.mode == "evaluate":
        metrics = evaluate(
            weights=args.weights,
            data_yaml=args.data,
            output_dir=f"{args.output}/evaluate",
            conf_threshold=args.conf
        )
        # Vérification cible mAP50 > 75%
        if metrics["mAP50"] >= 0.75:
            print(f"\n✅ CIBLE ATTEINTE : mAP50={metrics['mAP50']:.4f} > 0.75")
            print(f"   Le modèle est prêt pour l'export TFLite.")
        else:
            print(f"\n⚠️ CIBLE NON ATTEINTE : mAP50={metrics['mAP50']:.4f} < 0.75")
            print(f"   Recommandations :")
            print(f"   - Augmenter les epochs (actuellement {args.epochs})")
            print(f"   - Vérifier la qualité des annotations")
            print(f"   - Ajouter plus de données africaines")
    
    elif args.mode == "export":
        tflite_path = export_tflite_int8(
            weights=args.weights,
            calibration_data_dir=args.calibration_data,
            output_name="yolov8n_int8.tflite",
            assets_dir=args.assets_dir
        )
        if tflite_path:
            print(f"\n✅ Export réussi : {tflite_path}")
            print(f"   Commande adb pour copier sur l'appareil :")
            print(f"   adb push {tflite_path} /sdcard/Download/")
    
    elif args.mode == "benchmark":
        stats = benchmark_inference(args.weights, num_runs=50)
        print(f"\n[BENCHMARK COMPLET]")
        for k, v in stats.items():
            print(f"  {k}: {v:.2f}")


if __name__ == "__main__":
    main()
