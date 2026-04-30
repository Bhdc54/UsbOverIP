package ui;

import service.UsbService;
import service.UsoUsbService;
import service.UsoUsb;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;

public class MainFrame extends JFrame {

    private final UsbService usbService = new UsbService();
    private final UsoUsbService usoUsbService = new UsoUsbService();

    public MainFrame() {
        setTitle("POLITEC - Usb over IP ");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setIconImage(new ImageIcon(getClass().getResource("/logoPolitec.png")).getImage());

        add(new RightPanel(), BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fecharAplicacaoComSeguranca();
            }
        });
    }

    private void fecharAplicacaoComSeguranca() {
        try {
            if (getContentPane().getComponentCount() > 0) {
                Component comp = getContentPane().getComponent(0);
                if (comp instanceof LeftPanel) {
                    ((LeftPanel) comp).encerrar();
                }
            }

            String ip = getIpLocal();
            System.out.println("[MainFrame] Encerrando usos para IP: " + ip);

            List<UsoUsb> meusUsos = usoUsbService.listarUsosAtivosPorIp(ip);

            for (UsoUsb uso : meusUsos) {
                String busid = uso.getBusid();
                System.out.println("[MainFrame] Detach em " + busid);
                try {
                    usbService.detachUsbDevice(busid);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                usoUsbService.encerrarUso(busid);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        dispose();
        System.exit(0);
    }

    // Retorna IP da rede local — ignora VPN, loopback e interfaces virtuais
    private String getIpLocal() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String fallback = null;

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) continue;

                String nome = ni.getName() != null ? ni.getName().toLowerCase() : "";
                if (nome.contains("vpn") || nome.contains("tun") || nome.contains("tap")
                        || nome.contains("docker") || nome.contains("veth")
                        || nome.contains("vmware") || nome.contains("virtualbox")) continue;

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!(addr instanceof Inet4Address)) continue;
                    if (addr.isLoopbackAddress()) continue;

                    String ip = addr.getHostAddress();
                    if (ip.startsWith("172.") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
                        return ip;
                    }
                    if (fallback == null) fallback = ip;
                }
            }
            if (fallback != null) return fallback;
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}