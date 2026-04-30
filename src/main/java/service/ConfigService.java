package service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigService {

    // Salva ou atualiza configuração buscando pelo IP (não pelo nome).
    // Lógica correta:
    //   - Se já existe um registro para esse IP → atualiza o nome
    //   - Se não existe → cria novo registro
    // Assim cada máquina tem exatamente um registro, identificado pelo IP.
    public void salvarOuAtualizarConfiguracao(String nome, String ip) {
        String checkSql  = "SELECT id FROM configuracoes WHERE ip = ?";
        String updateSql = "UPDATE configuracoes SET nome = ? WHERE ip = ?";
        String insertSql = "INSERT INTO configuracoes (nome, ip) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setString(1, ip);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // IP já existe → atualiza apenas o nome
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, nome);
                    updateStmt.setString(2, ip);
                    updateStmt.executeUpdate();
                    System.out.println("🔁 Nome atualizado para IP " + ip + ": " + nome);
                }
            } else {
                // IP novo → cria registro
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, nome);
                    insertStmt.setString(2, ip);
                    insertStmt.executeUpdate();
                    System.out.println("✅ Nova configuração: " + nome + " (" + ip + ")");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Lista todas as configurações
    public List<Configuracao> listarConfiguracoes() {
        List<Configuracao> lista = new ArrayList<>();
        String sql = "SELECT * FROM configuracoes ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(new Configuracao(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("ip")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // Busca pelo nome
    public Configuracao buscarPorNome(String nome) {
        String sql = "SELECT * FROM configuracoes WHERE nome = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nome);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Configuracao(rs.getInt("id"), rs.getString("nome"), rs.getString("ip"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Busca pelo IP — retorna o registro desta máquina
    public Configuracao buscarPorIp(String ip) {
        String sql = "SELECT * FROM configuracoes WHERE ip = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ip);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Configuracao(rs.getInt("id"), rs.getString("nome"), rs.getString("ip"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}