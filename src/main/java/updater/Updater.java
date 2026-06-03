package updater;

import service.UsoUsbService;
import service.UsoUsb;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import config.AppConfig;

public class Updater {

    private static final String SERVIDOR      = "http://172.20.41.61:9000";
    private static final String URL_VERSION   = SERVIDOR + "/version.txt";
    private static final String URL_INSTALLER = SERVIDOR + "/usb.exe";
    public static final String VERSION_ATUAL = AppConfig.VERSION; // ← atualize a cada release

    private final UsoUsbService usoUsbService = new UsoUsbService();
    private final String ipLocal;

    public Updater(String ipLocal) {
        this.ipLocal = ipLocal;
    }

    // -----------------------------------------------------------------------
    // Ponto de entrada — chame no Main antes de abrir o MainFrame
    // -----------------------------------------------------------------------
    public void verificar() {
        new Thread(() -> {
            try {
                String versaoServidor = buscarVersaoServidor();
                if (versaoServidor == null || versaoServidor.equals(VERSION_ATUAL)) {
                    System.out.println("[Updater] Sem atualizações. Versão atual: " + VERSION_ATUAL);
                    return;
                }

                System.out.println("[Updater] Nova versão disponível: " + versaoServidor);

                List<UsoUsb> emUso = usoUsbService.listarUsosAtivosPorIp(ipLocal);

                if (emUso.isEmpty()) {
                    SwingUtilities.invokeLater(() -> aplicarAtualizacao(versaoServidor));
                } else {
                    SwingUtilities.invokeLater(() -> mostrarAvisoAguardando(versaoServidor));
                    aguardarLiberacaoEAtualizar(versaoServidor);
                }

            } catch (Exception e) {
                System.out.println("[Updater] Erro ao verificar atualização: " + e.getMessage());
            }
        }, "updater-check").start();
    }

    // -----------------------------------------------------------------------
    // Aguarda todos os USBs serem liberados e então atualiza
    // -----------------------------------------------------------------------
    private void aguardarLiberacaoEAtualizar(String versaoServidor) {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10_000);
                    List<UsoUsb> emUso = usoUsbService.listarUsosAtivosPorIp(ipLocal);
                    if (emUso.isEmpty()) {
                        SwingUtilities.invokeLater(() -> aplicarAtualizacao(versaoServidor));
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    System.out.println("[Updater] Erro ao aguardar liberação: " + e.getMessage());
                }
            }
        }, "updater-wait").start();
    }

    // -----------------------------------------------------------------------
    // Baixa o instalador e executa silenciosamente
    // -----------------------------------------------------------------------
    private void aplicarAtualizacao(String versaoServidor) {
        UpdateDialog dialog = new UpdateDialog(versaoServidor);
        dialog.setVisible(true);

        new Thread(() -> {
            try {
                // Salva instalador na pasta temp do sistema
                Path instalador = Paths.get(System.getProperty("java.io.tmpdir"), "politec_update.exe");

                dialog.setStatus("Baixando atualização...", 10);
                baixarArquivo(URL_INSTALLER, instalador.toFile(), dialog);

                dialog.setStatus("Preparando instalação...", 95);
                Thread.sleep(500);

                dialog.setStatus("Instalando... O app será reiniciado.", 100);
                Thread.sleep(800);

                // Roda o instalador silenciosamente e fecha o app atual
                // /VERYSILENT = sem janelas
                // /NORESTART  = não reinicia o Windows
                // /CLOSEAPPLICATIONS = fecha processos que usam arquivos sendo substituídos
                new ProcessBuilder(
                        instalador.toAbsolutePath().toString(),
                        "/VERYSILENT",
                        "/NORESTART",
                        "/CLOSEAPPLICATIONS"
                ).start();

                // Fecha o app atual — o instalador abrirá o novo
                System.exit(0);

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    dialog.dispose();
                    JOptionPane.showMessageDialog(null,
                            "Falha ao atualizar: " + e.getMessage() +
                            "\nO aplicativo continuará com a versão atual.",
                            "Erro na atualização", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "updater-install").start();
    }

    // -----------------------------------------------------------------------
    // Aviso quando há USB em uso
    // -----------------------------------------------------------------------
    private void mostrarAvisoAguardando(String versaoServidor) {
        JDialog aviso = new JDialog((Frame) null, "Atualização Pendente", false);
        aviso.setSize(400, 130);
        aviso.setLocationRelativeTo(null);
        aviso.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        aviso.setAlwaysOnTop(true);

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(new Color(10, 40, 90));
        p.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel ico = new JLabel("⚠");
        ico.setFont(new Font("Segoe UI", Font.PLAIN, 28));
        ico.setForeground(new Color(255, 200, 50));

        JLabel msg = new JLabel("<html>Nova versão <b>" + versaoServidor + "</b> disponível.<br>" +
                "Será instalada automaticamente quando<br>" +
                "você liberar todos os dispositivos USB.</html>");
        msg.setForeground(Color.WHITE);
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        p.add(ico, BorderLayout.WEST);
        p.add(msg, BorderLayout.CENTER);
        aviso.setContentPane(p);
        aviso.setVisible(true);

        // Fecha o aviso quando não tiver mais USB em uso
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10_000);
                    List<UsoUsb> emUso = usoUsbService.listarUsosAtivosPorIp(ipLocal);
                    if (emUso.isEmpty()) {
                        SwingUtilities.invokeLater(aviso::dispose);
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }, "updater-aviso-watch").start();
    }

    // -----------------------------------------------------------------------
    // Download com barra de progresso
    // -----------------------------------------------------------------------
    private void baixarArquivo(String urlStr, File destino, UpdateDialog dialog) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);
        int tamanho = conn.getContentLength();

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(destino)) {

            byte[] buf = new byte[8192];
            int lido, total = 0;
            while ((lido = in.read(buf)) != -1) {
                out.write(buf, 0, lido);
                total += lido;
                if (tamanho > 0) {
                    int pct = 10 + (int) ((total / (double) tamanho) * 80);
                    dialog.setStatus("Baixando... " + (total / 1024) + " KB / "
                            + (tamanho / 1024) + " KB", pct);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Busca versão no servidor
    // -----------------------------------------------------------------------
    private String buscarVersaoServidor() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(URL_VERSION).openConnection();
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                return br.readLine().trim();
            }
        } catch (Exception e) {
            System.out.println("[Updater] Servidor de atualização indisponível.");
            return null;
        }
    }
}