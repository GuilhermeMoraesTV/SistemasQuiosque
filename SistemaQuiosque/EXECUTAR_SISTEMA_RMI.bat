@echo off
cd /d "%~dp0"

echo "== Sistema de Quiosque (RMI) =="

REM Define o classpath para incluir o driver e as classes compiladas
set CLASSPATH=./bin;./resources;./lib/mssql-jdbc-12.10.1.jre11.jar

echo "Classpath definido como: %CLASSPATH%"
echo "Iniciando o Painel de Controle (Servidor RMI)..."

REM --- MUDANÇAS PRINCIPAIS AQUI ---
REM Trocamos 'javaw' por 'java' para forçar a exibição do console.
java -Djava.rmi.server.hostname=127.0.0.1 -cp "%CLASSPATH%" quiosque.rmi.PainelControleRMI

REM Adicionamos um 'pause' para que a janela não feche se houver um erro.
pause