@echo off
cd /d "%~dp0"

echo "== Sistema de Quiosque (RMI) =="

set CLASSPATH=./bin;./resources

echo "Classpath definido como: %CLASSPATH%"
echo "Iniciando o Painel de Controle (Servidor RMI)..."


start "Painel de Controle RMI" javaw -cp "%CLASSPATH%" quiosque.rmi.PainelControleRMI

echo "Servidor RMI iniciado em uma nova janela."
