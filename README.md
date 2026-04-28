📌 USB Over IP - Raspberry Pi <-> Windows
Descrição

Este projeto permite compartilhar dispositivos USB conectados a um Raspberry Pi para um computador com Windows, utilizando o protocolo USB/IP.
Ele possibilita que o dispositivo USB seja acessado remotamente como se estivesse conectado diretamente no PC.

Tecnologias Utilizadas

Raspberry Pi OS (Servidor USB)

Windows 10/11 (Cliente USB)

USB/IP (protocolo de compartilhamento USB via rede)

Python/Java (opcional, para integração com apps que usam os dispositivos USB)

https://github.com/cezanne/usbip-win/tags

Funcionalidades

Exportar dispositivos USB do Raspberry Pi para Windows

Conectar remotamente dispositivos USB ao PC

Permite automação de detecção e conexão (via scripts)

Compatível com diversos tipos de dispositivos USB (pendrives, dongles, câmeras, etc.)

Arquitetura
[ Dispositivo USB ] 
        |
        
[ Raspberry Pi (Servidor USB) ] 
        |
         
[ Windows (Cliente USB) ]

Como Usar
No Raspberry Pi (Servidor)

Atualizar pacotes:

sudo apt update && sudo apt upgrade -y


Instalar USB/IP:

sudo apt install usbip -y


Ativar os drivers:

sudo modprobe usbip_host
sudo modprobe vhci_hcd


Listar dispositivos USB:

usbip list -l


Exportar o dispositivo desejado:

sudo usbip bind -b <busid>


Iniciar o daemon:

sudo usbipd -D

No Windows (Cliente)

Instalar USB/IP para Windows (ex.: usbip-win)

Listar dispositivos disponíveis no Raspberry:

usbip.exe list -r <IP_DO_RASPBERRY>


Conectar ao dispositivo:

usbip.exe attach -r <IP_DO_RASPBERRY> -b <busid>


Após isso, o dispositivo USB aparecerá no Gerenciador de Dispositivos do Windows.

Autor

Brunno Camargo
