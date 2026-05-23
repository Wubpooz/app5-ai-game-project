#!/usr/bin/env bash
# Bash Script to Launch Escampe Game Server and Clients
# Usage: ./scripts/launch.sh [mode] [port] [games] [player_class] [opponent_class]

set -euo pipefail

MODE="${1:-interactive}"
PORT="${2:-1234}"
GAMES="${3:-1}"
PLAYER_CLASS="${4:-game.AIPlayer}"
OPPONENT_CLASS="${5:-game.AIPlayer}"

# Resolve directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ESCAMPE_DIR="$ROOT_DIR/escampe"

# Detect OS and Terminal capability
OS_TYPE="$(uname -s)"
IS_WINDOWS=false
if [[ "$OS_TYPE" == *"MINGW"* || "$OS_TYPE" == *"MSYS"* || "$OS_TYPE" == *"CYGWIN"* ]]; then
    IS_WINDOWS=true
fi

compile_project() {
    echo "Compiling Java classes..."
    cd "$ESCAMPE_DIR"
    if [ "$IS_WINDOWS" = true ]; then
        ./gradlew.bat compileJava
    else
        ./gradlew compileJava
    fi
    cd "$ROOT_DIR"
    echo "Compilation successful!"
}

# Run command in a new terminal window or fallback to background process
run_in_terminal() {
    local title="$1"
    local cmd="$2"
    local color_cmd="echo -e \"\\033[1;33m=== $title ===\\033[0m\"; $cmd"

    if [ "$IS_WINDOWS" = true ]; then
        # On Git Bash, use cmd start to open a new PowerShell window with auto-close on success
        cmd //c start powershell -Command "cd '$ROOT_DIR'; Write-Host '=== $title ===' -ForegroundColor Yellow; $cmd; if (\$LASTEXITCODE -ne 0) { Write-Host 'Press Enter to exit...' -ForegroundColor Red; [void][System.Console]::ReadLine() }"
    elif [[ "$OS_TYPE" == "Darwin" ]]; then
        # On macOS, use AppleScript to launch a new Terminal window and pause on error
        osascript -e "tell application \"Terminal\" to do script \"cd '$ROOT_DIR' && $cmd || read -p 'Press Enter to exit...'\""
    elif command -v gnome-terminal &>/dev/null; then
        gnome-terminal --title="$title" -- bash -c "cd '$ROOT_DIR' && $color_cmd || { read -p 'Press Enter to exit...'; }"
    elif command -v xterm &>/dev/null; then
        xterm -T "$title" -e "bash -c \"cd '$ROOT_DIR' && $color_cmd || { read -p 'Press Enter to exit...'; }\"" &
    else
        # Fallback: run in background and log to a file
        local logfile="$ROOT_DIR/$(echo "$title" | tr ' ' '_').log"
        echo "No window manager found. Running in background. Logs: $logfile"
        cd "$ROOT_DIR"
        bash -c "$cmd" &> "$logfile" &
    fi
}

start_server() {
    run_in_terminal "Escampe Server" "java -cp libraries/escampeobf.jar escampe.ServeurJeu $PORT $GAMES"
}

start_random_client() {
    run_in_terminal "Random Player (Jar)" "java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurAleatoire localhost $PORT"
}

start_human_client() {
    run_in_terminal "Human Player (Jar)" "java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurHumain localhost $PORT"
}

start_custom_client() {
    local pclass="$1"
    # Use colon on Unix/macOS, semicolon on Windows classpath
    local cp="escampe/build/classes/java/main:libraries/escampeobf.jar"
    if [ "$IS_WINDOWS" = true ]; then
        cp="escampe/build/classes/java/main;libraries/escampeobf.jar"
    fi
    run_in_terminal "Custom Local Player ($pclass)" "java -cp '$cp' io.ClientJeu $pclass localhost $PORT"
}

start_solo_local() {
    local p1="$1"
    local p2="$2"
    local cp="escampe/build/classes/java/main:libraries/escampeobf.jar"
    if [ "$IS_WINDOWS" = true ]; then
        cp="escampe/build/classes/java/main;libraries/escampeobf.jar"
    fi
    run_in_terminal "Solo Local Game ($p1 vs $p2)" "java -cp '$cp' io.Solo $p1 $p2 dummy1 dummy2"
}

