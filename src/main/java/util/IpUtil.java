package util;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;

public class IpUtil {

    private static final String DB_HOST = "172.20.41.61";
    private static final int    DB_PORT = 5432;

    /**
     * Retorna o IP local que esta máquina usa para chegar ao servidor.
     * Abre um socket para o banco e lê qual IP local o SO escolheu para
     * essa rota — sempre o IP correto da rede local.
     */
    public static String getIpLocal() {
        // Método principal: socket para o servidor de banco
        try (Socket socket = new Socket(DB_HOST, DB_PORT)) {
            String ip = socket.getLocalAddress().getHostAddress();
            System.out.println("[IpUtil] IP detectado via socket: " + ip);
            return ip;
        } catch (Exception e) {
            System.out.println("[IpUtil] Socket falhou: " + e.getMessage());
        }

        // Fallback: procura interface com IP da rede 172.20.x.x (rede POLITEC)
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String fallback = null;

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!(addr instanceof Inet4Address)) continue;
                    if (addr.isLoopbackAddress()) continue;

                    String ip = addr.getHostAddress();
                    if (ip.startsWith("172.20.")) return ip;
                    if (ip.startsWith("172.") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
                        if (fallback == null) fallback = ip;
                    }
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
}