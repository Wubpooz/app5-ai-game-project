@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0.."

set PORT=1234
set GAMES=1
set PLAYER_CLASS=game.EscampeAIPlayer
set OPPONENT_CLASS=game.EscampeAIPlayer

if "%1"=="compile" goto compile
if "%1"=="server" goto server
if "%1"=="client-random" goto client-random
if "%1"=="client-human" goto client-human
if "%1"=="client-custom" goto client-custom-cli
if "%1"=="solo" goto solo-cli
if "%1"=="match-custom-random" goto match-custom-random-cli
if "%1"=="match-custom-human" goto match-custom-human-cli
if "%1"=="match-custom-custom" goto match-custom-custom-cli
if "%1"=="match-random-human" goto match-random-human-cli
if "%1"=="match-random-random" goto match-random-random-cli

:interactive
cls
echo ====================================================================
echo                      ESCAMPE GAME LAUNCH MANAGER        
echo ====================================================================
echo  1. Compile Project
echo  2. Start Game Server (Port %PORT%, %GAMES% Game(s))
echo  3. Connect Random Player Client
echo  4. Connect Human Player Client
echo  5. Connect Custom Local Player Client (%PLAYER_CLASS%)
echo  6. [MATCH] Custom Local Player vs Random Player
echo  7. [MATCH] Custom Local Player vs Human Player
echo  8. [MATCH] Custom Local Player vs Custom Local Player
echo  9. [MATCH] Random Player vs Human Player (Jar Only - Demo)
echo 10. [MATCH] Random Player vs Random Player (Jar Only - Demo)
echo 11. [SOLO] Local Solo Game (Custom vs Custom - No Network Server)
echo 12. Exit
echo ====================================================================
set /p choice="Select an option [1-12]: "

if "%choice%"=="1" goto compile_menu
if "%choice%"=="2" goto server
if "%choice%"=="3" goto client-random
if "%choice%"=="4" goto client-human
if "%choice%"=="5" goto client-custom
if "%choice%"=="6" goto match-custom-random
if "%choice%"=="7" goto match-custom-human
if "%choice%"=="8" goto match-custom-custom
if "%choice%"=="9" goto match-random-human
if "%choice%"=="10" goto match-random-random
if "%choice%"=="11" goto solo
if "%choice%"=="12" exit /b 0

echo Invalid choice!
pause
goto interactive

:compile_menu
call :compile_func
pause
goto interactive

:compile_func
echo Compiling Java classes...
cd escampe
call gradlew.bat compileJava
set ERR=%ERRORLEVEL%
cd ..
if %ERR% neq 0 (
    echo Compilation failed with code %ERR%!
) else (
    echo Compilation successful!
)
exit /b %ERR%

:compile
call :compile_func
exit /b %ERRORLEVEL%

:server
echo Starting Game Server...
start cmd /c "title Escampe Server && java -cp libraries/escampeobf.jar escampe.ServeurJeu %PORT% %GAMES% || pause"
if "%1"=="" (
    echo Server launched in a separate window.
    pause
    goto interactive
)
exit /b 0

:client-random
echo Starting Random Player Client...
start cmd /c "title Random Player && java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurAleatoire localhost %PORT% || pause"
if "%1"=="" (
    echo Random client launched in a separate window.
    pause
    goto interactive
)
exit /b 0

:client-human
echo Starting Human Player Client...
start cmd /c "title Human Player && java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurHumain localhost %PORT% || pause"
if "%1"=="" (
    echo Human client launched in a separate window.
    pause
    goto interactive
)
exit /b 0

:client-custom
call :compile_func
if %ERRORLEVEL% neq 0 goto interactive
:client-custom-cli
echo Starting Custom Player Client...
start cmd /c "title Custom Local Player && java -cp "escampe/build/classes/java/main;libraries/escampeobf.jar" io.ClientJeu %PLAYER_CLASS% localhost %PORT% || pause"
if "%1"=="" (
    echo Custom client launched in a separate window.
    pause
    goto interactive
)
exit /b 0

