package ui;

import config.ConfigManager;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;

public class RightPanel extends JPanel {
    private JTextField nomeField;
    private JButton okButton;

    public RightPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(10, 34, 64));

        // Painel superior com logo
        JLabel logoLabel = new JLabel(new ImageIcon(getClass().getResource("/logoPolitec.png")));
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Painel central com título, subtítulo e campo
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(10, 34, 64));

        // espaço extra para abaixar
        centerPanel.add(Box.createVerticalStrut(60));

        JLabel titleLabel = new JLabel("POLITEC");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Gerência de Perícias de Computação");
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nomeLabel = new JLabel("Informe seu nome:");
        nomeLabel.setForeground(Color.WHITE);
        nomeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        nomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        nomeField = new JTextField(20);
        nomeField.setMaximumSize(new Dimension(250, 30));
        nomeField.setHorizontalAlignment(SwingConstants.CENTER);
        nomeField.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Painel de botões
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(10, 34, 64));
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));

        okButton = new JButton("OK");
        okButton.setBackground(new Color(76, 175, 80));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setPreferredSize(new Dimension(90, 35));

        buttonPanel.add(okButton);
              
        // Monta painel central
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(5));
        centerPanel.add(subtitleLabel);
        centerPanel.add(Box.createVerticalStrut(25));
        centerPanel.add(nomeLabel);
        centerPanel.add(Box.createVerticalStrut(5));
        centerPanel.add(nomeField);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(buttonPanel);

        // Adiciona na tela principal
        add(logoLabel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        // Listeners
        okButton.addActionListener(e -> conectar());
       
    }

    private void conectar() {
        String nome = nomeField.getText().trim();

        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite um nome!");
            return;
        }

        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            ConfigManager manager = new ConfigManager();
            manager.saveUserName(nome, ip);

            JOptionPane.showMessageDialog(this,
                    "Configuração salva!\nUsuário: " + nome + "\nIP: " + ip,
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);

            // Troca para LeftPanel (tela principal)
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            frame.getContentPane().removeAll();
            frame.getContentPane().add(new LeftPanel());
            frame.revalidate();
            frame.repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao conectar ao banco.\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
