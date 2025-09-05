package service;

import listener.UsbListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UsbService {
    private static final String USBIP_PATH = "C:\\Users\\03357343169\\Downloads\\usbip-win-0.3.6-dev\\usbip.exe";
    private static final String RASPBERRY_IP = "172.20.40.163";
    private String portaAssociada;

    private UsbListener listener;
    private Set<String> dispositivosAtuais = new HashSet<>();
    private volatile boolean monitorando = false;

    public void setListener(UsbListener listener) {
        this.listener = listener;
    }

    public ArrayList<String> listUsbDevices() {
        ArrayList<String> nomes = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(USBIP_PATH, "list", "-r", RASPBERRY_IP);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().matches("\\d+-\\d+(\\.\\d+)*:.*")) {
                    String[] parts = line.trim().split(":", 3);
                    if (parts.length >= 2) {
                        String id = parts[0].trim();
                        String nome = parts[1].trim() + (parts.length == 3 ? ": " + parts[2].trim() : "");
                        nomes.add(id + " - " + nome);
                    }
                }
            }
            proc.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nomes;
    }

    public boolean attachUsbDevice(String busid) {
        try {
            ProcessBuilder pb = new ProcessBuilder(USBIP_PATH, "attach", "-r", RASPBERRY_IP, "-b", busid);
            pb.inheritIO();
            Process p = pb.start();
            if (p.waitFor() != 0) return false;

            ProcessBuilder portPb = new ProcessBuilder(USBIP_PATH, "port");
            portPb.redirectErrorStream(true);
            Process portProc = portPb.start();
            portProc.waitFor();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean detachUsbDevice(String busid) {
        try {
            ProcessBuilder pb = new ProcessBuilder(USBIP_PATH, "port");
            Process portProc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(portProc.getInputStream()));
            String line;
            portaAssociada = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Port ")) {
                    portaAssociada = line.trim().split(" ")[1].replace(":", "");
                }
            }

            pb = new ProcessBuilder(USBIP_PATH, "detach", "--port", portaAssociada);
            pb.inheritIO();
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getAssociatedPort(String busid) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(USBIP_PATH, "port");
        Process proc = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("Port ")) {
                if (line.contains(busid)) {
                    return line.trim().split(" ")[1].replace(":", "");
                }
            }
        }
        proc.waitFor();
        return null;
    }
}

