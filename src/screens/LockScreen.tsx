import { useEffect, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ActivityIndicator } from 'react-native';
import * as LocalAuthentication from 'expo-local-authentication';
import * as Haptics from 'expo-haptics';
import VoiceService from '../services/VoiceService';

interface Props {
  onUnlocked: () => void;
}

export default function LockScreen({ onUnlocked }: Props) {
  const [status, setStatus] = useState('Initialisation...');
  const [hasBiometrics, setHasBiometrics] = useState(false);
  const [failed, setFailed] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    checkBiometrics();
  }, []);

  // ─── VÉRIFIER SI LE TÉLÉPHONE A UNE BIOMÉTRIE ────────
  const checkBiometrics = async () => {
        const compatible = await LocalAuthentication.hasHardwareAsync();
        const enrolled = await LocalAuthentication.isEnrolledAsync();

        if (compatible && enrolled) {
          // Biométrie disponible
          setHasBiometrics(true);
          setStatus('Appuyez pour déverrouiller');
          VoiceService.speak('Veuillez vous authentifier pour accéder à Lumina.');
          setTimeout(() => authenticate(), 800);
        } else {
          // Pas de biométrie → fallback PIN/schéma du téléphone
          setStatus('Utilisez votre code PIN ou schéma');
          VoiceService.speak('Veuillez entrer votre code de déverrouillage.');
          setTimeout(() => authenticate(), 800); // ← on appelle quand même authenticate()
        }
    };

  // ─── AUTHENTIFICATION ─────────────────────────────────
  const authenticate = async () => {
    if (loading) return;
    setLoading(true);
    setFailed(false);

    VoiceService.speak('Veuillez vous authentifier.');

    const result = await LocalAuthentication.authenticateAsync({
      promptMessage: 'Déverrouiller Lumina',
      fallbackLabel: 'Utiliser le code PIN',
      cancelLabel: 'Annuler',
      disableDeviceFallback: false,
    });

    setLoading(false);

    if (result.success) {
      await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      VoiceService.speak('Bienvenue. Lumina est prêt.', {
        onDone: () => onUnlocked(),
      });
      setStatus('Déverrouillé !');
    } else {
      await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
      VoiceService.speak('Authentification échouée. Réessayez.');
      setFailed(true);
      setStatus('Échec — Réessayez');
    }
  };

  return (
    <View style={styles.container}>

      {/* Logo */}
      <View style={styles.header}>
        <Text style={styles.title}>LUMINA</Text>
        <Text style={styles.subtitle}>Voir le monde avec les oreilles</Text>
      </View>

      {/* Icône biométrie */}
      <TouchableOpacity
        style={[styles.biometricBtn, failed && styles.biometricFailed]}
        onPress={authenticate}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator size="large" color="#C8991A" />
        ) : (
          <Text style={styles.biometricIcon}>
            {failed ? '❌' : '👆'}
          </Text>
        )}
      </TouchableOpacity>

      {/* Status vocal */}
      <Text style={styles.status}>{status}</Text>

      {/* Bouton manuel si auto échoue */}
      {failed && (
        <TouchableOpacity style={styles.retryBtn} onPress={authenticate}>
          <Text style={styles.retryText}>Réessayer</Text>
        </TouchableOpacity>
      )}

      {/* Bas de page */}
      <Text style={styles.footer}>
        Sécurisé par biométrie · Lumina v1.0
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0D1B2A',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  header: {
    alignItems: 'center',
    marginBottom: 60,
  },
  title: {
    fontSize: 48,
    fontWeight: 'bold',
    color: '#FFFFFF',
    letterSpacing: 10,
  },
  subtitle: {
    fontSize: 14,
    color: '#C8991A',
    marginTop: 8,
    fontStyle: 'italic',
  },
  biometricBtn: {
    width: 120,
    height: 120,
    borderRadius: 60,
    backgroundColor: '#1E3A5F',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: '#C8991A',
    marginBottom: 32,
  },
  biometricFailed: {
    borderColor: '#FF4444',
    backgroundColor: '#3A1A1A',
  },
  biometricIcon: {
    fontSize: 48,
  },
  status: {
    color: '#888',
    fontSize: 15,
    textAlign: 'center',
    marginBottom: 24,
  },
  retryBtn: {
    backgroundColor: '#1E3A5F',
    paddingHorizontal: 32,
    paddingVertical: 14,
    borderRadius: 12,
    marginBottom: 24,
  },
  retryText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  footer: {
    position: 'absolute',
    bottom: 40,
    color: '#444',
    fontSize: 12,
  },
});