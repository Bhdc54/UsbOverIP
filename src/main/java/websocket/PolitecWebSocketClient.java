package websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

public class PolitecWebSocketClient extends WebSocketClient {

    // callback que o LeftPanel registra para tratar mensagens
    private Consumer<String> onMessageHandler;

    public PolitecWebSocketClient(String serverUrl) {
        super(URI.create(serverUrl));
    }

    // LeftPanel chama isso para registrar o callback
    public void setOnMessage(Consumer<String> handler) {
        this.onMessageHandler = handler;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[WS] Conectado ao servidor WebSocket");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("[WS] Mensagem recebida (client): " + message);

        if (onMessageHandler != null) {
            onMessageHandler.accept(message);   // repassa pro LeftPanel
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[WS] Conexão WebSocket fechada: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("[WS] Erro no WebSocket");
        ex.printStackTrace();
    }
}
