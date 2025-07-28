@echo off
echo --- Limpando compilacoes antigas...
if exist bin rmdir /s /q bin
mkdir bin
echo.

echo --- Compilando todo o projeto...
REM Define o classpath para incluir o driver do SQL Server
set CLASSPATH=./lib/mssql-jdbc-12.6.1.jar

REM --- MODIFICAÇÃO PRINCIPAL ---
REM Cria um arquivo temporário com a lista de todos os .java a serem compilados.
REM Este método é mais robusto que usar *.java diretamente no comando.
dir /s /b src\*.java > sources.txt

REM Compila o projeto usando a lista de arquivos do sources.txt
javac -d bin -cp "%CLASSPATH%" -encoding UTF-8 @sources.txt

REM Deleta o arquivo temporário
del sources.txt
REM --- FIM DA MODIFICAÇÃO ---

echo.
if exist "bin\quiosque\rmi\PainelControleRMI.class" (
    echo Compilacao finalizada com SUCESSO!
) else (
    echo ***** FALHA NA COMPILACAO! *****
    echo Verifique as mensagens de erro acima.
)

pause