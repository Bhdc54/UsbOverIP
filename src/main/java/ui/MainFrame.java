package ui;

import service.UsbService;
import service.UsoUsbService;
import service.UsoUsb;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.util.List;

public class MainFrame extends JFrame {

    private final UsbService usbService = new UsbService();
    private final UsoUsbService usoUsbService = new UsoUsbService();

    public MainFrame() {
        setTitle("POLITEC - Usb over IP ");
        setSize(800, 600);

        // vamos controlar o fechamento manualmente
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ícone da janela
        setIconImage(new ImageIcon(getClass().getResource("/logoPolitec.png")).getImage());

        // abre com a tela de login
        add(new RightPanel(), BorderLayout.CENTER);

        // listener para quando clicar no X
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fecharAplicacaoComSeguranca();
            }
        });
    }

    private void fecharAplicacaoComSeguranca() {
        try {
            // 0) Se estiver com LeftPanel aberto, manda ele parar timer/WebSocket
            if (getContentPane().getComponentCount() > 0) {
                Component comp = getContentPane().getComponent(0);
                if (comp instanceof LeftPanel) {
                    ((LeftPanel) comp).encerrar();
                }
            }

            // 1) descobre IP da máquina
            String ip = InetAddress.getLocalHost().getHostAddress();
            System.out.println("[MainFrame] Encerrando usos para IP: " + ip);

            // 2) busca todos os usos ativos desta máquina
            List<UsoUsb> meusUsos = usoUsbService.listarUsosAtivosPorIp(ip);

            // 3) para cada busid, tenta detach e marca como livre no banco
            for (UsoUsb uso : meusUsos) {
                String busid = uso.getBusid();
                System.out.println("[MainFrame] Detach em " + busid + " (usuário " + uso.getUsuario() + ")");

                try {
                    boolean ok = usbService.detachUsbDevice(busid);
                    if (!ok) {
                        System.out.println("[MainFrame] Aviso: detach falhou para " + busid + " (pode já estar solto)");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // de qualquer forma, limpa o 'em_uso' no banco
                usoUsbService.encerrarUso(busid);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 4) fecha UI e encerra o programa
        dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
