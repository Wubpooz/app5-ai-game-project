# PowerShell Script to Launch Escampe Game Server and Clients
# Usage: .\scripts\launch.ps1 [-Mode <mode>] [-Port <port>] [-Games <games>] [-PlayerClass <class>]

param(
    [ValidateSet("interactive", "server", "client-random", "client-human", "client-custom", "solo", "match-custom-random", "match-custom-human", "match-custom-custom", "match-random-human", "match-random-random", "compile")]
    [string]$Mode = "interactive",
    [int]$Port = 1234,
    [int]$Games = 1,
    [string]$PlayerClass = "game.AIPlayer",
    [string]$OpponentClass = "game.AIPlayer"
)

$root = Resolve-Path "$PSScriptRoot\.."
$escampeDir = Join-Path $root "escampe"

function Compile-Project {
    Write-Host "Compiling Java classes..." -ForegroundColor Cyan
    $gradlew = Join-Path $escampeDir "gradlew.bat"
    if (Test-Path $gradlew) {
        Push-Location $escampeDir
        & .\gradlew.bat compileJava
        Pop-Location
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Compilation failed!"
            exit 1
        }
        Write-Host "Compilation successful!" -ForegroundColor Green
    } else {
        Write-Error "Gradle wrapper (gradlew.bat) not found at $escampeDir"
        exit 1
    }
}

function Start-ServerWindow {
    Write-Host "Starting Game Server on port $Port in a new window..." -ForegroundColor Yellow
    Start-Process powershell -WorkingDirectory $root -ArgumentList "-Command", "& { Write-Host '--- ESCAMPE SERVER ---' -ForegroundColor Yellow; java -cp libraries/escampeobf.jar escampe.ServeurJeu $Port $Games; if ($LASTEXITCODE -ne 0) { Write-Host 'Press Enter to exit...' -ForegroundColor Red; [void][System.Console]::ReadLine() } }"
}

function Start-RandomClientWindow {
    Write-Host "Starting Random Player Client (Jar) in a new window..." -ForegroundColor Cyan
    Start-Process powershell -WorkingDirectory $root -ArgumentList "-Command", "& { Write-Host '--- RANDOM PLAYER CLIENT (JAR) ---' -ForegroundColor Cyan; java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurAleatoire localhost $Port; if ($LASTEXITCODE -ne 0) { Write-Host 'Press Enter to exit...' -ForegroundColor Red; [void][System.Console]::ReadLine() } }"
}

function Start-HumanClientWindow {
    Write-Host "Starting Human Player Client (Jar) in a new window..." -ForegroundColor Magenta
    Start-Process powershell -WorkingDirectory $root -ArgumentList "-Command", "& { Write-Host '--- HUMAN PLAYER CLIENT (JAR) ---' -ForegroundColor Magenta; java -cp libraries/escampeobf.jar escampe.ClientJeu escampe.JoueurHumain localhost $Port; if ($LASTEXITCODE -ne 0) { Write-Host 'Press Enter to exit...' -ForegroundColor Red; [void][System.Console]::ReadLine() } }"
}

function Start-CustomClientWindow([string]$pClass) {
    Write-Host "Starting Custom Player Client ($pClass) in a new window..." -ForegroundColor Green
    Start-Process powershell -WorkingDirectory $root -ArgumentList "-Command", "& { Write-Host '--- CUSTOM LOCAL PLAYER ($pClass) ---' -ForegroundColor Green; java -cp 'escampe/build/classes/java/main;libraries/escampeobf.jar' io.ClientJeu $pClass localhost $Port; if ($LASTEXITCODE -ne 0) { Write-Host 'Press Enter to exit...' -ForegroundColor Red; [void][System.Console]::ReadLine() } }"
}

function Start-SoloLocalWindow([string]$p1, [string]$p2) {
    Write-Host "Starting Solo Local Game ($p1 vs $p2) in a new window..." -ForegroundColor Green
    Start-Process powershell -WorkingDirectory $root -ArgumentList "-Command", "& { Write-Host '--- SOLO LOCAL GAME ($p1 vs $p2) ---' -ForegroundColor Green; java -cp 'escampe/build/classes/java/main;libraries/escampeobf.jar' io.Solo $p1 $p2 dummy1 dummy2; if ($LASTEXITCODE -ne 0) { Write-Host 'Press Enter to exit...' -ForegroundColor Red; [void][System.Console]::ReadLine() } }"
}

