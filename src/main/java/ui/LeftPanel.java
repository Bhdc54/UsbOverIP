package ui;

import service.UsbService;
import service.ConfigService;
import service.Configuracao;
import service.UsoUsbService;
import service.UsoUsb;

import websocket.PolitecWebSocketClient;

import javax.swing.*;  // já traz javax.swing.Timer
import java.awt.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
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

    // nome que será usado na tela toda
    private String usuarioAtual;

    // cliente WebSocket
    private PolitecWebSocketClient wsClient;

    // timer para atualizar a tela periodicamente
    private Timer autoRefreshTimer;

    // ============================
    // CONSTRUTORES
    // ============================
    public LeftPanel() {
        this(null);
    }

    public LeftPanel(String usuario) {
        setLayout(new BorderLayout());
        setBackground(new Color(10, 40, 90));

        ConfigService configService = new ConfigService();
        String ipDetectado = "";

        usuarioAtual = usuario;

        try {
            ipDetectado = InetAddress.getLocalHost().getHostAddress();

            Configuracao cfg = configService.buscarPorIp(ipDetectado);
            if (cfg != null && cfg.getNome() != null && !cfg.getNome().trim().isEmpty()) {
                usuarioAtual = cfg.getNome();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (usuarioAtual == null || usuarioAtual.trim().isEmpty()) {
            usuarioAtual = "desconhecido";
        }

        // Topo com usuário/IP
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setBackground(new Color(10, 40, 90));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        nomeLabel = new JLabel("Usuário: " + usuarioAtual);
        nomeLabel.setForeground(Color.WHITE);
        nomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        ipLabel = new JLabel("IP: " + ipDetectado);
        ipLabel.setForeground(Color.WHITE);
        ipLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        topPanel.add(nomeLabel);
        topPanel.add(ipLabel);

        // Cabeçalho com botão Atualizar
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(10, 40, 90));

        headerPanel.add(topPanel, BorderLayout.WEST);

        JButton atualizarButton = new JButton("Atualizar");
        atualizarButton.setBackground(new Color(33, 150, 243));
        atualizarButton.setForeground(Color.WHITE);
        atualizarButton.setFocusPainted(false);
        atualizarButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        atualizarButton.setPreferredSize(new Dimension(110, 32));
        atualizarButton.addActionListener(e -> atualizarListaDispositivos());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnPanel.setOpaque(false);
        btnPanel.add(atualizarButton);

        headerPanel.add(btnPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Painel dos dispositivos USB
        devicesPanel = new JPanel();
        devicesPanel.setLayout(new BoxLayout(devicesPanel, BoxLayout.Y_AXIS));
        devicesPanel.setBackground(new Color(10, 40, 90));
        devicesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(devicesPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
        // desenvolvido por 
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(new Color(10, 40, 90));
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // lado esquerdo (versão)
        JLabel versaoLabel = new JLabel("Versão 1.0");
        versaoLabel.setForeground(new Color(180, 180, 180));
        versaoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        // lado direito (crédito)
        JLabel devLabel = new JLabel("© 2026 - GPC | Brunno Camargo");
        devLabel.setForeground(new Color(180, 180, 180));
        devLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        // adiciona nos lados
        footerPanel.add(versaoLabel, BorderLayout.WEST);
        footerPanel.add(devLabel, BorderLayout.EAST);

        // adiciona no painel principal
add(footerPanel, BorderLayout.SOUTH);

        // primeira carga
        atualizarListaDispositivos();

        // inicia WebSocket para ouvir mudanças vindas do Raspberry
        iniciarWebSocket();

        // inicia refresh automático a cada 10 segundos
        iniciarAutoRefresh();
    }

    // ============================
    // WEBSOCKET CLIENT
    // ============================
    private void iniciarWebSocket() {
        String wsUrl = "ws://172.20.41.61:8080";  // ajuste se esse IP mudar

        try {
            wsClient = new PolitecWebSocketClient(wsUrl);

            wsClient.setOnMessage(msg -> {
                System.out.println("[WS-LeftPanel] Mensagem recebida: " + msg);
                SwingUtilities.invokeLater(this::atualizarListaDispositivos);
            });

            wsClient.connect();
            System.out.println("[WS-LeftPanel] Cliente WebSocket tentando conectar em " + wsUrl);

        } catch (Exception e) {
            System.out.println("[WS-LeftPanel] Não foi possível conectar ao WebSocket");
            e.printStackTrace();
        }
    }

    // ============================
    // AUTO REFRESH A CADA 10s
    // ============================
    private void iniciarAutoRefresh() {
        autoRefreshTimer = new Timer(10_000, e -> {
            System.out.println("[AUTO-REFRESH] Atualizando lista de dispositivos...");
            atualizarListaDispositivos();
        });
        autoRefreshTimer.setRepeats(true);
        autoRefreshTimer.start();
    }

    // ============================
    // ATUALIZAÇÃO DA LISTA
    // ============================
    private void atualizarListaDispositivos() {
        devicesPanel.removeAll();
        
        ArrayList<String> dispositivosFisicos = new ArrayList<>();
        try {
            dispositivosFisicos = usbService.listUsbDevices();
        } catch (Exception e) {
            e.printStackTrace();
            JLabel erro = new JLabel("Erro ao listar dispositivos USB.");
            erro.setForeground(Color.WHITE);
            erro.setHorizontalAlignment(SwingConstants.CENTER);
            erro.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            JPanel erroPanel = new JPanel(new BorderLayout());
            erroPanel.setBackground(new Color(10, 40, 90));
            erroPanel.add(erro, BorderLayout.CENTER);
            devicesPanel.add(erroPanel);

            devicesPanel.revalidate();
            devicesPanel.repaint();
            return;
        }

        // Usos ativos no banco
        List<UsoUsb> usosAtivos = usoUsbService.listarUsosAtivos();

        // Busids que o usbip.exe diz que estão anexados neste cliente
        Set<String> busidsAnexados = usbService.listarBusidsAnexados();

        // Conjunto para não duplicar cards
        Set<String> busidsComCard = new HashSet<>();

        // 1) Cria cards para os dispositivos físicos atuais
        for (String disp : dispositivosFisicos) {
            String busid = disp.split(" - ")[0].trim();
            busidsComCard.add(busid);

            JPanel card = criarCardDispositivo(disp);
            devicesPanel.add(card);
            devicesPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        // 2) Garante que todo uso ativo apareça, mesmo que o dispositivo
        //    não esteja na lista física (por exemplo, ainda anexado a outro cliente).
        for (UsoUsb uso : usosAtivos) {
            String busid = uso.getBusid();

            // Se o dispositivo NÃO está mais anexado para este cliente
            // e apareceu de novo na lista física, limpamos o uso “fantasma”.
            if (!busidsAnexados.contains(busid)
                    && dispositivosFisicos.stream().anyMatch(d -> d.startsWith(busid + " "))) {
                // Já voltou a ficar físico e não está anexado -> libera no banco
                usoUsbService.encerrarUso(busid);
                continue; // vai aparecer como livre na próxima atualização
            }

            // Se ainda não criamos card para ele, mostra como “EM USO”
            if (!busidsComCard.contains(busid)) {
                String dispFake = busid + " - EM USO";
                JPanel card = criarCardDispositivo(dispFake);
                devicesPanel.add(card);
                devicesPanel.add(Box.createRigidArea(new Dimension(0, 8)));

                busidsComCard.add(busid);
            }
        }

        if (devicesPanel.getComponentCount() == 0) {
            JLabel vazio = new JLabel("Nenhum dispositivo USB disponível.");
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

    // ============================
    // CRIAÇÃO DOS CARDS
    // ============================
    private JPanel criarCardDispositivo(String disp) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 60, 120));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // ---- renomeia Aladdin -> Cellebrite ----
        String textoParaExibir = disp;
        String dispLower = disp.toLowerCase();
        if (dispLower.contains("aladdin")) {
            String busidTmp = disp.split(" - ")[0].trim();
            textoParaExibir = busidTmp + " - Cellebrite Physical Analyser";
        }

        JLabel nomeDisp = new JLabel(textoParaExibir);
        nomeDisp.setForeground(Color.WHITE);
        nomeDisp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(nomeDisp, BorderLayout.CENTER);

        String busid = disp.split(" - ")[0].trim();

        String usuarioBotao = (usuarioAtual != null && !usuarioAtual.trim().isEmpty())
                ? usuarioAtual
                : "desconhecido";

        JButton botao = new JButton();
        botao.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botao.setFocusPainted(false);
        botao.setPreferredSize(new Dimension(160, 35));
        botao.setForeground(Color.WHITE);

        UsoUsb usoAtual = usoUsbService.buscarUsoAtivo(busid);
        boolean emUso = (usoAtual != null);
        boolean ehDono =
                emUso &&
                usoAtual.getUsuario() != null &&
                usuarioAtual != null &&
                usoAtual.getUsuario().equalsIgnoreCase(usuarioAtual);

        if (emUso) {
            LocalDateTime inicio = usoAtual.getInicioUso().toLocalDateTime();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");
            String horario = inicio.format(fmt);

            botao.setBackground(new Color(200, 60, 60)); // vermelho
            botao.setText("<html><center>" + usoAtual.getUsuario() +
                    "<br>" + horario + "</center></html>");

            if (ehDono) {
                botao.setEnabled(true);
                botao.setToolTipText("Clique para liberar o dispositivo.");
            } else {
                botao.setEnabled(false);
                botao.setToolTipText("Em uso por " + usoAtual.getUsuario()
                        + ". Apenas esse usuário pode liberar.");
            }
        } else {
            botao.setBackground(new Color(76, 175, 80)); // verde
            botao.setText("Atribuir");
            botao.setEnabled(true);
            botao.setToolTipText("Clique para atribuir este dispositivo a você.");
        }

        JPanel btnWrapper = new JPanel(new GridBagLayout());
        btnWrapper.setOpaque(false);
        btnWrapper.add(botao);
        panel.add(btnWrapper, BorderLayout.EAST);

        botao.addActionListener(e -> {
            UsoUsb uso = usoUsbService.buscarUsoAtivo(busid);

            if (uso != null &&
                uso.getUsuario() != null &&
                usuarioAtual != null &&
                !uso.getUsuario().equalsIgnoreCase(usuarioAtual)) {

                JOptionPane.showMessageDialog(
                        panel,
                        "Este dispositivo está em uso por: " + uso.getUsuario(),
                        "Dispositivo em uso",
                        JOptionPane.WARNING_MESSAGE
                );
                atualizarListaDispositivos();
                return;
            }

            if (botao.getText().startsWith("Atribuir")) {

                if (uso != null) {
                    JOptionPane.showMessageDialog(
                            panel,
                            "Este dispositivo já está em uso por: " + uso.getUsuario(),
                            "Dispositivo em uso",
                            JOptionPane.WARNING_MESSAGE
                    );
                    atualizarListaDispositivos();
                    return;
                }

                if (usbService.attachUsbDevice(busid)) {
                    try {
                        String ip = InetAddress.getLocalHost().getHostAddress();
                        usoUsbService.registrarUso(busid, usuarioBotao, ip);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");
                    String horario = LocalDateTime.now().format(fmt);

                    botao.setText("<html><center>" + usuarioBotao +
                            "<br>" + horario + "</center></html>");
                    botao.setBackground(new Color(200, 60, 60));
                    botao.setEnabled(true);
                    botao.setToolTipText("Clique para liberar o dispositivo.");

                    atualizarListaDispositivos();
                }

            } else {
                if (usbService.detachUsbDevice(busid)) {
                    usoUsbService.encerrarUso(busid);
                    botao.setText("Atribuir");
                    botao.setBackground(new Color(76, 175, 80));
                    botao.setEnabled(true);
                    botao.setToolTipText("Clique para atribuir este dispositivo a você.");
                    atualizarListaDispositivos();
                }
            }
        });

        return panel;
    }

    // ============================
    // ENCERRAR (timer + websocket)
    // ============================
    public void encerrar() {
        try {
            if (autoRefreshTimer != null) {
                autoRefreshTimer.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (wsClient != null && wsClient.isOpen()) {
                wsClient.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}