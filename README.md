# 🌟 VoxIA
### *Voir le monde avec les oreilles*

> Assistant vocal intelligent pour les personnes malvoyantes — 100% offline-first, online-enhanced.

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![SDK](https://img.shields.io/badge/Expo%20SDK-55-black)
![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-green)
![License](https://img.shields.io/badge/license-MIT-yellow)

---

## 📖 Table des matières
- [Vision](#-vision)
- [Architecture](#-architecture)
- [Fonctionnalités](#-fonctionnalités)
- [Stack technique](#-stack-technique)
- [Installation](#-installation)
- [Structure du projet](#-structure-du-projet)
- [Contribuer](#-contribuer)
- [Équipe](#-équipe)

---

## 🎯 Vision

VoxIA transforme le téléphone en paire d'yeux intelligente pour les personnes malvoyantes.
Zéro friction, zéro écran à lire — uniquement la voix.

**Trois principes fondateurs :**
- **Offline-first** : fonctionne sans internet, meilleure qualité avec
- **Zéro friction** : dès le lancement, VoxIA parle
- **Évolutif** : chaque fonctionnalité est un module plug-in

---

## 🏗 Architecture

```
COUCHE 1 — PERCEPTION
├── Micro / ASR        → Whisper.cpp (offline) / Whisper API (online)
├── Caméra / Vision    → MobileNet + CLIP (offline) / GPT-4o Vision (online)
├── OCR documents      → Tesseract (offline) / Google Vision (online)
└── Visages            → MediaPipe (offline)

COUCHE 2 — CERVEAU CENTRAL
├── LLM                → Phi-3 Mini (offline) / Claude API (online)
├── Routing            → LangChain
├── Mémoire            → Redis (cache local)
└── Fine-tuning        → Whisper accents FR/AF

COUCHE 3 — EXPRESSION
├── TTS                → Kokoro (offline) / ElevenLabs (online)
├── Audio spatial      → Web Audio API
└── Haptics            → Expo Haptics API
```

---

## ✨ Fonctionnalités

| Fonctionnalité | Version | Statut |
|---|---|---|
| Description de l'environnement | V1 | 🚧 En cours |
| Identification des personnes | V1 | 🚧 En cours |
| Lecture de documents (OCR) | V1 | 🚧 En cours |
| Navigation guidée | V1 | 🚧 En cours |
| Réveil & Rappels | V1 | 🚧 En cours |
| Mode Éducation | V2 | 📅 Planifié |
| Aide aux achats | V2 | 📅 Planifié |
| Assistant Conversationnel | V2 | 📅 Planifié |
| Accès Internet vocal | V3 | 📅 Planifié |

---

## 🛠 Stack technique

| Couche | Technologie | Rôle |
|---|---|---|
| Mobile | React Native + Expo SDK 55 | App cross-platform |
| LLM offline | Phi-3 Mini / Llama 3.2 | Cerveau local |
| LLM online | Claude API (Anthropic) | Cerveau cloud |
| ASR | Whisper.cpp | Voix → Texte offline |
| TTS offline | Kokoro | Texte → Voix local |
| TTS online | ElevenLabs | Texte → Voix cloud |
| Vision | MobileNet + MediaPipe | Analyse caméra offline |
| OCR | Tesseract | Lecture documents offline |
| Auth | Google OAuth + Biométrie | Sécurité |
| Navigation | React Navigation | Routing screens |

---

## 🚀 Installation

### Prérequis
- Node.js 18+
- Expo CLI
- Expo Go sur ton téléphone (Play Store)

### Cloner le projet
```bash
git clone https://github.com/TON_USERNAME/voxia.git
cd voxia
```

### Installer les dépendances
```bash
npm install
```

### Configurer les variables d'environnement
Crée un fichier `.env` à la racine :
```env
EXPO_PUBLIC_ANTHROPIC_KEY=ta_clé_claude_api
EXPO_PUBLIC_GOOGLE_CLIENT_ID=ton_google_client_id
```

### Lancer le projet
```bash
npx expo start
```
Scanne le QR code avec **Expo Go** sur ton téléphone.

---

## 📁 Structure du projet

```
voxia/
├── src/
│   ├── components/           # Composants réutilisables
│   ├── screens/              # Écrans de l'app
│   │   └── LockScreen.tsx    # Écran de déverrouillage biométrique
│   ├── services/             # Logique métier
│   │   ├── AIService.ts      # Cerveau IA (offline/online)
│   │   ├── NetworkService.ts # Détection réseau
│   │   └── VoiceService.ts   # Synthèse vocale + haptics
│   ├── hooks/                # Custom React hooks
│   └── utils/                # Fonctions utilitaires
├── assets/                   # Images, fonts, sons
├── App.tsx                   # Point d'entrée
├── app.json                  # Config Expo
└── README.md
```

---

## 🤝 Contribuer

1. Fork le projet
2. Crée ta branche : `git checkout -b feature/ma-fonctionnalite`
3. Commit tes changements : `git commit -m "feat: ma fonctionnalité"`
4. Push : `git push origin feature/ma-fonctionnalite`
5. Ouvre une **Pull Request**

### Convention de commits
| Préfixe | Usage |
|---|---|
| `feat:` | Nouvelle fonctionnalité |
| `fix:` | Correction de bug |
| `refactor:` | Refactoring |
| `docs:` | Documentation |
| `style:` | Mise en forme |

---

## 👥 Équipe

| Rôle | Responsabilité |
|---|---|
| Lead Dev | Architecture & Services IA |
| Mobile Dev | UI/UX React Native |
| IA Engineer | Fine-tuning & modèles offline |

---

## 📄 Licence

MIT — Conçu pour l'autonomie · Développé avec empathie · VoxIA 2026
