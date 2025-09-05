package listener;

import java.util.List;

public interface UsbListener {
    void onNewDevices(List<String> novosDispositivos);
}
