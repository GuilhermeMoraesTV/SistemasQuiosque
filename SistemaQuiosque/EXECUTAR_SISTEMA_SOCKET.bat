@echo off
echo Iniciando Sistema Socket...

REM Define o classpath para incluir o driver e as classes compiladas
set CLASSPATH=./bin;./resources;./lib/mssql-jdbc-12.10.1.jre11.jar

REM Executa o Painel de Controle usando o classpath
javaw -cp "%CLASSPATH%" quiosque.socket.PainelControleSocket