package service;

import listener.UsbListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsbService {
    private static final String USBIP_PATH   = "usbip-win-0.3.6-dev/usbip.exe";
    private static final String RASPBERRY_IP = "172.20.41.61";
    private static final int    TIMEOUT_SEC  = 30;

    private static final Map<String, String> busidToPort = new HashMap<>();

    private UsbListener listener;
    public void setListener(UsbListener listener) { this.listener = listener; }

    // -----------------------------------------------------------------------
    // Lista dispositivos disponíveis no servidor Raspberry
    // Sem filtro de hub — todos os dispositivos exportados aparecem.
    // -----------------------------------------------------------------------
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
                        String id   = parts[0].trim();
                        String nome = parts[1].trim() + (parts.length == 3 ? ": " + parts[2].trim() : "");
                        nomes.add(id + " - " + nome);
                    }
                }
            }
            proc.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nomes;
    }

    // -----------------------------------------------------------------------
    // Attach — grava busid->porta no mapa interno lendo a saída do processo.
    // Saída típica: "succesfully attached to port 0"
    // -----------------------------------------------------------------------
    public boolean attachUsbDevice(String busid) {
        try {
            ProcessBuilder pb = new ProcessBuilder(USBIP_PATH, "attach", "-r", RASPBERRY_IP, "-b", busid);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            Thread leitor = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String l;
                    while ((l = br.readLine()) != null) {
                        output.append(l).append("\n");
                        System.out.println("[attach] " + l);
                    }
                } catch (Exception ignored) {}
            });
            leitor.setDaemon(true);
            leitor.start();

            boolean terminou = p.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!terminou) {
                p.destroyForcibly();
                System.out.println("[UsbService] attach timeout para busid=" + busid);
                return false;
            }
            leitor.join(2000);

            if (p.exitValue() == 0) {
                Set<String> portasAbertas = listarTodasPortasAbertas();

                String saida = output.toString();
                Pattern pat = Pattern.compile("attached to port\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
                Matcher mat = pat.matcher(saida);
                if (mat.find()) {
                    String porta = String.format("%02d", Integer.parseInt(mat.group(1)));
                    busidToPort.put(busid, porta);
                    System.out.println("[UsbService] busid=" + busid + " -> porta=" + porta);
                } else {
                    String portaNova = portasAbertas.isEmpty() ? null
                            : portasAbertas.stream()
                                    .filter(porta -> !busidToPort.containsValue(porta))
                                    .findFirst()
                                    .orElse(null);
                    if (portaNova != null) {
                        busidToPort.put(busid, portaNova);
                        System.out.println("[UsbService] busid=" + busid + " -> porta=" + portaNova + " (fallback diff)");
                    } else {
                        System.out.println("[UsbService] AVISO: nao foi possivel mapear porta para busid=" + busid);
                    }
                }
                return true;
            }
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Detach — usa o mapa interno busid->porta.
    // -----------------------------------------------------------------------
    public boolean detachUsbDevice(String busid) {
        String porta = busidToPort.get(busid);

        if (porta == null) {
            System.out.println("[UsbService] busid=" + busid + " nao esta no mapa local. Tentando por porta orfa.");
            List<String> orfas = listarPortasOrfas();
            if (orfas.size() == 1) {
                porta = orfas.get(0);
                System.out.println("[UsbService] Usando porta orfa " + porta + " para detach de " + busid);
            } else if (orfas.isEmpty()) {
                System.out.println("[UsbService] Nenhuma porta encontrada para " + busid + " — pode ja estar solto.");
                busidToPort.remove(busid);
                return true;
            } else {
                System.out.println("[UsbService] Multiplas portas orfas, nao e possivel determinar qual e " + busid);
                return false;
            }
        }

        try {
            System.out.println("[UsbService] Detach busid=" + busid + " porta=" + porta);
            ProcessBuilder pb = new ProcessBuilder(USBIP_PATH, "detach", "--port", porta);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            Thread leitor = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String l;
                    while ((l = br.readLine()) != null) System.out.println("[detach] " + l);
                } catch (Exception ignored) {}
            });
            leitor.setDaemon(true);
            leitor.start();

            boolean terminou = p.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!terminou) {
                p.destroyForcibly();
                System.out.println("[UsbService] detach timeout para busid=" + busid);
                return false;
            }
            leitor.join(2000);

            busidToPort.remove(busid);
            return p.exitValue() == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Retorna lista de números de porta com estado corrompido "?-?"
    // -----------------------------------------------------------------------
    public List<String> listarPortasOrfas() {
        List<String> orfas = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(USBIP_PATH, "port");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            String currentPort = null;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("Port ")) {
                    String[] parts = trimmed.split("[\\s:]+");
                    if (parts.length >= 2) {
                        try {
                            currentPort = String.format("%02d", Integer.parseInt(parts[1].replace(":", "")));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                if (currentPort != null && trimmed.startsWith("?-?")) {
                    orfas.add(currentPort);
                }
            }
            proc.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orfas;
    }

    // -----------------------------------------------------------------------
    // Lista TODAS as portas abertas atualmente
    // -----------------------------------------------------------------------
    private Set<String> listarTodasPortasAbertas() {
        Set<String> portas = new HashSet<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(USBIP_PATH, "port");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("Port ")) {
                    String[] parts = trimmed.split("[\\s:]+");
                    if (parts.length >= 2) {
                        try {
                            portas.add(String.format("%02d", Integer.parseInt(parts[1].replace(":", ""))));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            proc.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return portas;
    }

    // -----------------------------------------------------------------------
    // Limpa portas corrompidas ?-? na inicialização do app
    // -----------------------------------------------------------------------
    public void detachAllOrphanPorts() {
        try {
            List<String> orfas = listarPortasOrfas();
            if (orfas.isEmpty()) {
                System.out.println("[UsbService] Nenhuma porta orfa encontrada.");
                return;
            }
            for (String porta : orfas) {
                System.out.println("[UsbService] Limpando porta orfa: " + porta);
                ProcessBuilder pb = new ProcessBuilder(USBIP_PATH, "detach", "--port", porta);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                new BufferedReader(new InputStreamReader(p.getInputStream()))
                        .lines().forEach(l -> System.out.println("[detach-orfa] " + l));
                boolean terminou = p.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
                if (!terminou) p.destroyForcibly();
            }
            System.out.println("[UsbService] Portas orfas limpas.");
        } catch (Exception e) {
            System.out.println("[UsbService] Erro ao limpar portas orfas");
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------
    // Desanexa todos os dispositivos desta máquina (shutdown)
    // -----------------------------------------------------------------------
    public void detachAllDevices() {
        List<String> busids = new ArrayList<>(busidToPort.keySet());
        for (String busid : busids) {
            System.out.println("[UsbService] detachAll: " + busid);
            detachUsbDevice(busid);
        }
        detachAllOrphanPorts();
    }

    // -----------------------------------------------------------------------
    // Retorna busids mapeados localmente
    // -----------------------------------------------------------------------
    public Set<String> listarBusidsAnexados() {
        return new HashSet<>(busidToPort.keySet());
    }

    // -----------------------------------------------------------------------
    // Re-exporta um dispositivo no Raspberry (unbind/bind)
    // -----------------------------------------------------------------------
    public void reexportUsb(String busid) {
        try {
            new ProcessBuilder("ssh", "pi@" + RASPBERRY_IP, "sudo usbip unbind -b " + busid)
                    .start().waitFor();
            new ProcessBuilder("ssh", "pi@" + RASPBERRY_IP, "sudo usbip bind -b " + busid)
                    .start().waitFor();
            System.out.println("[UsbService] USB reexportado: " + busid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}