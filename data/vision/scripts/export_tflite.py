#!/usr/bin/env python3
"""
export_tflite.py - Export et validation TFLite INT8 pour VOXIA Android
=======================================================================

Script dédié à l'export du modèle YOLOv8n.pt vers le format TFLite INT8
déployable sur Android (Kotlin + TFLite Interpreter).

Usage :
  # Export standard depuis best.pt
  python export_tflite.py --weights runs/train/best.pt

  # Export avec calibration INT8 (meilleure précision)
  python export_tflite.py --weights runs/train/best.pt \
      --calibration-data data/vision/datasets/val/images

  # Validation seule d'un modèle déjà exporté
  python export_tflite.py --validate-only --tflite yolov8n_int8.tflite

  # Benchmark complet
  python export_tflite.py --weights runs/train/best.pt --benchmark

Sortie :
  yolov8n_int8.tflite  → copié dans app/src/main/assets/
"""

import argparse
import sys
import time
import shutil
import json
from pathlib import Path
import numpy as np

try:
    from ultralytics import YOLO
    HAS_ULTRALYTICS = True
except ImportError:
    HAS_ULTRALYTICS = False
    print("[WARN] ultralytics non installé. pip install ultralytics")

try:
    import tensorflow as tf
    HAS_TF = True
except ImportError:
    HAS_TF = False
    print("[WARN] tensorflow non installé. pip install tensorflow")


IMGSZ = 416
OUTPUT_NAME = "yolov8n_int8.tflite"
ASSETS_DIR = "../../app/src/main/assets"

# Cibles de validation
VALIDATION_TARGETS = {
    "max_size_mb": 10.0,
    "input_shape": [1, IMGSZ, IMGSZ, 3],
    "output_channels": 54,   # 4 + 50 classes
    "max_inference_ms": 500,
}


def export_model(
    weights: str,
    calibration_data: str = None,
    output_name: str = OUTPUT_NAME,
    assets_dir: str = ASSETS_DIR
) -> str:
    """
    Exporte best.pt → TFLite INT8 via Ultralytics.
    
    Étapes internes Ultralytics :
      1. PyTorch .pt → ONNX (via torch.onnx.export)
      2. ONNX → TFLite (via onnx2tf ou tf.lite.TFLiteConverter)
      3. Quantisation INT8 (post-training quantization)
      4. Simplification du graphe
    """
    if not HAS_ULTRALYTICS:
        print("[ERREUR] ultralytics requis pour l'export")
        return ""
    
    print(f"\n{'='*55}")
    print(f"  VOXIA - Export TFLite INT8")
    print(f"{'='*55}")
    print(f"  Poids source   : {weights}")
    print(f"  Résolution     : {IMGSZ}×{IMGSZ}")
    print(f"  Quantisation   : INT8")
    print(f"  Calibration    : {calibration_data or 'auto (aléatoire)'}")
    print(f"{'='*55}\n")
    
    if not Path(weights).exists():
        print(f"[ERREUR] Fichier introuvable : {weights}")
        return ""
    
    model = YOLO(weights)
    
    print("[EXPORT] Démarrage de la conversion...")
    
    # Export TFLite INT8
    export_result = model.export(
        format="tflite",
        imgsz=IMGSZ,
        int8=True,
        data=calibration_data,
        simplify=True,
        opset=17,
        nms=False,        # NMS géré dans VisionModule.kt (plus flexible)
        verbose=True,
    )
    
    if not export_result:
        print("[ERREUR] Export échoué")
        return ""
    
    # Localiser le fichier exporté
    exported_file = Path(str(export_result))
    if not exported_file.exists():
        # Ultralytics peut placer le fichier différemment selon la version
        possible_paths = [
            Path(weights).parent / f"{Path(weights).stem}_int8.tflite",
            Path(weights).parent / f"{Path(weights).stem}.tflite",
            Path("yolov8n_int8.tflite"),
        ]
        exported_file = next((p for p in possible_paths if p.exists()), None)
        if not exported_file:
            print("[ERREUR] Fichier TFLite non trouvé après export")
            return ""
    
    # Copier avec le nom standard VOXIA
    output_path = Path(output_name)
    shutil.copy(exported_file, output_path)
    
    size_mb = output_path.stat().st_size / (1024 * 1024)
    print(f"\n[OK] TFLite INT8 généré : {output_path} ({size_mb:.2f} Mo)")
    
    # Copier dans les assets Android si le dossier existe
    assets_path = Path(assets_dir)
    if assets_path.exists():
        dest = assets_path / output_name
        shutil.copy(output_path, dest)
        print(f"[OK] Copié dans assets Android : {dest}")
    else:
        print(f"\n[INFO] Assets Android non trouvés ({assets_dir})")
        print(f"       Commande de copie manuelle :")
        print(f"       cp {output_path} <projet>/app/src/main/assets/\n")
    
    return str(output_path)


