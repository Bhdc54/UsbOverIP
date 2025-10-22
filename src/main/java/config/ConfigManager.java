package config;

import service.ConfigService;
import service.Configuracao;

public class ConfigManager {
    private final ConfigService configService;

    public ConfigManager() {
        this.configService = new ConfigService();
    }

    // 🔹 Salva ou atualiza automaticamente o nome e IP no banco
    public void saveUserName(String nome, String ip) {
        try {
            configService.salvarOuAtualizarConfiguracao(nome, ip);
        } catch (Exception e) {
            System.err.println("❌ Erro ao salvar configuração: " + e.getMessage());
        }
    }

    // 🔹 (Opcional) Carrega o último usuário desse IP, se quiser usar depois
    public String loadUserName(String ip) {
        try {
            Configuracao cfg = configService.buscarPorNome(ip); // pode ajustar depois se quiser buscar por IP
            return (cfg != null) ? cfg.getNome() : null;
        } catch (Exception e) {
            System.err.println("❌ Erro ao carregar usuário do banco: " + e.getMessage());
            return null;
        }
    }
}
