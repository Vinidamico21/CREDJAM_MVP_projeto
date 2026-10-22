@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "CHECK_ONLY="
if /I "%~1"=="--check" set "CHECK_ONLY=1"

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
cd /d "%PROJECT_ROOT%"

where java >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Java nao encontrado no PATH. Instale o Java 17 ou superior.
    echo        Download: https://adoptium.net/
    pause
    exit /b 1
)

rem Define o comando de build.
rem 1) Se o Maven ja estiver instalado no sistema, usa ele (mais rapido, sem download).
rem 2) Caso contrario, usa o Maven Wrapper (mvnw) do projeto, que baixa o Maven
rem    automaticamente - assim o usuario precisa apenas do Java.
set "MVN_CMD="
where mvn >nul 2>&1
if not errorlevel 1 (
    set "MVN_CMD=call mvn"
) else (
    if exist "%PROJECT_ROOT%\mvnw.cmd" set "MVN_CMD=call "%PROJECT_ROOT%\mvnw.cmd""
)

call :find_jar
if not defined JAR_FILE (
    if defined CHECK_ONLY (
        echo [INFO] Nenhum JAR encontrado em target\.
        if not defined MVN_CMD (
            echo [ERRO] Nem o Maven Wrapper ^(mvnw^) nem o Maven ^(mvn^) estao disponiveis para gerar o JAR.
            exit /b 1
        )
        echo [OK] Ferramenta de build disponivel.
        exit /b 0
    )

    if not defined MVN_CMD (
        echo [ERRO] Nem o Maven Wrapper ^(mvnw^) nem o Maven ^(mvn^) estao disponiveis para gerar o JAR.
        pause
        exit /b 1
    )

    echo [INFO] JAR nao encontrado. Compilando o projeto ^(primeira execucao pode demorar e requer internet^)...
    %MVN_CMD% -DskipTests package
    if errorlevel 1 (
        echo [ERRO] Falha no build. Verifique a conexao com a internet e tente novamente.
        pause
        exit /b 1
    )

    call :find_jar
    if not defined JAR_FILE (
        echo [ERRO] JAR principal nao encontrado em target\ apos o build.
        pause
        exit /b 1
    )
)

if defined CHECK_ONLY (
    echo [OK] Launcher validado.
    echo [OK] JAR detectado: %JAR_FILE%
    exit /b 0
)

set "APP_URL=http://localhost:8080"

echo [INFO] Iniciando CREDJAM...
echo [INFO] URL: %APP_URL%
echo [INFO] O navegador sera aberto automaticamente em %APP_URL% assim que a aplicacao iniciar.

rem Abre o navegador em paralelo apos um atraso, aguardando a aplicacao subir.
rem A auto-abertura interna da aplicacao fica desligada para evitar abas duplicadas.
start "" cmd /c "timeout /t 12 /nobreak >nul & start "" "%APP_URL%""

java -jar "%JAR_FILE%" --app.auto-open-browser=false
set "EXIT_CODE=%ERRORLEVEL%"

echo [INFO] Aplicacao finalizada com codigo %EXIT_CODE%.
exit /b %EXIT_CODE%

:find_jar
set "JAR_FILE="
for /f "delims=" %%F in ('dir /b /a:-d /o:-d "target\*.jar" 2^>nul') do (
    echo %%F | findstr /I /C:".original" >nul
    if errorlevel 1 (
        set "JAR_FILE=%PROJECT_ROOT%\target\%%F"
        goto :eof
    )
)
goto :eof