def validate_tflite_model(tflite_path: str) -> dict:
    """
    Validation complète du modèle TFLite exporté.
    
    Vérifications :
    ✅ Taille du fichier < 10 Mo
    ✅ Forme du tenseur d'entrée : [1, 416, 416, 3]
    ✅ Quantisation INT8
    ✅ Forme du tenseur de sortie : [1, 54, 3549]
    ✅ Inférence test sans erreur
    ✅ Latence mesurée
    
    Returns:
        Dictionnaire des résultats de validation
    """
    if not HAS_TF:
        print("[ERREUR] TensorFlow requis pour la validation")
        return {}
    
    print(f"\n{'='*55}")
    print(f"  VOXIA - Validation TFLite")
    print(f"  Modèle : {tflite_path}")
    print(f"{'='*55}\n")
    
    results = {"path": tflite_path, "checks": {}, "passed": True}
    
    if not Path(tflite_path).exists():
        print(f"[ERREUR] Fichier non trouvé : {tflite_path}")
        return {"passed": False, "error": "Fichier non trouvé"}
    
    # ── Vérification 1 : Taille ──────────────────────────────────
    size_mb = Path(tflite_path).stat().st_size / (1024 * 1024)
    check_size = size_mb <= VALIDATION_TARGETS["max_size_mb"]
    results["checks"]["size_mb"] = {
        "value": round(size_mb, 2),
        "target": f"<= {VALIDATION_TARGETS['max_size_mb']} Mo",
        "passed": check_size
    }
    icon = "✅" if check_size else "❌"
    print(f"{icon} Taille         : {size_mb:.2f} Mo (cible <= {VALIDATION_TARGETS['max_size_mb']} Mo)")
    
    # ── Chargement de l'interpréteur ────────────────────────────
    try:
        interpreter = tf.lite.Interpreter(model_path=tflite_path)
        interpreter.allocate_tensors()
    except Exception as e:
        print(f"❌ Chargement TFLite échoué : {e}")
        results["passed"] = False
        return results
    
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    # ── Vérification 2 : Forme d'entrée ─────────────────────────
    input_shape = list(input_details[0]["shape"])
    target_shape = VALIDATION_TARGETS["input_shape"]
    check_input = input_shape == target_shape
    results["checks"]["input_shape"] = {
        "value": input_shape,
        "target": target_shape,
        "passed": check_input
    }
    icon = "✅" if check_input else "❌"
    print(f"{icon} Input shape    : {input_shape} (cible {target_shape})")
    
    # ── Vérification 3 : Quantisation INT8 ──────────────────────
    dtype = input_details[0]["dtype"]
    is_int8 = dtype == np.int8
    results["checks"]["quantization"] = {
        "value": str(dtype),
        "target": "int8",
        "passed": is_int8
    }
    icon = "✅" if is_int8 else "⚠️"
    print(f"{icon} Quantisation   : {dtype} ({'INT8 ✓' if is_int8 else 'pas INT8 - vérifier export'})")
    
    # ── Vérification 4 : Forme de sortie ────────────────────────
    output_shape = list(output_details[0]["shape"])
    check_output = (
        len(output_shape) >= 2 and
        output_shape[0] == 1 and
        (output_shape[1] == VALIDATION_TARGETS["output_channels"] or
         len(output_shape) > 2 and output_shape[1] == VALIDATION_TARGETS["output_channels"])
    )
    results["checks"]["output_shape"] = {
        "value": output_shape,
        "target": f"[1, {VALIDATION_TARGETS['output_channels']}, ...]",
        "passed": check_output
    }
    icon = "✅" if check_output else "⚠️"
    print(f"{icon} Output shape   : {output_shape}")
    
    # ── Vérification 5 : Inférence ──────────────────────────────
    try:
        if is_int8:
            dummy = np.random.randint(-128, 127, input_shape, dtype=np.int8)
        else:
            dummy = np.random.rand(*input_shape).astype(np.float32)
        
        # Warm-up
        for _ in range(3):
            interpreter.set_tensor(input_details[0]["index"], dummy)
            interpreter.invoke()
        
        # Mesure latence sur 10 runs
        times = []
        for _ in range(10):
            interpreter.set_tensor(input_details[0]["index"], dummy)
            t0 = time.perf_counter()
            interpreter.invoke()
            times.append((time.perf_counter() - t0) * 1000)
        
        output = interpreter.get_tensor(output_details[0]["index"])
        mean_ms = np.mean(times)
        p95_ms = np.percentile(times, 95)
        
        check_latency = mean_ms <= VALIDATION_TARGETS["max_inference_ms"]
        results["checks"]["inference"] = {
            "mean_ms": round(mean_ms, 1),
            "p95_ms": round(p95_ms, 1),
            "output_shape": list(output.shape),
            "passed": True  # L'inférence a réussi
        }
        
        icon = "✅" if check_latency else "⚠️"
        print(f"✅ Inférence     : OK (output shape: {list(output.shape)})")
        print(f"{icon} Latence CPU   : {mean_ms:.1f}ms moy | {p95_ms:.1f}ms P95 "
              f"(cible < {VALIDATION_TARGETS['max_inference_ms']}ms)")
        
    except Exception as e:
        results["checks"]["inference"] = {"passed": False, "error": str(e)}
        print(f"❌ Inférence     : ÉCHEC - {e}")
        results["passed"] = False
    
    # ── Résumé ───────────────────────────────────────────────────
    failed = [k for k, v in results["checks"].items() if not v.get("passed", True)]
    results["passed"] = len(failed) == 0
    
    print(f"\n{'='*55}")
    if results["passed"]:
        print(f"✅ VALIDATION RÉUSSIE - Modèle prêt pour Android")
    else:
        print(f"⚠️  VALIDATION PARTIELLE - Points à vérifier : {', '.join(failed)}")
    print(f"{'='*55}\n")
    
    return results


