@echo off
set USBIP_PATH= \\caminho da pasta da pasra usbip\usbip.exe
set CMD=%*
powershell -Command "Start-Process '%USBIP_PATH%' -ArgumentList '%CMD%' -Verb RunAs"
