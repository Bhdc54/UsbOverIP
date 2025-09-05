package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import service.UsbService;

public class RightPanel extends JPanel {
    private List<TokenPanel> usbPanels = new ArrayList<>();

    public RightPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(22, 40, 80));
        setBorder(new EmptyBorder(20, 20, 20, 20));
    }

    public void atualizar(List<String> usbNames, UsbService usbService) {
        removeAll();
        usbPanels.clear();

        for (String nome : usbNames) {
            TokenPanel token = new TokenPanel(nome, null, usbService);
            usbPanels.add(token);
            add(token);
            add(Box.createVerticalStrut(10));
        }

        revalidate();
        repaint();
    }
}