interactive_menu() {
    while true; do
        clear
        echo "===================================================================="
        echo "                     ESCAMPE GAME LAUNCH MANAGER                    "
        echo "===================================================================="
        echo " 1. Compile Project"
        echo " 2. Start Game Server (Port $PORT, $GAMES Game(s))"
        echo " 3. Connect Random Player Client"
        echo " 4. Connect Human Player Client"
        echo " 5. Connect Custom Local Player Client ($PLAYER_CLASS)"
        echo " 6. [MATCH] Custom Local Player vs Random Player"
        echo " 7. [MATCH] Custom Local Player vs Human Player"
        echo " 8. [MATCH] Custom Local Player vs Custom Local Player"
        echo " 9. [MATCH] Random Player vs Human Player (Jar Only - Demo)"
        echo "10. [MATCH] Random Player vs Random Player (Jar Only - Demo)"
        echo "11. [SOLO] Local Solo Game (Custom vs Custom - No Network Server)"
        echo "12. Exit"
        echo "===================================================================="
        
        read -p "Select an option [1-12]: " choice
        
        case "$choice" in
            1) compile_project; read -p "Press Enter to return to the menu..." ;;
            2) start_server; read -p "Server launched! Press Enter to return to the menu..." ;;
            3) start_random_client; read -p "Random client launched! Press Enter to return to the menu..." ;;
            4) start_human_client; read -p "Human client launched! Press Enter to return to the menu..." ;;
            5) compile_project; start_custom_client "$PLAYER_CLASS"; read -p "Custom client launched! Press Enter to return to the menu..." ;;
            6)
                compile_project
                start_server
                sleep 2
                start_random_client
                sleep 1
                start_custom_client "$PLAYER_CLASS"
                read -p "Match launched in separate windows! Press Enter to return to the menu..."
                ;;
            7)
                compile_project
                start_server
                sleep 2
                start_human_client
                sleep 1
                start_custom_client "$PLAYER_CLASS"
                read -p "Match launched in separate windows! Press Enter to return to the menu..."
                ;;
            8)
                compile_project
                start_server
                sleep 2
                start_custom_client "$PLAYER_CLASS"
                sleep 1
                start_custom_client "$OPPONENT_CLASS"
                read -p "Match launched in separate windows! Press Enter to return to the menu..."
                ;;
            9)
                start_server
                sleep 2
                start_random_client
                sleep 1
                start_human_client
                read -p "Match launched in separate windows! Press Enter to return to the menu..."
                ;;
            10)
                start_server
                sleep 2
                start_random_client
                sleep 1
                start_random_client
                read -p "Match launched in separate windows! Press Enter to return to the menu..."
                ;;
            11)
                compile_project
                start_solo_local "$PLAYER_CLASS" "$OPPONENT_CLASS"
                read -p "Solo game launched! Press Enter to return to the menu..."
                ;;
            12) exit 0 ;;
            *) echo "Invalid choice!"; sleep 1 ;;
        esac
    done
}

case "$MODE" in
    interactive) interactive_menu ;;
    compile) compile_project ;;
    server) start_server ;;
    client-random) start_random_client ;;
    client-human) start_human_client ;;
    client-custom) compile_project; start_custom_client "$PLAYER_CLASS" ;;
    solo) compile_project; start_solo_local "$PLAYER_CLASS" "$OPPONENT_CLASS" ;;
    match-custom-random)
        compile_project
        start_server
        sleep 2
        start_random_client
        sleep 1
        start_custom_client "$PLAYER_CLASS"
        ;;
    match-custom-human)
        compile_project
        start_server
        sleep 2
        start_human_client
        sleep 1
        start_custom_client "$PLAYER_CLASS"
        ;;
    match-custom-custom)
        compile_project
        start_server
        sleep 2
        start_custom_client "$PLAYER_CLASS"
        sleep 1
        start_custom_client "$OPPONENT_CLASS"
        ;;
    match-random-human)
        start_server
        sleep 2
        start_random_client
        sleep 1
        start_human_client
        ;;
    match-random-random)
        start_server
        sleep 2
        start_random_client
        sleep 1
        start_random_client
        ;;
    *)
        echo "Unknown mode: $MODE"
        exit 1
        ;;
esac
