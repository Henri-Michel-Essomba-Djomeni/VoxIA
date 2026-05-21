import NetInfo, { NetInfoState } from '@react-native-community/netinfo';

export type ConnectionMode = 'offline' | 'online';

class NetworkService {
  private mode: ConnectionMode = 'offline';
  private listeners: ((mode: ConnectionMode) => void)[] = [];

  init() {
    NetInfo.addEventListener((state: NetInfoState) => {
      const newMode: ConnectionMode =
        state.isConnected && state.isInternetReachable ? 'online' : 'offline';

      if (newMode !== this.mode) {
        this.mode = newMode;
        console.log(`[Lumina] Mode switched: ${newMode}`);
        this.listeners.forEach(fn => fn(newMode));
      }
    });
  }

  getMode(): ConnectionMode {
    return this.mode;
  }

  isOnline(): boolean {
    return this.mode === 'online';
  }

  onChange(fn: (mode: ConnectionMode) => void) {
    this.listeners.push(fn);
    return () => {
      this.listeners = this.listeners.filter(l => l !== fn);
    };
  }
}

export default new NetworkService();