package ui;

import service.UsbService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TokenPanel extends JPanel {
    private UsbService usbService;

    public TokenPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        usbService = new UsbService();
        atualizarLista();
    }

    private void atualizarLista() {
        removeAll(); // limpa antes de redesenhar

        List<String> dispositivos = usbService.listUsbDevices();

        if (dispositivos.isEmpty()) {
            add(new JLabel("Nenhum dispositivo USB disponível."));
        } else {
            for (String device : dispositivos) {
                add(criarCard(device));
            }
        }

        revalidate();
        repaint();
    }

    private JPanel criarCard(String deviceInfo) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        card.setBackground(new Color(20, 50, 100));

        // Quebra busid + nome
        String[] parts = deviceInfo.split(" - ", 2);
        String busid = parts[0];
        String nome = (parts.length > 1) ? parts[1] : "Desconhecido";

        JLabel nomeLabel = new JLabel(nome);
        nomeLabel.setForeground(Color.WHITE);
        nomeLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JButton actionButton = new JButton("Atribuir");
        actionButton.setBackground(new Color(0, 180, 0));
        actionButton.setForeground(Color.WHITE);
        actionButton.setFocusPainted(false);

        actionButton.addActionListener(e -> {
            if (actionButton.getText().equals("Atribuir")) {
                boolean ok = usbService.attachUsbDevice(busid);
                if (ok) {
                    actionButton.setText("Em uso");
                    actionButton.setBackground(Color.RED);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Falha ao atribuir " + nome,
                        "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                boolean ok = usbService.detachUsbDevice(busid);
                if (ok) {
                    actionButton.setText("Atribuir");
                    actionButton.setBackground(new Color(0, 180, 0));
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Falha ao desatribuir " + nome,
                        "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        card.add(nomeLabel, BorderLayout.WEST);
        card.add(actionButton, BorderLayout.EAST);

        return card;
    }
}
