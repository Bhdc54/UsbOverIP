package ui;

import service.UsbService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("POLITEC - Usb over IP ");
        setSize(800, 600);

        // Vamos controlar o fechamento manualmente
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Ícone da janela
        setIconImage(new ImageIcon(getClass().getResource("/logoPolitec.png")).getImage());

        // Tela inicial (login)
        add(new RightPanel(), BorderLayout.CENTER);

        // Listener para quando clicar no X
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 1) Desconecta todos os USBs anexados neste cliente
                try {
                    System.out.println("[MainFrame] Fechando app, detachAllDevices...");
                    UsbService usbService = new UsbService();
                    usbService.detachAllDevices();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // 2) Fecha a janela e encerra o programa
                dispose();
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
