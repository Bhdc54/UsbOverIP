package ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import config.ConfigManager;
import service.UsbService;

public class MainFrame extends JFrame {
    private UsbService usbService;
    private ConfigManager configManager;
    private RightPanel rightPanel;

    public MainFrame() {
        usbService = new UsbService();
        configManager = new ConfigManager();

        setTitle("POLITEC - Gerência de Perícias de Computação");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        // Ícone da janela
        ImageIcon logoIcon = LogoLoader.loadLogo(0, 0);
        if (logoIcon != null) {
            setIconImage(logoIcon.getImage());
        }

        // Painéis
        LeftPanel leftPanel = new LeftPanel(configManager, usbService, this::atualizarUSBs);
        rightPanel = new RightPanel();

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(new JScrollPane(rightPanel), BorderLayout.CENTER);

        setContentPane(mainPanel);
        atualizarUSBs();
    }

    private void atualizarUSBs() {
        new Thread(() -> {
            List<String> devices = new ArrayList<>();
            int tentativas = 0;
            while (devices.isEmpty() && tentativas < 3) {
                devices = usbService.listUsbDevices();
                tentativas++;
                if (devices.isEmpty()) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
            final List<String> finalDevices = devices;
            SwingUtilities.invokeLater(() -> rightPanel.atualizar(finalDevices, usbService));
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