def benchmark_on_device_simulation(tflite_path: str, num_runs: int = 50) -> dict:
    """
    Simule les performances sur différents profils d'appareils Android.
    
    Les multiplicateurs de latence sont basés sur des benchmarks réels
    de TFLite sur mobile vs PC :
      - Redmi 9A (Helio G35) : ~8-12x plus lent que CPU x86 moderne
      - Tecno Spark 7 (Helio G85) : ~5-8x
      - Infinix Hot 12 (Helio G85) : ~5-8x
    """
    if not HAS_TF:
        return {}
    
    interpreter = tf.lite.Interpreter(model_path=tflite_path)
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()
    
    is_int8 = input_details[0]["dtype"] == np.int8
    dummy = (np.random.randint(-128, 127, input_details[0]["shape"], dtype=np.int8)
             if is_int8 else
             np.random.rand(*input_details[0]["shape"]).astype(np.float32))
    
    # Mesure sur CPU local
    for _ in range(5):
        interpreter.set_tensor(input_details[0]["index"], dummy)
        interpreter.invoke()
    
    times = []
    for _ in range(num_runs):
        interpreter.set_tensor(input_details[0]["index"], dummy)
        t0 = time.perf_counter()
        interpreter.invoke()
        times.append((time.perf_counter() - t0) * 1000)
    
    local_mean_ms = np.mean(times)
    
    # Projections appareils cibles
    device_profiles = {
        "PC/Dev (mesure réelle)": local_mean_ms,
        "Redmi 9A (Helio G35)": local_mean_ms * 10,
        "Tecno Spark 7 (Helio G85)": local_mean_ms * 6,
        "Infinix Hot 12 (Helio G85)": local_mean_ms * 6,
        "Samsung A03 (Snapdragon 450)": local_mean_ms * 8,
        "Itel P37 (Unisoc SC9863A)": local_mean_ms * 12,
    }
    
    print(f"\n{'='*55}")
    print(f"  BENCHMARK SIMULÉ - Appareils VOXIA Cibles")
    print(f"{'='*55}")
    
    target_ms = VALIDATION_TARGETS["max_inference_ms"]
    for device, est_ms in device_profiles.items():
        status = "✅" if est_ms <= target_ms else "⚠️"
        bar = "█" * min(int(est_ms / 50), 20)
        print(f"{status} {device:<40} : {est_ms:5.0f}ms {bar}")
    
    print(f"\n   Cible YOLOv8n : < {target_ms}ms par inférence")
    print(f"   Note : Les appareils Android utilisent NNAPI + INT8 qui peut")
    print(f"          réduire la latence de 2-4x vs CPU pure.")
    print(f"{'='*55}\n")
    
    return {device: round(ms, 1) for device, ms in device_profiles.items()}


