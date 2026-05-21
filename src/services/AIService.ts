import NetworkService from './NetworkService';

// Types
export interface AIResponse {
  text: string;
  mode: 'offline' | 'online';
  latency: number;
}

export interface AIRequest {
  prompt: string;
  imageBase64?: string; // pour la vision caméra
  context?: string;     // historique conversation
}

class AIService {

  // ─── CERVEAU PRINCIPAL ───────────────────────────────
  async think(request: AIRequest): Promise<AIResponse> {
    const start = Date.now();

    if (NetworkService.isOnline()) {
      try {
        return await this.thinkOnline(request, start);
      } catch (error) {
        console.warn('[Lumina] Cloud failed, falling back to offline');
        return await this.thinkOffline(request, start);
      }
    } else {
      return await this.thinkOffline(request, start);
    }
  }

  // ─── MODE ONLINE : Claude API ────────────────────────
  private async thinkOnline(request: AIRequest, start: number): Promise<AIResponse> {
    const messages: any[] = [];

    // Si on a une image (vision caméra)
    if (request.imageBase64) {
      messages.push({
        role: 'user',
        content: [
          {
            type: 'image',
            source: {
              type: 'base64',
              media_type: 'image/jpeg',
              data: request.imageBase64,
            },
          },
          { type: 'text', text: request.prompt },
        ],
      });
    } else {
      messages.push({
        role: 'user',
        content: request.prompt,
      });
    }

    const response = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': process.env.EXPO_PUBLIC_ANTHROPIC_KEY || '',
        'anthropic-version': '2023-06-01',
      },
      body: JSON.stringify({
        model: 'claude-sonnet-4-20250514',
        max_tokens: 1024,
        system: `Tu es Lumina, un assistant vocal pour personnes malvoyantes. 
                 Réponds toujours en français, de façon claire et concise. 
                 Tes réponses seront lues à voix haute, évite le markdown.`,
        messages,
      }),
    });

    const data = await response.json();
    const text = data.content[0].text;

    return {
      text,
      mode: 'online',
      latency: Date.now() - start,
    };
  }

  // ─── MODE OFFLINE : réponses locales ─────────────────
  private async thinkOffline(request: AIRequest, start: number): Promise<AIResponse> {
    // Pour l'instant : règles simples en attendant Phi-3
    const prompt = request.prompt.toLowerCase();
    let text = '';

    if (prompt.includes('heure')) {
      const now = new Date();
      text = `Il est ${now.getHours()} heures ${now.getMinutes()} minutes.`;
    } else if (prompt.includes('bonjour') || prompt.includes('salut')) {
      text = 'Bonjour ! Je suis Lumina. Je suis en mode hors ligne. Comment puis-je vous aider ?';
    } else if (prompt.includes('date') || prompt.includes('jour')) {
      text = `Nous sommes le ${new Date().toLocaleDateString('fr-FR', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}.`;
    } else {
      text = `Je suis en mode hors ligne. Je ne peux pas répondre à cette question pour l'instant. Connectez-vous à internet pour accéder à toutes mes fonctionnalités.`;
    }

    return {
      text,
      mode: 'offline',
      latency: Date.now() - start,
    };
  }
}

export default new AIService();