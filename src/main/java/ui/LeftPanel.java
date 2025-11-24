package ui;

import service.UsbService;
import service.ConfigService;
import service.Configuracao;
import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.util.ArrayList;


public class LeftPanel extends JPanel {
    private JLabel nomeLabel;
    private JLabel ipLabel;
    private JPanel devicesPanel;
    private UsbService usbService = new UsbService();

    // nome que será usado na tela toda
    private String usuarioAtual;

    // Construtor padrão (caso ainda seja usado)
    public LeftPanel() {
        this(null);
    }

    // Construtor recebendo nome opcional (da tela de login)
    public LeftPanel(String usuario) {
        setLayout(new BorderLayout());
        setBackground(new Color(10, 40, 90));

        ConfigService configService = new ConfigService();
        String ipDetectado = "";

        // começa com o que veio da tela de login
        usuarioAtual = usuario;

        try {
            ipDetectado = InetAddress.getLocalHost().getHostAddress();

            // 👉 tenta buscar no banco pelo IP
            Configuracao cfg = configService.buscarPorIp(ipDetectado);
            if (cfg != null && cfg.getNome() != null && !cfg.getNome().trim().isEmpty()) {
                usuarioAtual = cfg.getNome();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // fallback caso continue nulo/vazio
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

        atualizarListaDispositivos();
    }

    private void atualizarListaDispositivos() {
        devicesPanel.removeAll();

        ArrayList<String> dispositivos = usbService.listUsbDevices();
        if (dispositivos.isEmpty()) {
            JLabel vazio = new JLabel("Nenhum dispositivo USB disponível.");
            vazio.setForeground(Color.WHITE);
            vazio.setHorizontalAlignment(SwingConstants.CENTER);
            vazio.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            JPanel vazioPanel = new JPanel(new BorderLayout());
            vazioPanel.setBackground(new Color(10, 40, 90));
            vazioPanel.add(vazio, BorderLayout.CENTER);
            devicesPanel.add(vazioPanel);
        } else {
            for (String disp : dispositivos) {
                JPanel card = criarCardDispositivo(disp);
                devicesPanel.add(card);
                devicesPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }

        devicesPanel.revalidate();
        devicesPanel.repaint();
    }

    private JPanel criarCardDispositivo(String disp) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 60, 120));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70)); // card baixinho

        // Texto do dispositivo
        JLabel nomeDisp = new JLabel(disp);
        nomeDisp.setForeground(Color.WHITE);
        nomeDisp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(nomeDisp, BorderLayout.CENTER);

        // Nome que aparecerá no botão
        String usuarioBotao = (usuarioAtual != null && !usuarioAtual.trim().isEmpty())
                ? usuarioAtual
                : "desconhecido";

        // Botão pequeno
        JButton botao = new JButton("Atribuir");
        botao.setBackground(new Color(76, 175, 80));
        botao.setForeground(Color.WHITE);
        botao.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botao.setFocusPainted(false);
        botao.setPreferredSize(new Dimension(140, 32));

        JPanel btnWrapper = new JPanel(new GridBagLayout());
        btnWrapper.setOpaque(false);
        btnWrapper.add(botao);

        panel.add(btnWrapper, BorderLayout.EAST);

        String finalUsuarioBotao = usuarioBotao;

        botao.addActionListener(e -> {
            // Extrair busid do texto (antes do " - ")
            String busid = disp.split(" - ")[0].trim();

            if (botao.getText().startsWith("Atribuir")) {
                if (usbService.attachUsbDevice(busid)) {
                    botao.setText(finalUsuarioBotao);
                    botao.setBackground(new Color(200, 60, 60)); 
                }
            } else {
                if (usbService.detachUsbDevice(busid)) {
                    botao.setText("Atribuir");
                    botao.setBackground(new Color(76, 175, 80)); 
                }
            }
        });

        return panel;
    }
}
