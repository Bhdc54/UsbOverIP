package ui;

import service.UsbService;
import service.ConfigService;
import service.Configuracao;
import service.UsoUsbService;
import service.UsoUsb;

import websocket.PolitecWebSocketClient;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
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
    private String ipLocal = "";

    private PolitecWebSocketClient wsClient;
    private Timer autoRefreshTimer;

    private volatile boolean atualizando = false;

    public LeftPanel() { this(null); }

    public LeftPanel(String usuario) {
        setLayout(new BorderLayout());
        setBackground(new Color(10, 40, 90));

        usuarioAtual = usuario;

        try {
            ipLocal = InetAddress.getLocalHost().getHostAddress();
            ConfigService configService = new ConfigService();
            Configuracao cfg = configService.buscarPorIp(ipLocal);
            if (cfg != null && cfg.getNome() != null && !cfg.getNome().trim().isEmpty()) {
                usuarioAtual = cfg.getNome();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (usuarioAtual == null || usuarioAtual.trim().isEmpty()) {
            usuarioAtual = "desconhecido";
        }

        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setBackground(new Color(10, 40, 90));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        nomeLabel = new JLabel("Usuário: " + usuarioAtual);
        nomeLabel.setForeground(Color.WHITE);
        nomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        ipLabel = new JLabel("IP: " + ipLocal);
        ipLabel.setForeground(Color.WHITE);
        ipLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        topPanel.add(nomeLabel);
        topPanel.add(ipLabel);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(10, 40, 90));
        headerPanel.add(topPanel, BorderLayout.WEST);

        // Botão removido — a lista atualiza automaticamente
        add(headerPanel, BorderLayout.NORTH);

        devicesPanel = new JPanel();
        devicesPanel.setLayout(new BoxLayout(devicesPanel, BoxLayout.Y_AXIS));
        devicesPanel.setBackground(new Color(10, 40, 90));
        devicesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(devicesPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(new Color(10, 40, 90));
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel versaoLabel = new JLabel("Versão 2.1");
        versaoLabel.setForeground(new Color(180, 180, 180));
        versaoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JLabel devLabel = new JLabel("© 2026 - GPC | Brunno Camargo");
        devLabel.setForeground(new Color(180, 180, 180));
        devLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        footerPanel.add(versaoLabel, BorderLayout.WEST);
        footerPanel.add(devLabel, BorderLayout.EAST);
        add(footerPanel, BorderLayout.SOUTH);

        // Limpa portas corrompidas (?-?) E registros órfãos no banco.
        // Cenário: app fechou sem detach (crash/Alt+F4) — USB voltou ao Raspberry
        // mas banco ainda marca como em_uso=TRUE. Outros usuários viam como ocupado.
        new Thread(() -> {
            System.out.println("[LeftPanel] Verificando portas orfas...");
            List<String> portasOrfas = usbService.listarPortasOrfas();

            if (!portasOrfas.isEmpty()) {
                System.out.println("[LeftPanel] " + portasOrfas.size() + " porta(s) orfa(s) — limpando USB e banco...");

                // 1) Libera as portas presas localmente
                usbService.detachAllOrphanPorts();

                // 2) Limpa registros desta máquina no banco que estejam como em_uso=TRUE
                //    (o USB já voltou ao Raspberry, então o registro é inválido)
                List<UsoUsb> meusUsos = usoUsbService.listarUsosAtivosPorIp(ipLocal);
                for (UsoUsb uso : meusUsos) {
                    System.out.println("[LeftPanel] Limpando registro fantasma no banco: busid=" + uso.getBusid());
                    usoUsbService.encerrarUso(uso.getBusid());
                }
            } else {
                System.out.println("[LeftPanel] Nenhuma porta orfa. Tudo limpo.");
            }

            SwingUtilities.invokeLater(LeftPanel.this::atualizarListaDispositivos);
        }).start();

        iniciarWebSocket();
        iniciarAutoRefresh();
    }

    private void iniciarWebSocket() {
        String wsUrl = "ws://172.20.41.61:8080";
        try {
            wsClient = new PolitecWebSocketClient(wsUrl);
            wsClient.setOnMessage(msg -> {
                System.out.println("[WS-LeftPanel] Mensagem recebida: " + msg);
                SwingUtilities.invokeLater(this::atualizarListaDispositivos);
            });
            wsClient.connect();
        } catch (Exception e) {
            System.out.println("[WS-LeftPanel] Nao foi possivel conectar ao WebSocket");
            e.printStackTrace();
        }
    }

    private void iniciarAutoRefresh() {
        autoRefreshTimer = new Timer(10_000, e -> atualizarListaDispositivos());
        autoRefreshTimer.setRepeats(true);
        autoRefreshTimer.start();
    }

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

                // -------------------------------------------------------
                // LIMPEZA DE FANTASMAS — SOMENTE para registros desta máquina.
                //
                // REGRA: um registro é fantasma se, E SOMENTE SE:
                //   1. O IP do registro é desta máquina
                //   2. O mapa local (busidToPort) NÃO tem esse busid
                //      (ou seja, não foi anexado nesta sessão)
                //   3. O dispositivo voltou para a lista do servidor (disponível)
                //
                // Usamos usbService.listarBusidsAnexados() que agora retorna
                // exatamente o mapa interno — sem depender do "usbip port" que
                // sempre mostra "?-?" no usbip-win.
                //
                // USBs de outras máquinas NUNCA são tocados.
                // -------------------------------------------------------
                for (UsoUsb uso : new ArrayList<>(usosAtivos)) {
                    String busid = uso.getBusid();

                    if (!ipLocal.equals(uso.getIpMaquina())) continue; // outro cliente, nunca mexe

                    if (busidsAnexadosLocalmente.contains(busid)) continue; // ainda está anexado aqui

                    boolean voltouParaServidor = dispositivosFisicos.stream()
                            .anyMatch(d -> d.startsWith(busid + " ") || d.startsWith(busid + "-"));

                    if (voltouParaServidor) {
                        System.out.println("[REFRESH] Fantasma desta maquina, limpando banco: busid=" + busid);
                        usoUsbService.encerrarUso(busid);
                        usoMap.remove(busid);
                    }
                }

                return null;
            }

            @Override
            protected void done() {
                try { redesenharCards(); }
                finally {
                    atualizando = false;

                }
            }

            private void redesenharCards() {
                devicesPanel.removeAll();
                Set<String> busidsComCard = new HashSet<>();

                // Cards para dispositivos disponíveis no servidor
                for (String disp : dispositivosFisicos) {
                    String busid = disp.split(" - ")[0].trim();
                    busidsComCard.add(busid);
                    devicesPanel.add(criarCardDispositivo(disp, usoMap.get(busid)));
                    devicesPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                }

                // Dispositivos em uso em outras máquinas (não aparecem na lista física)
                for (UsoUsb uso : usosAtivos) {
                    String busid = uso.getBusid();
                    if (busidsComCard.contains(busid)) continue;
                    if (!usoMap.containsKey(busid)) continue; // fantasma já limpo

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
            botao.setEnabled(true);
            botao.setToolTipText("Clique para atribuir este dispositivo a voce.");
        }

        JPanel btnWrapper = new JPanel(new GridBagLayout());
        btnWrapper.setOpaque(false);
        btnWrapper.add(botao);
        panel.add(btnWrapper, BorderLayout.EAST);

        botao.addActionListener(e -> tratarCliqueBotao(busid, usuarioBotao, botao, panel));
        return panel;
    }

    private void tratarCliqueBotao(String busid, String usuarioBotao,
                                   JButton botao, JPanel panel) {
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

    public void encerrar() {
        try { if (autoRefreshTimer != null) autoRefreshTimer.stop(); } catch (Exception e) { e.printStackTrace(); }
        try { if (wsClient != null && wsClient.isOpen()) wsClient.close(); } catch (Exception e) { e.printStackTrace(); }
    }
}