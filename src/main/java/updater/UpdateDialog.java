package updater;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Tela de progresso exibida durante a atualização.
 * Bloqueia interação com o app (modal sem botão de fechar).
 */
public class UpdateDialog extends JDialog {

    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private final JLabel versaoLabel;

    public UpdateDialog(String versaoNova) {
        super((Frame) null, "Atualizando POLITEC", false);
        setSize(420, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setUndecorated(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(10, 40, 90));
        root.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Título
        JLabel titulo = new JLabel("Atualizando para a versão " + versaoNova);
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titulo.setHorizontalAlignment(SwingConstants.CENTER);

        // Barra de progresso
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(76, 175, 80));
        progressBar.setBackground(new Color(20, 60, 120));
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progressBar.setPreferredSize(new Dimension(0, 28));

        // Status
        statusLabel = new JLabel("Iniciando...");
        statusLabel.setForeground(new Color(200, 200, 200));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Aviso
        versaoLabel = new JLabel("Não feche o aplicativo durante a atualização.");
        versaoLabel.setForeground(new Color(255, 200, 50));
        versaoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        versaoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(new Color(10, 40, 90));
        center.add(Box.createVerticalStrut(12));
        center.add(progressBar);
        center.add(Box.createVerticalStrut(8));
        center.add(statusLabel);
        center.add(Box.createVerticalStrut(6));
        center.add(versaoLabel);

        root.add(titulo, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);

        setContentPane(root);
    }

    /** Atualiza status e barra — pode ser chamado de qualquer thread. */
    public void setStatus(String mensagem, int percentual) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(mensagem);
            progressBar.setValue(percentual);
            progressBar.setString(percentual + "%");
        });
    }
}