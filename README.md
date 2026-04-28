#  POLITEC USB over IP

> Sistema de gerenciamento de dispositivos USB remotos via rede, desenvolvido para a **Gerência de Perícias de Computação - POLITEC**.

---

##  Sobre o Projeto

O **POLITEC USB over IP** permite que dispositivos USB conectados a um **Raspberry Pi** sejam compartilhados e utilizados remotamente por computadores Windows na rede local.

Cada perito pode atribuir um dispositivo USB para si através de uma interface gráfica simples, sem precisar de linha de comando. O sistema controla quem está usando cada dispositivo, evitando conflitos entre usuários.

---

##  Interface

- Login com nome do usuário
- Lista de dispositivos USB disponíveis em tempo real
- Atribuição e liberação de dispositivos com um clique
- Exibe quem está usando cada dispositivo e desde quando
- Atualização automática a cada 10 segundos

---

##  Arquitetura

```
[ Dispositivos USB (dongles, pendrives, tokens) ]
              |
[ Raspberry Pi - Servidor USB/IP + PostgreSQL + WebSocket ]
              |
     [ Rede Local (LAN) ]
              |
   ┌──────────┴──────────┐
[ PC Perito 1 ]   [ PC Perito 2 ]  ...
  Windows App      Windows App
```

---

##  Tecnologias

| Camada | Tecnologia |
|--------|-----------|
| Servidor USB | Raspberry Pi OS + usbip |
| Cliente USB | usbip-win 0.3.6 |
| Interface | Java 8 + Swing |
| Banco de dados | PostgreSQL |
| Comunicação em tempo real | WebSocket (java-websocket) |
| Empacotamento | Launch4j + Inno Setup |

---

##  Configuração do Raspberry Pi (Servidor)

### 1. Atualizar o sistema
```bash
sudo apt update && sudo apt upgrade -y
```

### 2. Instalar e ativar o USB/IP
```bash
sudo apt install usbip -y
sudo modprobe usbip_host
sudo modprobe vhci_hcd
```

### 3. Ativar os módulos automaticamente no boot
```bash
echo "usbip_host" | sudo tee -a /etc/modules
echo "vhci_hcd"   | sudo tee -a /etc/modules
```

### 4. Listar e exportar dispositivos USB
```bash
# Listar dispositivos conectados
usbip list -l

# Exportar o dispositivo desejado
sudo usbip bind -b <busid>

# Iniciar o daemon
sudo usbipd -D
```

### 5. Exportar todos os dispositivos automaticamente no boot
Crie o arquivo `/etc/rc.local` ou um serviço systemd:
```bash
sudo usbip bind -b 1-1.1
sudo usbip bind -b 1-1.2
sudo usbipd -D
```

### 6. PostgreSQL — criar banco e tabelas
```sql
CREATE DATABASE politecdb;

CREATE TABLE configuracoes (
    id   SERIAL PRIMARY KEY,
    nome VARCHAR(100),
    ip   VARCHAR(50)
);

CREATE TABLE uso_usb (
    id          SERIAL PRIMARY KEY,
    busid       VARCHAR(50),
    usuario     VARCHAR(100),
    ip_maquina  VARCHAR(50),
    inicio_uso  TIMESTAMP,
    em_uso      BOOLEAN DEFAULT TRUE
);

-- Índice único para evitar race condition (dois usuários no mesmo dispositivo)
CREATE UNIQUE INDEX uso_usb_busid_ativo_idx
    ON uso_usb (busid)
    WHERE em_uso = TRUE;
```

---

##  Instalação no Windows (Cliente)

1. Execute o instalador `POLITEC_USB_Setup.exe` como **Administrador**
2. O instalador irá:
   - Instalar o aplicativo em `Arquivos de Programas\POLITEC USB`
   - Instalar o driver usbip-win automaticamente
   - Criar atalho na área de trabalho
3. Abra o **POLITEC USB** e informe seu nome
4. Selecione o dispositivo desejado e clique em **Atribuir**

> O aplicativo requer **execução como Administrador** para comunicar com o driver USB.

---

## Estrutura do Projeto

```
POLITEC/
├── src/
│   └── main/java/
│       ├── config/
│       │   └── ConfigManager.java
│       ├── listener/
│       │   └── UsbListener.java
│       ├── service/
│       │   ├── ConfigService.java
│       │   ├── Configuracao.java
│       │   ├── DatabaseConnection.java
│       │   ├── UsbService.java
│       │   ├── UsoUsb.java
│       │   └── UsoUsbService.java
│       ├── ui/
│       │   ├── LeftPanel.java
│       │   ├── LogoLoader.java
│       │   ├── MainFrame.java
│       │   ├── RightPanel.java
│       │   └── TokenPanel.java
│       ├── websocket/
│       │   └── PolitecWebSocketClient.java
│       └── Main.java
├── release/
│   ├── usbip-win-0.3.6-dev/
│   ├── PolitecUsb.exe
│   └── logoPolitec.ico
└── pom.xml
```

---

##  Compilar e Gerar o Executável

### Gerar o JAR
```bash
mvn clean package
```

### Gerar o EXE (Launch4j)
1. Abra o **Launch4j**
2. Carregue a configuração ou configure:
   - **Output:** `release/PolitecUsb.exe`
   - **Jar:** `target/politec-1.0-SNAPSHOT-jar-with-dependencies.jar`
   - **Main class:** `Main`
   - **Min JRE:** `1.8`
3. Clique em **Build ▶**

### Gerar o Instalador (Inno Setup)
1. Abra o script `instalador.iss`
2. Clique em **Build → Compile**
3. O instalador `POLITEC_USB_Setup.exe` será gerado na pasta `dist/`

---

## Solução de Problemas

| Problema | Causa | Solução |
|----------|-------|---------|
| Dispositivo some ao outro usuário liberar | Bug de porta no usbip-win | Atualizado — mapa interno de porta por busid |
| Tela travada ao atribuir | Processo usbip sem timeout | Corrigido — timeout de 30s |
| Dispositivo preso com `?-?` | App fechado sem liberar | Limpeza automática de portas órfãs na inicialização |
| Hub aparece como dispositivo | Chip do hub exposto pelo usbip | Filtro por VID de fabricantes de hub |

---

##  Autor

**Brunno Henrique Damasceno Camargo**  
Gerência de Perícias de Computação — POLITEC  

---

##  Licença

Uso interno — POLITEC. Todos os direitos reservados.