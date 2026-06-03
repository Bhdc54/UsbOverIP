import javax.swing.SwingUtilities;
import ui.MainFrame;
import updater.Updater;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class Main {
    public static void main(String[] args) {
        // Verifica atualização antes de abrir a interface
        String ip = getIpLocal();
        new Updater(ip).verificar();

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    private static String getIpLocal() {
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
                    if (!(addr instanceof Inet4Address) || addr.isLoopbackAddress()) continue;
                    String ipStr = addr.getHostAddress();
                    if (ipStr.startsWith("172.") || ipStr.startsWith("192.168.") || ipStr.startsWith("10."))
                        return ipStr;
                    if (fallback == null) fallback = ipStr;
                }
            }
            if (fallback != null) return fallback;
        } catch (Exception e) { e.printStackTrace(); }
        try { return InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { return "127.0.0.1"; }
    }
}