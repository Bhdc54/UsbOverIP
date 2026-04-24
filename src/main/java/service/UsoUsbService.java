package service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsoUsbService {

    // -----------------------------------------------------------------------
    // REGISTRA uso de um dispositivo.
    // BUG CORRIGIDO: usa INSERT ... ON CONFLICT DO NOTHING para evitar que
    // dois clientes registrem o mesmo busid ao mesmo tempo (race condition).
    // Requer UNIQUE constraint em uso_usb(busid) WHERE em_uso = TRUE.
    //
    // SQL para criar no banco (execute uma vez):
    //   CREATE UNIQUE INDEX IF NOT EXISTS uso_usb_busid_ativo_idx
    //       ON uso_usb (busid)
    //       WHERE em_uso = TRUE;
    // -----------------------------------------------------------------------
    public boolean registrarUso(String busid, String usuario, String ipMaquina) {
        String sql =
            "INSERT INTO uso_usb (busid, usuario, ip_maquina, inicio_uso, em_uso) " +
            "VALUES (?, ?, ?, NOW(), TRUE) " +
            "ON CONFLICT DO NOTHING";          // garante que só UM registro ativo por busid

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, busid);
            stmt.setString(2, usuario);
            stmt.setString(3, ipMaquina);
            int rows = stmt.executeUpdate();

            if (rows == 0) {
                // Outra máquina inseriu antes (conflito de índice único)
                System.out.println("[UsoUsbService] Conflito: busid=" + busid
                        + " já está em uso por outro usuário (race condition evitada).");
                return false;
            }
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // ENCERRA uso pelo busid
    // -----------------------------------------------------------------------
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

    // -----------------------------------------------------------------------
    // BUSCA uso ativo por busid
    // -----------------------------------------------------------------------
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

    // -----------------------------------------------------------------------
    // LISTA todos os usos ativos
    // -----------------------------------------------------------------------
    public List<UsoUsb> listarUsosAtivos() {
        List<UsoUsb> lista = new ArrayList<>();
        String sql =
            "SELECT id, busid, usuario, ip_maquina, inicio_uso " +
            "FROM uso_usb WHERE em_uso = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

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

    // -----------------------------------------------------------------------
    // LISTA usos ativos desta máquina (por IP) — usado no shutdown
    // -----------------------------------------------------------------------
    public List<UsoUsb> listarUsosAtivosPorIp(String ipMaquina) {
        List<UsoUsb> lista = new ArrayList<>();
        String sql =
            "SELECT id, busid, usuario, ip_maquina, inicio_uso " +
            "FROM uso_usb WHERE em_uso = TRUE AND ip_maquina = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ipMaquina);
            ResultSet rs = stmt.executeQuery();

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