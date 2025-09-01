// ConfigManager.java
import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class ConfigManager {
    public void saveUserName(String nome) {
        try {
            Properties props = new Properties();
            props.setProperty("usuario", nome);
            props.setProperty("ip", getLocalIp());
            props.setProperty("hora", new SimpleDateFormat("HH:mm:ss").format(new Date()));
            FileOutputStream fos = new FileOutputStream("config.properties");
            props.store(fos, "Configuração do usuário");
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String loadUserName() {
        try {
            Properties props = new Properties();
            File f = new File("config.properties");
            if (f.exists()) {
                FileInputStream fis = new FileInputStream(f);
                props.load(fis);
                fis.close();
                return props.getProperty("usuario");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "desconhecido";
        }
    }
}