@echo off
REM This script converts LaTeX diagrams into PNG images using pdflatex and ImageMagick on Windows.

IF "%~1"=="" (
    echo Usage: %~nx0 ^<tex_file^>
    exit /b 1
)

set "TEXFILE=%~1"
IF NOT EXIST "%TEXFILE%" (
    echo File not found: %TEXFILE%
    exit /b 1
)

where pdflatex >nul 2>&1
IF ERRORLEVEL 1 (
    echo pdflatex could not be found. Please install MiKTeX: winget install MiKTeX.MiKTeX
    exit /b 1
)

where magick >nul 2>&1
IF ERRORLEVEL 1 (
    echo magick could not be found. Please install ImageMagick: winget install --id ImageMagick.ImageMagick -e
    exit /b 1
)

echo Converting %TEXFILE% to PNG image...

pdflatex -shell-escape -interaction=nonstopmode "%TEXFILE%"
IF ERRORLEVEL 1 (
    echo pdflatex failed. Check the LaTeX source and try again.
    exit /b 1
)

set "BASENAME=%~n1"
magick -density 500 "%BASENAME%.pdf" -quality 100 "%BASENAME%.png"
IF ERRORLEVEL 1 (
    echo.
    echo ImageMagick conversion failed. Ghostscript may be absent or the PDF delegate could not run.
    echo Please install Ghostscript if needed: winget install --id Ghostscript.Ghostscript -e
    echo Review the error output above for the real cause.
    exit /b 1
)

echo Done: %BASENAME%.png
exit /b 0