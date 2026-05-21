import * as Speech from 'expo-speech';
import * as Haptics from 'expo-haptics';
import NetworkService from './NetworkService';

export interface SpeakOptions {
  urgent?: boolean;      // alerte obstacle → voix plus rapide
  slow?: boolean;        // lecture document → voix plus lente
  onDone?: () => void;   // callback quand la parole est terminée
}

class VoiceService {
  private isSpeaking: boolean = false;
  private queue: { text: string; options: SpeakOptions }[] = [];

  // ─── PARLER ──────────────────────────────────────────
  async speak(text: string, options: SpeakOptions = {}) {
    // Retour haptique avant de parler
    await Haptics.impactAsync(
      options.urgent
        ? Haptics.ImpactFeedbackStyle.Heavy
        : Haptics.ImpactFeedbackStyle.Light
    );

    // Si déjà en train de parler → on met en file
    if (this.isSpeaking) {
      this.queue.push({ text, options });
      return;
    }

    await this._speak(text, options);
  }

  private async _speak(text: string, options: SpeakOptions) {
    this.isSpeaking = true;

    // Paramètres vocaux selon le contexte
    const rate = options.urgent ? 1.4 : options.slow ? 0.75 : 1.0;
    const pitch = options.urgent ? 1.2 : 1.0;

    Speech.speak(text, {
      language: 'fr-FR',
      rate,
      pitch,
      onDone: () => {
        this.isSpeaking = false;
        options.onDone?.();
        this._processQueue(); // parler le suivant dans la file
      },
      onError: () => {
        this.isSpeaking = false;
        this._processQueue();
      },
    });
  }

  // ─── FILE D'ATTENTE ───────────────────────────────────
  private _processQueue() {
    if (this.queue.length === 0) return;
    const next = this.queue.shift()!;
    this._speak(next.text, next.options);
  }

  // ─── STOPPER ─────────────────────────────────────────
  stop() {
    Speech.stop();
    this.isSpeaking = false;
    this.queue = [];
  }

  // ─── ANNONCER LE MODE RÉSEAU ──────────────────────────
  announceMode(mode: 'offline' | 'online') {
    const text =
      mode === 'online'
        ? 'Connexion internet détectée. Passage en mode haute qualité.'
        : 'Connexion perdue. Passage en mode hors ligne.';
    this.speak(text, { urgent: false });
  }

  isCurrentlySpeaking(): boolean {
    return this.isSpeaking;
  }
}

export default new VoiceService();