# Interactive Mode Menu
if ($Mode -eq "interactive") {
    while ($true) {
        Clear-Host
        Write-Host "====================================================================" -ForegroundColor Green
        Write-Host "                     ESCAMPE GAME LAUNCH MANAGER                    " -ForegroundColor Green
        Write-Host "====================================================================" -ForegroundColor Green
        Write-Host " 1. Compile Project"
        Write-Host " 2. Start Game Server (Port $Port, $Games Game(s))"
        Write-Host " 3. Connect Random Player Client"
        Write-Host " 4. Connect Human Player Client"
        Write-Host " 5. Connect Custom Local Player Client ($PlayerClass)"
        Write-Host " 6. [MATCH] Custom Local Player vs Random Player"
        Write-Host " 7. [MATCH] Custom Local Player vs Human Player"
        Write-Host " 8. [MATCH] Custom Local Player vs Custom Local Player"
        Write-Host " 9. [MATCH] Random Player vs Human Player (Jar Only - Demo)"
        Write-Host "10. [MATCH] Random Player vs Random Player (Jar Only - Demo)"
        Write-Host "11. [SOLO] Local Solo Game (Custom vs Custom - No Network Server)"
        Write-Host "12. Exit"
        Write-Host "====================================================================" -ForegroundColor Green
        
        $choice = Read-Host "Select an option [1-12]"
        
        switch ($choice) {
            "1" { Compile-Project; Read-Host "Press Enter to return to the menu..." }
            "2" { Start-ServerWindow; Read-Host "Server launched! Press Enter to return to the menu..." }
            "3" { Start-RandomClientWindow; Read-Host "Random player client launched! Press Enter to return to the menu..." }
            "4" { Start-HumanClientWindow; Read-Host "Human player client launched! Press Enter to return to the menu..." }
            "5" { Compile-Project; Start-CustomClientWindow $PlayerClass; Read-Host "Custom client launched! Press Enter to return to the menu..." }
            "6" {
                Compile-Project
                Start-ServerWindow
                Start-Sleep -Seconds 2
                Start-RandomClientWindow
                Start-Sleep -Seconds 1
                Start-CustomClientWindow $PlayerClass
                Read-Host "Match launched in separate windows! Press Enter to return to the menu..."
            }
            "7" {
                Compile-Project
                Start-ServerWindow
                Start-Sleep -Seconds 2
                Start-HumanClientWindow
                Start-Sleep -Seconds 1
                Start-CustomClientWindow $PlayerClass
                Read-Host "Match launched in separate windows! Press Enter to return to the menu..."
            }
            "8" {
                Compile-Project
                Start-ServerWindow
                Start-Sleep -Seconds 2
                Start-CustomClientWindow $PlayerClass
                Start-Sleep -Seconds 1
                Start-CustomClientWindow $OpponentClass
                Read-Host "Match launched in separate windows! Press Enter to return to the menu..."
            }
            "9" {
                Start-ServerWindow
                Start-Sleep -Seconds 2
                Start-RandomClientWindow
                Start-Sleep -Seconds 1
                Start-HumanClientWindow
                Read-Host "Match launched in separate windows! Press Enter to return to the menu..."
            }
            "10" {
                Start-ServerWindow
                Start-Sleep -Seconds 2
                Start-RandomClientWindow
                Start-Sleep -Seconds 1
                Start-RandomClientWindow
                Read-Host "Match launched in separate windows! Press Enter to return to the menu..."
            }
            "11" {
                Compile-Project
                Start-SoloLocalWindow $PlayerClass $OpponentClass
                Read-Host "Solo game launched! Press Enter to return to the menu..."
            }
            "12" { exit }
            default { Write-Host "Invalid choice!" -ForegroundColor Red; Start-Sleep -Seconds 1 }
        }
    }
}
else {
    # Non-interactive CLI modes
    switch ($Mode) {
        "compile" { Compile-Project }
        "server" { Start-ServerWindow }
        "client-random" { Start-RandomClientWindow }
        "client-human" { Start-HumanClientWindow }
        "client-custom" { Compile-Project; Start-CustomClientWindow $PlayerClass }
        "solo" { Compile-Project; Start-SoloLocalWindow $PlayerClass $OpponentClass }
        "match-custom-random" {
            Compile-Project
            Start-ServerWindow
            Start-Sleep -Seconds 2
            Start-RandomClientWindow
            Start-Sleep -Seconds 1
            Start-CustomClientWindow $PlayerClass
        }
        "match-custom-human" {
            Compile-Project
            Start-ServerWindow
            Start-Sleep -Seconds 2
            Start-HumanClientWindow
            Start-Sleep -Seconds 1
            Start-CustomClientWindow $PlayerClass
        }
        "match-custom-custom" {
            Compile-Project
            Start-ServerWindow
            Start-Sleep -Seconds 2
            Start-CustomClientWindow $PlayerClass
            Start-Sleep -Seconds 1
            Start-CustomClientWindow $OpponentClass
        }
        "match-random-human" {
            Start-ServerWindow
            Start-Sleep -Seconds 2
            Start-RandomClientWindow
            Start-Sleep -Seconds 1
            Start-HumanClientWindow
        }
        "match-random-random" {
            Start-ServerWindow
            Start-Sleep -Seconds 2
            Start-RandomClientWindow
            Start-Sleep -Seconds 1
            Start-RandomClientWindow
        }
    }
}