def save_validation_report(results: dict, output_path: str = "tflite_validation_report.json"):
    """Sauvegarde le rapport de validation en JSON."""
    with open(output_path, "w") as f:
        json.dump(results, f, indent=2)
    print(f"[RAPPORT] Sauvegardé : {output_path}")


def parse_args():
    parser = argparse.ArgumentParser(description="VOXIA - Export et validation TFLite INT8")
    
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--export", action="store_true", default=True,
                      help="Exporter le modèle (défaut)")
    mode.add_argument("--validate-only", action="store_true",
                      help="Valider un TFLite existant sans exporter")
    
    parser.add_argument("--weights", default="runs/train/voxia_yolov8n/weights/best.pt",
                        help="Poids PyTorch source (.pt)")
    parser.add_argument("--tflite", default=OUTPUT_NAME,
                        help="Chemin TFLite pour validation ou sortie")
    parser.add_argument("--calibration-data", default=None,
                        help="Dossier images calibration INT8")
    parser.add_argument("--assets-dir", default=ASSETS_DIR,
                        help="Dossier assets Android")
    parser.add_argument("--benchmark", action="store_true",
                        help="Lancer le benchmark simulé multi-appareils")
    parser.add_argument("--report", default="tflite_validation_report.json",
                        help="Chemin du rapport JSON")
    return parser.parse_args()


def main():
    args = parse_args()
    
    if args.validate_only:
        # Mode validation seule
        results = validate_tflite_model(args.tflite)
    else:
        # Mode export complet
        tflite_path = export_model(
            weights=args.weights,
            calibration_data=args.calibration_data,
            output_name=args.tflite,
            assets_dir=args.assets_dir
        )
        if tflite_path:
            results = validate_tflite_model(tflite_path)
        else:
            print("[ERREUR] Export échoué, validation annulée")
            sys.exit(1)
    
    if args.benchmark and Path(args.tflite).exists():
        benchmark_on_device_simulation(args.tflite)
    
    if results:
        save_validation_report(results, args.report)
    
    sys.exit(0 if results.get("passed", False) else 1)


if __name__ == "__main__":
    main()
