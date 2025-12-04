package service;

import java.sql.Timestamp;

public class UsoUsb {
    private int id;
    private String busid;
    private String usuario;
    private String ipMaquina;
    private Timestamp inicioUso;

    // getters e setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBusid() { return busid; }
    public void setBusid(String busid) { this.busid = busid; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getIpMaquina() { return ipMaquina; }
    public void setIpMaquina(String ipMaquina) { this.ipMaquina = ipMaquina; }

    public Timestamp getInicioUso() { return inicioUso; }
    public void setInicioUso(Timestamp inicioUso) { this.inicioUso = inicioUso; }
}
