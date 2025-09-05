import javax.swing.SwingUtilities;
import ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Erro ao iniciar o programa: " + e.getMessage(),
                    "Erro",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
