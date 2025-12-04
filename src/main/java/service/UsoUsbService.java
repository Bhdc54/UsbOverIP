package service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsoUsbService {

    // REGISTRA (ou atualiza) uso de um dispositivo
    public void registrarUso(String busid, String usuario, String ipMaquina) {
        String sql =
            "INSERT INTO uso_usb (busid, usuario, ip_maquina, inicio_uso, em_uso) " +
            "VALUES (?, ?, ?, NOW(), TRUE)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, busid);
            stmt.setString(2, usuario);
            stmt.setString(3, ipMaquina);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // MARCA COMO LIBERADO
    public void encerrarUso(String busid) {
        String sql =
            "UPDATE uso_usb SET em_uso = FALSE " +
            "WHERE busid = ? AND em_uso = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, busid);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // BUSCA USO ATIVO POR BUSID (se estiver em uso, retorna; senão, null)
    public UsoUsb buscarUsoAtivo(String busid) {
        String sql =
            "SELECT id, busid, usuario, ip_maquina, inicio_uso " +
            "FROM uso_usb WHERE busid = ? AND em_uso = TRUE " +
            "ORDER BY id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, busid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                UsoUsb uso = new UsoUsb();
                uso.setId(rs.getInt("id"));
                uso.setBusid(rs.getString("busid"));
                uso.setUsuario(rs.getString("usuario"));
                uso.setIpMaquina(rs.getString("ip_maquina"));
                uso.setInicioUso(rs.getTimestamp("inicio_uso"));
                return uso;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // 🔹 NOVO: lista todos os usos ATIVOS (em_uso = TRUE)
    public List<UsoUsb> listarUsosAtivos() {
        List<UsoUsb> lista = new ArrayList<>();

        String sql =
            "SELECT id, busid, usuario, ip_maquina, inicio_uso " +
            "FROM uso_usb WHERE em_uso = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UsoUsb uso = new UsoUsb();
                uso.setId(rs.getInt("id"));
                uso.setBusid(rs.getString("busid"));
                uso.setUsuario(rs.getString("usuario"));
                uso.setIpMaquina(rs.getString("ip_maquina"));
                uso.setInicioUso(rs.getTimestamp("inicio_uso"));
                lista.add(uso);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }
}
