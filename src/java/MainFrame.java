import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private String usuarioApp;
    private JTextField nomeField;
    private JButton okButton;
    private JPanel mainPanel;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private ArrayList<TokenPanel> usbPanels = new ArrayList<>();
    private UsbService usbService;
    private ConfigManager configManager;

    public MainFrame() {
        usbService = new UsbService();
        configManager = new ConfigManager();
        usuarioApp = configManager.loadUserName();

        setTitle("POLITEC - Gerência de Perícias de Computação");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        // Painel principal
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(15, 32, 60));

        // Painel esquerdo (nome usuário)
        leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(new Color(15, 32, 60));
        leftPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("POLITEC");
        title.setFont(new Font("Segoe UI", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel infoLabel = new JLabel("Gerência de Perícias de Computação");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        leftPanel.add(title);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(infoLabel);
        leftPanel.add(Box.createVerticalStrut(40));

        // Label e campo centralizado
        JLabel nomeLabel = new JLabel("Informe seu nome:");
        nomeLabel.setForeground(Color.WHITE);
        nomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        nomeField = new JTextField();
        nomeField.setMaximumSize(new Dimension(200, 30));
        nomeField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        nomeField.setText(usuarioApp != null ? usuarioApp : "");
        nomeField.setAlignmentX(Component.CENTER_ALIGNMENT);

        leftPanel.add(nomeLabel);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(nomeField);

        okButton = new JButton("OK");
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        okButton.setBackground(new Color(76, 175, 80));
        okButton.setForeground(Color.WHITE);
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(okButton);

        // Botão de atualização
        JButton refreshButton = new JButton(" 🔄 Atualizar");
        refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        refreshButton.setBackground(new Color(33, 150, 243));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(refreshButton);

        mainPanel.add(leftPanel, BorderLayout.WEST);

        // Painel direito (USBs)
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(22, 40, 80));
        rightPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.add(new JScrollPane(rightPanel), BorderLayout.CENTER);

        // Botão OK salva usuário
        okButton.addActionListener(e -> {
            String nome = nomeField.getText().trim();
            if (!nome.isEmpty()) {
                usuarioApp = nome;
                configManager.saveUserName(usuarioApp);
            } else {
                JOptionPane.showMessageDialog(this, "Digite seu nome.", "Atenção", JOptionPane.WARNING_MESSAGE);
            }
        });

        // Botão Atualizar recarrega lista de USBs
        refreshButton.addActionListener(e -> atualizarUSBs());

        setContentPane(mainPanel);

        // Atualiza lista de USBs automaticamente ao abrir
        atualizarUSBs();
    }

    // Método para atualizar USBs com tentativas
    private void atualizarUSBs() {
        new Thread(() -> {
            List<String> devices = new ArrayList<>();
            int tentativas = 0;

            while (devices.isEmpty() && tentativas < 3) {
                devices = usbService.listUsbDevices();
                tentativas++;
                if (devices.isEmpty()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                }
            }

            final List<String> devicesFinal = devices;
            SwingUtilities.invokeLater(() -> atualizarPainelUSBs(devicesFinal));
        }).start();
    }

    private void atualizarPainelUSBs(List<String> usbNames) {
        rightPanel.removeAll();
        usbPanels.clear();

        for (String nome : usbNames) {
            TokenPanel token = new TokenPanel(nome, usuarioApp, usbService);
            usbPanels.add(token);
            rightPanel.add(token);
            rightPanel.add(Box.createVerticalStrut(10));
        }

        rightPanel.revalidate();
        rightPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
