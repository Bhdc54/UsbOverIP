package ui;

import service.ConfigService;
import service.Configuracao;

import javax.swing.*;
import java.awt.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class RightPanel extends JPanel {
    private JTextField nomeField;
    private JButton okButton;

    public RightPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(10, 34, 64));

        JLabel logoLabel = new JLabel(new ImageIcon(getClass().getResource("/logoPolitec.png")));
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(new Color(10, 34, 64));
        logoPanel.add(Box.createVerticalStrut(40), BorderLayout.NORTH);
        logoPanel.add(logoLabel, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(10, 34, 64));

        JLabel titleLabel = new JLabel("POLITEC");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Gerência de Perícias de Computação");
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nomeLabel = new JLabel("Informe seu nome:");
        nomeLabel.setForeground(Color.WHITE);
        nomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        nomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        nomeField = new JTextField(20);
        nomeField.setMaximumSize(new Dimension(300, 35));
        nomeField.setHorizontalAlignment(SwingConstants.CENTER);
        nomeField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        nomeField.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Pré-carrega o último nome usado nesse computador (pelo IP)
        try {
            String ip = getIpLocal();
            ConfigService configService = new ConfigService();
            Configuracao cfg = configService.buscarPorIp(ip);
            if (cfg != null && cfg.getNome() != null && !cfg.getNome().trim().isEmpty()) {
                nomeField.setText(cfg.getNome());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(10, 34, 64));

        okButton = new JButton("OK");
        okButton.setBackground(new Color(76, 175, 80));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        okButton.setPreferredSize(new Dimension(120, 40));
        buttonPanel.add(okButton);

        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(subtitleLabel);
        centerPanel.add(Box.createVerticalStrut(35));
        centerPanel.add(nomeLabel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(nomeField);
        centerPanel.add(Box.createVerticalStrut(25));
        centerPanel.add(buttonPanel);

        add(logoPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        okButton.addActionListener(e -> conectar());
    }

    private void conectar() {
        String nome = nomeField.getText().trim();
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite um nome!");
            return;
        }

        try {
            String ip = getIpLocal();
            ConfigService configService = new ConfigService();
            configService.salvarOuAtualizarConfiguracao(nome, ip);

            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            frame.getContentPane().removeAll();
            frame.getContentPane().add(new LeftPanel(nome));
            frame.revalidate();
            frame.repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao conectar ao banco.\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Retorna o IP da rede local — prefere 172.x, 192.168.x, 10.x
    // Ignora IPs de VPN, loopback e interfaces virtuais
    private String getIpLocal() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String fallback = null;

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) continue;

                String nome = ni.getName() != null ? ni.getName().toLowerCase() : "";
                if (nome.contains("vpn") || nome.contains("tun") || nome.contains("tap")
                        || nome.contains("docker") || nome.contains("veth")
                        || nome.contains("vmware") || nome.contains("virtualbox")) continue;

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!(addr instanceof Inet4Address)) continue;
                    if (addr.isLoopbackAddress()) continue;

                    String ip = addr.getHostAddress();
                    if (ip.startsWith("172.") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
                        System.out.println("[RightPanel] IP local: " + ip + " (" + ni.getDisplayName() + ")");
                        return ip;
                    }
                    if (fallback == null) fallback = ip;
                }
            }
            if (fallback != null) return fallback;
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}