@echo off
rem Compila y abre la version grafica del programa
cd /d "%~dp0"
javac -encoding UTF-8 -d out src\*.java || pause
start "" javaw -cp out VentanaMetrolinea
