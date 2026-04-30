package ui;

import service.UsbService;
import service.ConfigService;
import service.Configuracao;
import service.UsoUsbService;
import service.UsoUsb;

import websocket.PolitecWebSocketClient;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LeftPanel extends JPanel {

    private JLabel nomeLabel;
    private JLabel ipLabel;
    private JPanel devicesPanel;

    private final UsbService usbService = new UsbService();
    private final UsoUsbService usoUsbService = new UsoUsbService();

    private String usuarioAtual;
    private volatile String ipLocal = "";

    private PolitecWebSocketClient wsClient;
    private Timer autoRefreshTimer;

    private volatile boolean atualizando = false;

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
        add(scrollPane, BorderLayout.CENTER);

        // ----- FOOTER -----
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(new Color(10, 40, 90));
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel versaoLabel = new JLabel("Versão 2.3");
        versaoLabel.setForeground(new Color(180, 180, 180));
        versaoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JLabel devLabel = new JLabel("© 2026 - GPC | Brunno Camargo");
        devLabel.setForeground(new Color(180, 180, 180));
        devLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        footerPanel.add(versaoLabel, BorderLayout.WEST);
        footerPanel.add(devLabel, BorderLayout.EAST);
        add(footerPanel, BorderLayout.SOUTH);

        // Limpa portas corrompidas na inicialização
        new Thread(() -> {
            System.out.println("[LeftPanel] Verificando portas orfas...");
            List<String> portasOrfas = usbService.listarPortasOrfas();

            if (!portasOrfas.isEmpty()) {
                System.out.println("[LeftPanel] " + portasOrfas.size() + " porta(s) orfa(s) — limpando...");
                usbService.detachAllOrphanPorts();

                int tentativas = 0;
                while (ipLocal.isEmpty() && tentativas++ < 100) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }

                if (!ipLocal.isEmpty()) {
                    List<UsoUsb> meusUsos = usoUsbService.listarUsosAtivosPorIp(ipLocal);
                    for (UsoUsb uso : meusUsos) {
                        System.out.println("[LeftPanel] Limpando registro fantasma: busid=" + uso.getBusid());
                        usoUsbService.encerrarUso(uso.getBusid());
                    }
                }
            } else {
                System.out.println("[LeftPanel] Nenhuma porta orfa.");
            }

            SwingUtilities.invokeLater(LeftPanel.this::atualizarListaDispositivos);
        }).start();

        iniciarWebSocket();
        iniciarAutoRefresh();
    }

    // -----------------------------------------------------------------------
    // WebSocket com reconexão automática via PolitecWebSocketClient
    // -----------------------------------------------------------------------
    private void iniciarWebSocket() {
        String wsUrl = "ws://172.20.41.61:8080";
        try {
            wsClient = new PolitecWebSocketClient(wsUrl);

            wsClient.setOnIpReceived(ip -> SwingUtilities.invokeLater(() -> {
                ipLocal = ip;
                ipLabel.setForeground(Color.WHITE);
                ipLabel.setText("IP: " + ip);
                System.out.println("[LeftPanel] IP recebido via WebSocket: " + ip);

                try {
                    new ConfigService().salvarOuAtualizarConfiguracao(usuarioAtual, ip);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                atualizarListaDispositivos();
            }));

            wsClient.setOnMessage(msg -> {
                System.out.println("[WS-LeftPanel] STATE recebido — atualizando lista");
                SwingUtilities.invokeLater(this::atualizarListaDispositivos);
            });

            wsClient.connect();
        } catch (Exception e) {
            System.out.println("[WS-LeftPanel] Nao foi possivel conectar ao WebSocket");
            e.printStackTrace();
        }
    }

    private void iniciarAutoRefresh() {
        // Timer de segurança — caso o WebSocket perca alguma mensagem
        autoRefreshTimer = new Timer(10_000, e -> atualizarListaDispositivos());
        autoRefreshTimer.setRepeats(true);
        autoRefreshTimer.start();
    }

    // -----------------------------------------------------------------------
    // Atualização da lista de dispositivos
    // -----------------------------------------------------------------------
    private void atualizarListaDispositivos() {
        if (atualizando) return;
        atualizando = true;

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            ArrayList<String> dispositivosFisicos = new ArrayList<>();
            List<UsoUsb> usosAtivos = new ArrayList<>();
            Set<String> busidsAnexadosLocalmente = new HashSet<>();
            Map<String, UsoUsb> usoMap = new HashMap<>();

            @Override
            protected Void doInBackground() {
                try {
                    dispositivosFisicos = usbService.listUsbDevices();
                } catch (Exception e) { e.printStackTrace(); }

                try {
                    usosAtivos = usoUsbService.listarUsosAtivos();
                    for (UsoUsb uso : usosAtivos) usoMap.put(uso.getBusid(), uso);
                } catch (Exception e) { e.printStackTrace(); }

                try {
                    busidsAnexadosLocalmente = usbService.listarBusidsAnexados();
                } catch (Exception e) { e.printStackTrace(); }

                if (!ipLocal.isEmpty()) {
                    for (UsoUsb uso : new ArrayList<>(usosAtivos)) {
                        String busid = uso.getBusid();
                        if (!ipLocal.equals(uso.getIpMaquina())) continue;
                        if (busidsAnexadosLocalmente.contains(busid)) continue;

                        boolean voltouParaServidor = dispositivosFisicos.stream()
                                .anyMatch(d -> d.startsWith(busid + " ") || d.startsWith(busid + "-"));

                        if (voltouParaServidor) {
                            System.out.println("[REFRESH] Fantasma desta maquina, limpando banco: busid=" + busid);
                            usoUsbService.encerrarUso(busid);
                            usoMap.remove(busid);
                        }
                    }
                }

                return null;
            }

            @Override
            protected void done() {
                try { redesenharCards(); }
                finally { atualizando = false; }
            }

            private void redesenharCards() {
                devicesPanel.removeAll();
                Set<String> busidsComCard = new HashSet<>();

                for (String disp : dispositivosFisicos) {
                    String busid = disp.split(" - ")[0].trim();
                    busidsComCard.add(busid);
                    devicesPanel.add(criarCardDispositivo(disp, usoMap.get(busid)));
                    devicesPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                }

                for (UsoUsb uso : usosAtivos) {
                    String busid = uso.getBusid();
                    if (busidsComCard.contains(busid)) continue;

                    boolean estaMinhaFantasma = ipLocal.equals(uso.getIpMaquina())
                            && !busidsAnexadosLocalmente.contains(busid);
                    if (estaMinhaFantasma) continue;

                    String dispFake = busid + " - EM USO por " + uso.getUsuario();
                    devicesPanel.add(criarCardDispositivo(dispFake, uso));
                    devicesPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                    busidsComCard.add(busid);
                }

                if (devicesPanel.getComponentCount() == 0) {
                    JLabel vazio = new JLabel("Nenhum dispositivo USB disponivel.");
                    vazio.setForeground(Color.WHITE);
                    vazio.setHorizontalAlignment(SwingConstants.CENTER);
                    vazio.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    JPanel vazioPanel = new JPanel(new BorderLayout());
                    vazioPanel.setBackground(new Color(10, 40, 90));
                    vazioPanel.add(vazio, BorderLayout.CENTER);
                    devicesPanel.add(vazioPanel);
                }

                devicesPanel.revalidate();
                devicesPanel.repaint();
            }
        };

        worker.execute();
    }

    // -----------------------------------------------------------------------
    // Card de dispositivo
    // -----------------------------------------------------------------------
    private JPanel criarCardDispositivo(String disp, UsoUsb usoAtual) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 60, 120));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        String textoParaExibir = disp;
        if (disp.toLowerCase().contains("aladdin")) {
            textoParaExibir = disp.split(" - ")[0].trim() + " - Cellebrite Physical Analyser";
        }

        JLabel nomeDisp = new JLabel(textoParaExibir);
        nomeDisp.setForeground(Color.WHITE);
        nomeDisp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(nomeDisp, BorderLayout.CENTER);

        String busid = disp.split(" - ")[0].trim();
        final String usuarioBotao = (usuarioAtual != null && !usuarioAtual.trim().isEmpty())
                ? usuarioAtual : "desconhecido";

        JButton botao = new JButton();
        botao.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botao.setFocusPainted(false);
        botao.setPreferredSize(new Dimension(160, 35));
        botao.setForeground(Color.WHITE);

        boolean emUso = (usoAtual != null);
        boolean ehDono = emUso
                && usoAtual.getUsuario() != null
                && usuarioAtual != null
                && usoAtual.getUsuario().equalsIgnoreCase(usuarioAtual);

        if (emUso) {
            LocalDateTime inicio = usoAtual.getInicioUso().toLocalDateTime();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");
            botao.setBackground(new Color(200, 60, 60));
            botao.setText("<html><center>" + usoAtual.getUsuario()
                    + "<br>" + inicio.format(fmt) + "</center></html>");
            botao.setEnabled(ehDono);
            botao.setToolTipText(ehDono
                    ? "Clique para liberar o dispositivo."
                    : "Em uso por " + usoAtual.getUsuario() + ". Apenas esse usuario pode liberar.");
        } else {
            botao.setBackground(new Color(76, 175, 80));
            botao.setText("Atribuir");
            botao.setEnabled(!ipLocal.isEmpty());
            botao.setToolTipText(ipLocal.isEmpty()
                    ? "Aguardando IP do servidor..."
                    : "Clique para atribuir este dispositivo a voce.");
        }

        JPanel btnWrapper = new JPanel(new GridBagLayout());
        btnWrapper.setOpaque(false);
        btnWrapper.add(botao);
        panel.add(btnWrapper, BorderLayout.EAST);

        botao.addActionListener(e -> tratarCliqueBotao(busid, usuarioBotao, botao, panel));
        return panel;
    }

    // -----------------------------------------------------------------------
    // Ação de attach/detach
    // -----------------------------------------------------------------------
    private void tratarCliqueBotao(String busid, String usuarioBotao,
                                   JButton botao, JPanel panel) {

        if (ipLocal.isEmpty()) {
            JOptionPane.showMessageDialog(panel,
                    "Aguarde a conexão com o servidor para obter seu IP.",
                    "Aguardando IP", JOptionPane.WARNING_MESSAGE);
            return;
        }

        botao.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            String erro = null;
            boolean eraAtribuir = botao.getText().contains("Atribuir");

            @Override
            protected Void doInBackground() {
                if (eraAtribuir) {
                    UsoUsb usoExistente = usoUsbService.buscarUsoAtivo(busid);
                    if (usoExistente != null) {
                        erro = "Este dispositivo ja foi atribuido por: " + usoExistente.getUsuario();
                        return null;
                    }
                    if (usbService.attachUsbDevice(busid)) {
                        boolean registrado = usoUsbService.registrarUso(busid, usuarioBotao, ipLocal);
                        if (!registrado) {
                            usbService.detachUsbDevice(busid);
                            UsoUsb vencedor = usoUsbService.buscarUsoAtivo(busid);
                            erro = "Dispositivo atribuido por outro usuario simultaneamente"
                                    + (vencedor != null ? ": " + vencedor.getUsuario() : "") + ".";
                        }
                    } else {
                        erro = "Falha ao atribuir o dispositivo.\nVerifique se o usbip esta acessivel.";
                    }
                } else {
                    UsoUsb uso = usoUsbService.buscarUsoAtivo(busid);
                    if (uso != null && !uso.getUsuario().equalsIgnoreCase(usuarioAtual)) {
                        erro = "Voce nao pode liberar um dispositivo em uso por: " + uso.getUsuario();
                        return null;
                    }
                    usbService.detachUsbDevice(busid);
                    usoUsbService.encerrarUso(busid);
                }
                return null;
            }

            @Override
            protected void done() {
                if (erro != null) {
                    JOptionPane.showMessageDialog(panel, erro, "Aviso", JOptionPane.WARNING_MESSAGE);
                }
                atualizarListaDispositivos();
            }
        };

        worker.execute();
    }

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

    // --- IMPORTANTE: usa fechar() para sinalizar ao WebSocket que é encerramento intencional
    public void encerrar() {
        try { if (autoRefreshTimer != null) autoRefreshTimer.stop(); } catch (Exception e) { e.printStackTrace(); }
        try { if (wsClient != null) wsClient.fechar(); } catch (Exception e) { e.printStackTrace(); }
    }
}