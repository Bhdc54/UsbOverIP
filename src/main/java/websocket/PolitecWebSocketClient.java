package websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

public class PolitecWebSocketClient extends WebSocketClient {

    private Consumer<String> onMessageHandler;
    private Consumer<String> onIpReceived;

    // --- Reconexão automática ---
    private volatile boolean encerrado = false; // true só quando o app fechar
    private static final int RECONNECT_DELAY_MS = 5_000;

    public PolitecWebSocketClient(String serverUrl) {
        super(URI.create(serverUrl));
    }

    public void setOnMessage(Consumer<String> handler) {
        this.onMessageHandler = handler;
    }

    public void setOnIpReceived(Consumer<String> handler) {
        this.onIpReceived = handler;
    }

    /** Chame ao fechar o app para não tentar reconectar mais. */
    public void fechar() {
        encerrado = true;
        try { close(); } catch (Exception ignored) {}
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[WS] Conectado ao servidor WebSocket");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("[WS] Mensagem recebida (client): " + message);

        if (message.contains("\"type\":\"IP_INFO\"")) {
            String ip = pick(message, "ip");
            System.out.println("[WS] IP recebido do servidor: " + ip);
            if (onIpReceived != null && !ip.isEmpty()) {
                onIpReceived.accept(ip);
            }
            return;
        }

        if (onMessageHandler != null) {
            onMessageHandler.accept(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[WS] Conexão WebSocket fechada: " + reason);

        // Se não foi encerramento intencional, agenda reconexão
        if (!encerrado) {
            Thread t = new Thread(() -> {
                try {
                    System.out.println("[WS] Tentando reconectar em " + (RECONNECT_DELAY_MS / 1000) + "s...");
                    Thread.sleep(RECONNECT_DELAY_MS);
                    if (!encerrado) {
                        reconnect();
                        System.out.println("[WS] Reconexão iniciada.");
                    }
                } catch (Exception e) {
                    System.out.println("[WS] Falha ao reconectar: " + e.getMessage());
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("[WS] Erro no WebSocket: " + ex.getMessage());
    }

    private static String pick(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int i = json.indexOf(pattern);
        if (i < 0) return "";
        int start = i + pattern.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "";
        return json.substring(start, end);
    }
}