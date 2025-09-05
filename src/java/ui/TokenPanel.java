package ui;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import service.UsbService;

public class TokenPanel extends JPanel {
    private JLabel usuarioLabel;
    private JLabel horaLabel;
    private JButton statusButton;
    private boolean livre = true;
    private String busid;
    private UsbService usbService;
    private String usuarioApp;

    public TokenPanel(String nomeUSB, String usuarioApp, UsbService usbService) {
        this.busid = nomeUSB.split(" - ")[0];
        this.usuarioApp = usuarioApp;
        this.usbService = usbService;

        configurarPainel();
        adicionarComponentes(nomeUSB);
    }

    /** Configura as propriedades do painel */
    private void configurarPainel() {
        setLayout(new BorderLayout());
        setBackground(new Color(33, 45, 90));
        setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
    }

    /** Adiciona os elementos visuais ao painel */
    private void adicionarComponentes(String nomeUSB) {
        // Nome do dispositivo
        String nomeDispositivo = nomeUSB.split(" - ", 2)[1].split(" ")[0];
        JLabel nomeLabel = new JLabel(nomeDispositivo);
        nomeLabel.setForeground(Color.WHITE);
        nomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        add(nomeLabel, BorderLayout.WEST);

        // Centro: Usuário + Hora
        JPanel center = new JPanel(new GridLayout(2, 1));
        center.setOpaque(false);

        usuarioLabel = new JLabel("Livre", SwingConstants.CENTER);
        usuarioLabel.setForeground(Color.LIGHT_GRAY);
        usuarioLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        horaLabel = new JLabel("", SwingConstants.CENTER);
        horaLabel.setForeground(Color.LIGHT_GRAY);

        center.add(usuarioLabel);
        center.add(horaLabel);
        add(center, BorderLayout.CENTER);

        // Botão de status
        statusButton = new JButton("atribuir");
        statusButton.setBackground(new Color(76, 175, 80));
        statusButton.setForeground(Color.WHITE);
        statusButton.addActionListener(e -> toggleStatus());
        add(statusButton, BorderLayout.EAST);
    }

    /** Alterna o status do dispositivo */
    private void toggleStatus() {
        if (livre) {
            if (usbService.attachUsbDevice(busid)) {
                setStatus(false);
            } else {
                mostrarErro("Falha ao conectar USB.");
            }
        } else {
            if (usbService.detachUsbDevice(busid)) {
                setStatus(true);
            } else {
                mostrarErro("Falha ao desconectar USB.");
            }
        }
    }

    /** Atualiza o status visual do painel */
    private void setStatus(boolean livre) {
        this.livre = livre;
        if (livre) {
            statusButton.setText("atribuir");
            statusButton.setBackground(new Color(76, 175, 80));
            usuarioLabel.setText("Livre");
            horaLabel.setText("");
        } else {
            statusButton.setText("Em uso");
            statusButton.setBackground(new Color(211, 47, 47));
            usuarioLabel.setText(usuarioApp);
            horaLabel.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()));
        }
    }

    /** Mostra mensagem de erro */
    private void mostrarErro(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
