package service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigService {

    // 🔹 Salva ou atualiza uma configuração (verifica se o nome já existe)
    public void salvarOuAtualizarConfiguracao(String nome, String ip) {
        String checkSql = "SELECT id FROM configuracoes WHERE nome = ?";
        String updateSql = "UPDATE configuracoes SET ip = ? WHERE nome = ?";
        String insertSql = "INSERT INTO configuracoes (nome, ip) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            // Verifica se já existe um registro com o mesmo nome
            checkStmt.setString(1, nome);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // ✅ Já existe -> atualiza o IP
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, ip);
                    updateStmt.setString(2, nome);
                    updateStmt.executeUpdate();
                    System.out.println("🔁 Configuração existente atualizada: " + nome + " (" + ip + ")");
                }
            } else {
                // 🆕 Não existe -> cria novo registro
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, nome);
                    insertStmt.setString(2, ip);
                    insertStmt.executeUpdate();
                    System.out.println("✅ Nova configuração salva: " + nome + " (" + ip + ")");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 🔹 Lista todas as configurações
    public List<Configuracao> listarConfiguracoes() {
        List<Configuracao> lista = new ArrayList<>();
        String sql = "SELECT * FROM configuracoes ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Configuracao cfg = new Configuracao(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("ip")
                );
                lista.add(cfg);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    // 🔹 Busca configuração pelo nome
    public Configuracao buscarPorNome(String nome) {
        Configuracao cfg = null;
        String sql = "SELECT * FROM configuracoes WHERE nome = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nome);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                cfg = new Configuracao(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("ip")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return cfg;
    }
   // 🔹 Busca configuração pelo IP (pega o último cadastro para aquele IP)
    public Configuracao buscarPorIp(String ip) {
        Configuracao cfg = null;
        String sql = "SELECT * FROM configuracoes WHERE ip = ? ORDER BY id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ip);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                cfg = new Configuracao(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("ip")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return cfg;
    }
}

 