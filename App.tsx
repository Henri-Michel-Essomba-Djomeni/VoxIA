import { useEffect, useState } from 'react';
import { StyleSheet, View, Text, TouchableOpacity, StatusBar } from 'react-native';
import NetworkService, { ConnectionMode } from './src/services/NetworkService';
import AIService from './src/services/AIService';
import VoiceService from './src/services/VoiceService';
import LockScreen from './src/screens/LockScreen';

export default function App() {
  const [mode, setMode] = useState<ConnectionMode>('offline');
  const [status, setStatus] = useState('Initialisation...');
  const [isThinking, setIsThinking] = useState(false);
  const [lastResponse, setLastResponse] = useState('');
  const [unlocked, setUnlocked] = useState(false);

  useEffect(() => {
    // Démarrer le surveillant réseau
    NetworkService.init();

    // Écouter les changements de connexion
    const unsubscribe = NetworkService.onChange((newMode) => {
      setMode(newMode);
      VoiceService.announceMode(newMode);
    });

    // Message de bienvenue
    setTimeout(() => {
      VoiceService.speak(
        'Bonjour, je suis Lumina. Votre assistant vocal est prêt.',
        { onDone: () => setStatus('Prêt — Appuyez pour parler') }
      );
      setStatus('Prêt — Appuyez pour parler');
    }, 1000);

    return () => unsubscribe();
  }, []);

  // ─── TEST : poser une question à Lumina ──────────────
  const askLumina = async (question: string) => {
    if (isThinking) return;

    setIsThinking(true);
    setStatus('Lumina réfléchit...');
    VoiceService.stop();

    const response = await AIService.think({ prompt: question });

    setLastResponse(response.text);
    setStatus(`Réponse (${response.mode}) — ${response.latency}ms`);
    setIsThinking(false);

    VoiceService.speak(response.text);
  };

  if (!unlocked) {
    return <LockScreen onUnlocked={() => setUnlocked(true)} />;
  }

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" />

      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>LUMINA</Text>
        <Text style={styles.subtitle}>Voir le monde avec les oreilles</Text>
      </View>

      {/* Indicateur mode réseau */}
      <View style={[styles.badge, mode === 'online' ? styles.online : styles.offline]}>
        <Text style={styles.badgeText}>
          {mode === 'online' ? '🌐 Mode Cloud (Claude)' : '📴 Mode Hors-ligne'}
        </Text>
      </View>

      {/* Status */}
      <Text style={styles.status}>{status}</Text>

      {/* Réponse */}
      {lastResponse !== '' && (
        <View style={styles.responseBox}>
          <Text style={styles.responseText}>{lastResponse}</Text>
        </View>
      )}

      {/* Boutons de test */}
      <View style={styles.buttons}>
        <TouchableOpacity
          style={[styles.btn, isThinking && styles.btnDisabled]}
          onPress={() => askLumina('Bonjour Lumina !')}
        >
          <Text style={styles.btnText}>👋 Bonjour</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.btn, isThinking && styles.btnDisabled]}
          onPress={() => askLumina('Quelle heure est-il ?')}
        >
          <Text style={styles.btnText}>🕐 Heure</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.btn, isThinking && styles.btnDisabled]}
          onPress={() => askLumina('Décris ce que tu vois autour de moi')}
        >
          <Text style={styles.btnText}>👁️ Décrire</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.btn, isThinking && styles.btnDisabled]}
          onPress={() => askLumina('Quelle est la date aujourd\'hui ?')}
        >
          <Text style={styles.btnText}>📅 Date</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0D1B2A',
    alignItems: 'center',
    paddingTop: 60,
    paddingHorizontal: 24,
  },
  header: {
    alignItems: 'center',
    marginBottom: 32,
  },
  title: {
    fontSize: 42,
    fontWeight: 'bold',
    color: '#FFFFFF',
    letterSpacing: 8,
  },
  subtitle: {
    fontSize: 14,
    color: '#C8991A',
    marginTop: 6,
    fontStyle: 'italic',
  },
  badge: {
    paddingHorizontal: 20,
    paddingVertical: 8,
    borderRadius: 20,
    marginBottom: 24,
  },
  online: { backgroundColor: '#1A4A2A' },
  offline: { backgroundColor: '#3A1A1A' },
  badgeText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '600',
  },
  status: {
    color: '#888',
    fontSize: 13,
    marginBottom: 20,
    textAlign: 'center',
  },
  responseBox: {
    backgroundColor: '#1A2A3A',
    borderRadius: 16,
    padding: 20,
    marginBottom: 32,
    width: '100%',
    borderLeftWidth: 3,
    borderLeftColor: '#C8991A',
  },
  responseText: {
    color: '#FFFFFF',
    fontSize: 16,
    lineHeight: 24,
  },
  buttons: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 12,
  },
  btn: {
    backgroundColor: '#1E3A5F',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderRadius: 12,
    minWidth: 140,
    alignItems: 'center',
  },
  btnDisabled: { opacity: 0.4 },
  btnText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '600',
  },
});