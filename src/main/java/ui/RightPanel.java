package ui;

import service.ConfigService;
import service.Configuracao;

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

        // Adiciona espaço antes da logo (abaixa ela)
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(new Color(10, 34, 64));
        logoPanel.add(Box.createVerticalStrut(40), BorderLayout.NORTH); // ↓↓↓ LOGO DESCENDO
        logoPanel.add(logoLabel, BorderLayout.CENTER);

        // Painel central com título, subtítulo e campo
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(10, 34, 64));

        // Título
        JLabel titleLabel = new JLabel("POLITEC");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Gerência de Perícias de Computação");
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Label do nome
        JLabel nomeLabel = new JLabel("Informe seu nome:");
        nomeLabel.setForeground(Color.WHITE);
        nomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        nomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Campo de texto
        nomeField = new JTextField(20);
        nomeField.setMaximumSize(new Dimension(300, 35));
        nomeField.setHorizontalAlignment(SwingConstants.CENTER);
        nomeField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        nomeField.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 🔹 Pré-carrega o último nome usado nesse computador (pelo IP)
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            ConfigService configService = new ConfigService();
            Configuracao cfg = configService.buscarPorIp(ip);

            if (cfg != null && cfg.getNome() != null && !cfg.getNome().trim().isEmpty()) {
                nomeField.setText(cfg.getNome());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            // se der erro, só não preenche e segue normal
        }

        // Botão OK
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(10, 34, 64));

        okButton = new JButton("OK");
        okButton.setBackground(new Color(76, 175, 80));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        okButton.setPreferredSize(new Dimension(120, 40));

        buttonPanel.add(okButton);

        // --- Montagem do painel central com espaçamento refinado ---
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

        // Adiciona na tela principal
        add(logoPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        // Listener do botão
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
            ConfigService configService = new ConfigService();
            configService.salvarOuAtualizarConfiguracao(nome, ip);

            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            frame.getContentPane().removeAll();

            // passa o nome para a próxima tela (LeftPanel também puxa do banco se precisar)
            frame.getContentPane().add(new LeftPanel(nome));

            frame.revalidate();
            frame.repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao conectar ao banco.\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
