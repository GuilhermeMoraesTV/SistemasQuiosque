@echo off
echo ========================================================
echo    FINALIZADOR DE PROCESSOS JAVA E RMI
echo ========================================================
echo.
echo Tentando fechar todas as janelas e processos...
echo.

taskkill /F /IM java.exe /IM javaw.exe /IM rmiregistry.exe

echo.
echo Processo de finalizacao concluido.
pause