:solo
call :compile_func
if %ERRORLEVEL% neq 0 goto interactive
:solo-cli
echo Starting Solo Local Game...
start cmd /c "title Solo Local Game && java -cp "escampe/build/classes/java/main;libraries/escampeobf.jar" io.Solo %PLAYER_CLASS% %OPPONENT_CLASS% dummy1 dummy2 || pause"
if "%1"=="" (
    echo Solo game launched in a separate window.
    pause
    goto interactive
)
exit /b 0

:match-custom-random
call :compile_func
if %ERRORLEVEL% neq 0 goto interactive
:match-custom-random-cli
echo Launching Match (Custom vs Random)...
start cmd /c "title Escampe Server && java -cp libraries/escampeobf.jar escampe.ServeurJeu %PORT% %GAMES% || pause"
timeout /t 2 >nul
start cmd /c "title Random Player && java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurAleatoire localhost %PORT% || pause"
timeout /t 1 >nul
start cmd /c "title Custom Local Player && java -cp "escampe/build/classes/java/main;libraries/escampeobf.jar" io.ClientJeu %PLAYER_CLASS% localhost %PORT% || pause"
if "%1"=="" (
    echo Match launched in separate windows!
    pause
    goto interactive
)
exit /b 0

:match-custom-human
call :compile_func
if %ERRORLEVEL% neq 0 goto interactive
:match-custom-human-cli
echo Launching Match (Custom vs Human)...
start cmd /c "title Escampe Server && java -cp libraries/escampeobf.jar escampe.ServeurJeu %PORT% %GAMES% || pause"
timeout /t 2 >nul
start cmd /c "title Human Player && java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurHumain localhost %PORT% || pause"
timeout /t 1 >nul
start cmd /c "title Custom Local Player && java -cp "escampe/build/classes/java/main;libraries/escampeobf.jar" io.ClientJeu %PLAYER_CLASS% localhost %PORT% || pause"
if "%1"=="" (
    echo Match launched in separate windows!
    pause
    goto interactive
)
exit /b 0

:match-custom-custom
call :compile_func
if %ERRORLEVEL% neq 0 goto interactive
:match-custom-custom-cli
echo Launching Match (Custom vs Custom)...
start cmd /c "title Escampe Server && java -cp libraries/escampeobf.jar escampe.ServeurJeu %PORT% %GAMES% || pause"
timeout /t 2 >nul
start cmd /c "title Custom Player 1 && java -cp "escampe/build/classes/java/main;libraries/escampeobf.jar" io.ClientJeu %PLAYER_CLASS% localhost %PORT% || pause"
timeout /t 1 >nul
start cmd /c "title Custom Player 2 && java -cp "escampe/build/classes/java/main;libraries/escampeobf.jar" io.ClientJeu %OPPONENT_CLASS% localhost %PORT% || pause"
if "%1"=="" (
    echo Match launched in separate windows!
    pause
    goto interactive
)
exit /b 0

:match-random-human
:match-random-human-cli
echo Launching Match (Random vs Human)...
start cmd /c "title Escampe Server && java -cp libraries/escampeobf.jar escampe.ServeurJeu %PORT% %GAMES% || pause"
timeout /t 2 >nul
start cmd /c "title Random Player && java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurAleatoire localhost %PORT% || pause"
timeout /t 1 >nul
start cmd /c "title Human Player && java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurHumain localhost %PORT% || pause"
if "%1"=="" (
    echo Match launched in separate windows!
    pause
    goto interactive
)
exit /b 0

:match-random-random
:match-random-random-cli
echo Launching Match (Random vs Random)...
start cmd /c "title Escampe Server && java -cp libraries/escampeobf.jar escampe.ServeurJeu %PORT% %GAMES% || pause"
timeout /t 2 >nul
start cmd /c "title Random Player 1 && java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurAleatoire localhost %PORT% || pause"
timeout /t 1 >nul
start cmd /c "title Random Player 2 && java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurAleatoire localhost %PORT% || pause"
if "%1"=="" (
    echo Match launched in separate windows!
    pause
    goto interactive
)
exit /b 0
