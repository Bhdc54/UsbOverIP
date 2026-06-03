package ui;

import service.UsbService;
import service.ConfigService;
import service.Configuracao;
import config.AppConfig;

import websocket.PolitecWebSocketClient;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class LeftPanel extends JPanel {

    private JLabel nomeLabel;
    private JLabel ipLabel;
    private JPanel devicesPanel;

    private final UsbService usbService = new UsbService();

    private String usuarioAtual;
    private volatile String ipLocal = "";

    private PolitecWebSocketClient wsClient;
    private javax.swing.Timer autoRefreshTimer;

    private final List<DeviceInfo> estadoAtual = new ArrayList<>();

    public LeftPanel() { this(null); }

    public LeftPanel(String usuario) {
        setLayout(new BorderLayout());
        setBackground(new Color(10, 40, 90));

        usuarioAtual = usuario;

        try {
            String ipFallback = getIpLocalFallback();
            ConfigService configService = new ConfigService();
            Configuracao cfg = configService.buscarPorIp(ipFallback);
            if (cfg != null && cfg.getNome() != null && !cfg.getNome().trim().isEmpty()) {
                usuarioAtual = cfg.getNome();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (usuarioAtual == null || usuarioAtual.trim().isEmpty()) {
            usuarioAtual = "desconhecido";
        }

        // ----- HEADER -----
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setBackground(new Color(10, 40, 90));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        nomeLabel = new JLabel("Usuário: " + usuarioAtual);
        nomeLabel.setForeground(Color.WHITE);
        nomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        ipLabel = new JLabel("IP: aguardando servidor...");
        ipLabel.setForeground(new Color(255, 200, 50));
        ipLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        topPanel.add(nomeLabel);
        topPanel.add(ipLabel);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(10, 40, 90));
        headerPanel.add(topPanel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // ----- CENTRO -----
        devicesPanel = new JPanel();
        devicesPanel.setLayout(new BoxLayout(devicesPanel, BoxLayout.Y_AXIS));
        devicesPanel.setBackground(new Color(10, 40, 90));
        devicesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(devicesPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // ----- FOOTER -----
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(new Color(10, 40, 90));
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel versaoLabel = new JLabel("Versão " + AppConfig.VERSION);
        versaoLabel.setForeground(new Color(180, 180, 180));
        versaoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JLabel devLabel = new JLabel("© 2026 - GPC | Brunno Camargo");
        devLabel.setForeground(new Color(180, 180, 180));
        devLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        footerPanel.add(versaoLabel, BorderLayout.WEST);
        footerPanel.add(devLabel, BorderLayout.EAST);
        add(footerPanel, BorderLayout.SOUTH);

        // Limpa portas órfãs na inicialização
        new Thread(() -> {
            List<String> portasOrfas = usbService.listarPortasOrfas();
            if (!portasOrfas.isEmpty()) {
                usbService.detachAllOrphanPorts();
            }
        }).start();

        iniciarWebSocket();

        autoRefreshTimer = new javax.swing.Timer(15_000, e -> redesenharCards());
        autoRefreshTimer.setRepeats(true);
        autoRefreshTimer.start();
    }

    // -----------------------------------------------------------------------
    // WebSocket
    // -----------------------------------------------------------------------
    private void iniciarWebSocket() {
        String wsUrl = "ws://172.20.41.61:8080";
        try {
            wsClient = new PolitecWebSocketClient(wsUrl);

            wsClient.setOnIpReceived(ip -> SwingUtilities.invokeLater(() -> {
                ipLocal = ip;
                ipLabel.setForeground(Color.WHITE);
                ipLabel.setText("IP: " + ip);
                try {
                    new ConfigService().salvarOuAtualizarConfiguracao(usuarioAtual, ip);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

            wsClient.setOnMessage(msg -> {
                List<DeviceInfo> novosDevices = parsearState(msg);
                SwingUtilities.invokeLater(() -> {
                    estadoAtual.clear();
                    estadoAtual.addAll(novosDevices);
                    redesenharCards();
                });
            });

            wsClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------
    // Parseia o JSON STATE do servidor
    // -----------------------------------------------------------------------
    private List<DeviceInfo> parsearState(String json) {
        List<DeviceInfo> lista = new ArrayList<>();
        try {
            int inicio = json.indexOf("[");
            int fim = json.lastIndexOf("]");
            if (inicio < 0 || fim < 0) return lista;

            String array = json.substring(inicio + 1, fim);
            int depth = 0;
            int start = -1;
            for (int i = 0; i < array.length(); i++) {
                char c = array.charAt(i);
                if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start >= 0) {
                        String obj = array.substring(start, i + 1);
                        DeviceInfo d = new DeviceInfo();
                        d.busid  = pickJson(obj, "busid");
                        d.name   = pickJson(obj, "name");
                        d.status = pickJson(obj, "status");
                        d.user   = pickJson(obj, "user");
                        d.ip     = pickJson(obj, "ip");
                        d.since  = pickJson(obj, "since");
                        if (!d.busid.isEmpty()) lista.add(d);
                        start = -1;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    private static String pickJson(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int i = json.indexOf(pattern);
        if (i < 0) return "";
        int start = i + pattern.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "";
        return json.substring(start, end);
    }

    // -----------------------------------------------------------------------
    // Redesenha os cards
    // -----------------------------------------------------------------------
    private void redesenharCards() {
        devicesPanel.removeAll();

        if (estadoAtual.isEmpty()) {
            JLabel vazio = new JLabel("Nenhum dispositivo USB disponivel.");
            vazio.setForeground(Color.WHITE);
            vazio.setHorizontalAlignment(SwingConstants.CENTER);
            vazio.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            JPanel vazioPanel = new JPanel(new BorderLayout());
            vazioPanel.setBackground(new Color(10, 40, 90));
            vazioPanel.add(vazio, BorderLayout.CENTER);
            devicesPanel.add(vazioPanel);
        } else {
            for (DeviceInfo d : estadoAtual) {
                devicesPanel.add(criarCard(d));
                devicesPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }

        devicesPanel.revalidate();
        devicesPanel.repaint();
    }

    // -----------------------------------------------------------------------
    // Card de dispositivo
    // -----------------------------------------------------------------------
    private JPanel criarCard(DeviceInfo d) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 60, 120));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        String nomeExibir = d.busid + " - ";
        if (d.name.toLowerCase().contains("aladdin")) {
            nomeExibir += "Cellebrite Physical Analyser";
        } else {
            nomeExibir += d.name.isEmpty() ? "USB Device" : d.name;
        }

        JLabel nomeDisp = new JLabel(nomeExibir);
        nomeDisp.setForeground(Color.WHITE);
        nomeDisp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(nomeDisp, BorderLayout.CENTER);

        boolean emUso = "BUSY".equals(d.status);
        boolean ehDono = emUso && d.ip.equals(ipLocal);

        JButton botao = new JButton();
        botao.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botao.setFocusPainted(false);
        botao.setPreferredSize(new Dimension(160, 35));
        botao.setForeground(Color.WHITE);

        if (emUso) {
            String dataHora = "";
            try {
                Instant instant = Instant.parse(d.since);
                LocalDateTime ldt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
                dataHora = ldt.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
            } catch (Exception ignored) {}

            botao.setBackground(new Color(200, 60, 60));
            botao.setText("<html><center>" + d.user + "<br>" + dataHora + "</center></html>");
            botao.setEnabled(ehDono);
            botao.setToolTipText(ehDono
                    ? "Clique para liberar o dispositivo."
                    : "Em uso por " + d.user + ". Apenas esse usuário pode liberar.");
        } else {
            botao.setBackground(new Color(76, 175, 80));
            botao.setText("Atribuir");
            botao.setEnabled(!ipLocal.isEmpty());
            botao.setToolTipText(ipLocal.isEmpty()
                    ? "Aguardando IP do servidor..."
                    : "Clique para atribuir este dispositivo a você.");
        }

        JPanel btnWrapper = new JPanel(new GridBagLayout());
        btnWrapper.setOpaque(false);
        btnWrapper.add(botao);
        panel.add(btnWrapper, BorderLayout.EAST);

        botao.addActionListener(e -> tratarClique(d.busid, botao, panel));
        return panel;
    }

    // -----------------------------------------------------------------------
    // Attach / Detach
    // -----------------------------------------------------------------------
    private void tratarClique(String busid, JButton botao, JPanel panel) {
        if (ipLocal.isEmpty()) {
            JOptionPane.showMessageDialog(panel,
                    "Aguarde a conexão com o servidor para obter seu IP.",
                    "Aguardando IP", JOptionPane.WARNING_MESSAGE);
            return;
        }

        botao.setEnabled(false);
        boolean eraAtribuir = botao.getText().contains("Atribuir");

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                if (eraAtribuir) {
                    boolean ok = usbService.attachUsbDevice(busid);
                    if (ok) {
                        String msg = "{\"type\":\"ATTACHED\",\"busid\":\"" + busid
                                + "\",\"user\":\"" + usuarioAtual
                                + "\",\"ip\":\"" + ipLocal + "\"}";
                        wsClient.send(msg);
                        return null;
                    }
                    return "Falha ao atribuir o dispositivo.\nVerifique se o usbip está acessível.";
                } else {
                    usbService.detachUsbDevice(busid);
                    String msg = "{\"type\":\"DETACHED\",\"busid\":\"" + busid
                            + "\",\"ip\":\"" + ipLocal + "\"}";
                    wsClient.send(msg);
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    String erro = get();
                    if (erro != null) {
                        JOptionPane.showMessageDialog(panel, erro, "Aviso", JOptionPane.WARNING_MESSAGE);
                        botao.setEnabled(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    botao.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    // -----------------------------------------------------------------------
    // IP local fallback
    // -----------------------------------------------------------------------
    private String getIpLocalFallback() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            String fallback = null;
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) continue;
                String nome = ni.getName() != null ? ni.getName().toLowerCase() : "";
                if (nome.contains("vpn") || nome.contains("tun") || nome.contains("tap")
                        || nome.contains("docker") || nome.contains("veth")
                        || nome.contains("vmware") || nome.contains("virtualbox")) continue;
                java.util.Enumeration<java.net.InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (!(addr instanceof java.net.Inet4Address)) continue;
                    if (addr.isLoopbackAddress()) continue;
                    String ip = addr.getHostAddress();
                    if (ip.startsWith("172.") || ip.startsWith("192.168.") || ip.startsWith("10.")) return ip;
                    if (fallback == null) fallback = ip;
                }
            }
            if (fallback != null) return fallback;
        } catch (Exception e) { e.printStackTrace(); }
        try { return java.net.InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { return "127.0.0.1"; }
    }

    public void encerrar() {
        try { if (autoRefreshTimer != null) autoRefreshTimer.stop(); } catch (Exception ignored) {}
        try {
            if (wsClient != null) {
                for (DeviceInfo d : estadoAtual) {
                    if ("BUSY".equals(d.status) && d.ip.equals(ipLocal)) {
                        usbService.detachUsbDevice(d.busid);
                    }
                }
                wsClient.fechar();
            }
        } catch (Exception ignored) {}
    }

    static class DeviceInfo {
        String busid  = "";
        String name   = "";
        String status = "FREE";
        String user   = "";
        String ip     = "";
        String since  = "";
    }
}