#  POLITEC USB over IP

Sistema para compartilhar dispositivos USB do Raspberry Pi para computadores Windows na rede local, com interface gráfica para gerenciamento de uso por múltiplos usuários.

---

##  Configuração do Raspberry Pi

```bash
sudo apt install usbip -y
sudo modprobe usbip_host
sudo modprobe vhci_hcd
sudo usbip bind -b <busid>
sudo usbipd -D
```

### Banco de dados (PostgreSQL)
```sql
CREATE TABLE uso_usb (
    id SERIAL PRIMARY KEY,
    busid VARCHAR(50),
    usuario VARCHAR(100),
    ip_maquina VARCHAR(50),
    inicio_uso TIMESTAMP,
    em_uso BOOLEAN DEFAULT TRUE
);

CREATE UNIQUE INDEX uso_usb_busid_ativo_idx ON uso_usb (busid) WHERE em_uso = TRUE;

CREATE TABLE configuracoes (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100),
    ip VARCHAR(50)
);
```

---

##  Instalação no Windows

Execute o `POLITEC_USB_Setup.exe` como **Administrador**.

---

##  Compilar

```bash
mvn clean package
```

Depois gere o `.exe` com o **Launch4j** e o instalador com o **Inno Setup**.

---

**Autor:** Brunno Henrique Damasceno Camargo 