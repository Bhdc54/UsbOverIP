@echo off
set USBIP_PATH=C:\Users\03357343169\Downloads\usbip-win-0.3.6-dev\usbip.exe
set CMD=%*
powershell -Command "Start-Process '%USBIP_PATH%' -ArgumentList '%CMD%' -Verb RunAs"
