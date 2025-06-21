@echo off
echo --- Limpando compilacoes antigas...
if exist bin rmdir /s /q bin
mkdir bin
echo.

echo --- Compilando todo o projeto...
javac -d bin -encoding UTF-8 -sourcepath src src\quiosque\model\*.java src\quiosque\rmi\*.java src\quiosque\socket\*.java

echo.
if exist "bin\quiosque\rmi\PainelControleRMI.class" (
    echo Compilacao finalizada com SUCESSO!
) else (
    echo ***** FALHA NA COMPILACAO! *****
    echo Verifique as mensagens de erro acima.
)

pause