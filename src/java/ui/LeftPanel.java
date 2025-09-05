package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import config.ConfigManager;
import service.UsbService;

public class LeftPanel extends JPanel {
    private String usuarioApp;
    private JTextField nomeField;
    private JButton okButton;
    private JButton refreshButton;

    public LeftPanel(ConfigManager configManager, UsbService usbService, Runnable onRefresh) {
        this.usuarioApp = configManager.loadUserName();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(15, 32, 60));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        // Logo
        JLabel logoLabel = new JLabel(LogoLoader.loadLogo(120, 120));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(logoLabel);
        add(Box.createVerticalStrut(20));

        // Título
        JLabel title = new JLabel("POLITEC");
        title.setFont(new Font("Segoe UI", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel infoLabel = new JLabel("Gerência de Perícias de Computação");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(title);
        add(Box.createVerticalStrut(20));
        add(infoLabel);
        add(Box.createVerticalStrut(40));

        // Nome usuário
        JLabel nomeLabel = new JLabel("Informe seu nome:");
        nomeLabel.setForeground(Color.WHITE);
        nomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        nomeField = new JTextField(usuarioApp != null ? usuarioApp : "");
        nomeField.setMaximumSize(new Dimension(200, 30));
        nomeField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        nomeField.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(nomeLabel);
        add(Box.createVerticalStrut(5));
        add(nomeField);

        // Botões
        okButton = new JButton("OK");
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        okButton.setBackground(new Color(76, 175, 80));
        okButton.setForeground(Color.WHITE);
        add(Box.createVerticalStrut(10));
        add(okButton);

        refreshButton = new JButton(" 🔄 Atualizar");
        refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        refreshButton.setBackground(new Color(33, 150, 243));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        add(Box.createVerticalStrut(10));
        add(refreshButton);

        // Ações
        okButton.addActionListener(e -> {
            String nome = nomeField.getText().trim();
            if (!nome.isEmpty()) {
                usuarioApp = nome;
                configManager.saveUserName(usuarioApp);
            } else {
                JOptionPane.showMessageDialog(this, "Digite seu nome.", "Atenção", JOptionPane.WARNING_MESSAGE);
            }
        });

        refreshButton.addActionListener(e -> onRefresh.run());
    }
}
