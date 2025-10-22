package ui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class LogoLoader {

    public static ImageIcon loadLogo(int width, int height) {
        URL location = LogoLoader.class.getResource("/logoPolitec.png");
        
        if (location == null) {
            System.out.println("Logo não encontrada!");
            return null;
        }

        ImageIcon icon = new ImageIcon(location);
        if (width > 0 && height > 0) {
            Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            icon = new ImageIcon(img);
        }
        return icon;
    }
}
