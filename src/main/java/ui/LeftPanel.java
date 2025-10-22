package ui;

import config.ConfigManager;
import service.UsbService;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.util.ArrayList;

public class LeftPanel extends JPanel {
    private JLabel nomeLabel;
    private JLabel ipLabel;
    private JPanel devicesPanel;
    private UsbService usbService = new UsbService();

    public LeftPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(10, 40, 90));

        // Topo com usuário/IP
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setBackground(new Color(10, 40, 90));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ConfigManager manager = new ConfigManager();
        String ipDetectado = "";
        String usuario = null;

        try {
            ipDetectado = InetAddress.getLocalHost().getHostAddress();
            usuario = manager.loadUserName(ipDetectado);
        } catch (Exception e) {
            e.printStackTrace();
        }

        nomeLabel = new JLabel("Usuário: " + (usuario != null ? usuario : "desconhecido"));
        nomeLabel.setForeground(Color.WHITE);
        ipLabel = new JLabel("IP: " + ipDetectado);
        ipLabel.setForeground(Color.WHITE);

        topPanel.add(nomeLabel);
        topPanel.add(ipLabel);

        add(topPanel, BorderLayout.NORTH);

        // Painel superior com botão de atualizar
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(10, 40, 90));

        headerPanel.add(topPanel, BorderLayout.WEST);

        JButton atualizarButton = new JButton("Atualizar");
        atualizarButton.setBackground(new Color(33, 150, 243));
        atualizarButton.setForeground(Color.WHITE);
        atualizarButton.setFocusPainted(false);
        atualizarButton.setPreferredSize(new Dimension(120, 35));
        atualizarButton.addActionListener(e -> atualizarListaDispositivos(
                nomeLabel.getText().replace("Usuário: ", "")
        ));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(atualizarButton);

        headerPanel.add(btnPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);


        // Painel dos dispositivos USB
        devicesPanel = new JPanel();
        devicesPanel.setLayout(new BoxLayout(devicesPanel, BoxLayout.Y_AXIS));
        devicesPanel.setBackground(new Color(10, 40, 90));
        add(new JScrollPane(devicesPanel), BorderLayout.CENTER);

        atualizarListaDispositivos(usuario);
    }

    private void atualizarListaDispositivos(String usuario) {
        devicesPanel.removeAll();

        ArrayList<String> dispositivos = usbService.listUsbDevices();
        if (dispositivos.isEmpty()) {
            JLabel vazio = new JLabel("Nenhum dispositivo USB disponível.");
            vazio.setForeground(Color.WHITE);
            vazio.setHorizontalAlignment(SwingConstants.CENTER);
            devicesPanel.add(vazio);
        } else {
            for (String disp : dispositivos) {
                JPanel card = criarCardDispositivo(disp, usuario);
                devicesPanel.add(card);
                devicesPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        devicesPanel.revalidate();
        devicesPanel.repaint();
    }

    private JPanel criarCardDispositivo(String disp, String usuario) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 60, 120));
        panel.setPreferredSize(new Dimension(600, 100));

        JLabel nomeDisp = new JLabel(disp);
        nomeDisp.setForeground(Color.WHITE);
        nomeDisp.setFont(new Font("Arial", Font.BOLD, 14));
        nomeDisp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(nomeDisp, BorderLayout.WEST);

        JButton botao = new JButton("Atribuir");
        botao.setBackground(Color.GREEN);
        botao.setForeground(Color.WHITE);
        botao.setFont(new Font("Arial", Font.BOLD, 12));

        botao.addActionListener(e -> {
            if (botao.getText().equals("Atribuir")) {
                // Extrair busid do texto (antes do " - ")
                String busid = disp.split(" - ")[0].trim();
                if (usbService.attachUsbDevice(busid)) {
                    botao.setText("Em uso por: " + usuario);
                    botao.setBackground(Color.RED);
                }
            } else {
                String busid = disp.split(" - ")[0].trim();
                if (usbService.detachUsbDevice(busid)) {
                    botao.setText("Atribuir");
                    botao.setBackground(Color.GREEN);
                }
            }
        });

        panel.add(botao, BorderLayout.EAST);

        return panel;
    }
}
