echo off
set "ROOT=%~dp0"
echo Converting LaTeX diagrams to PNG images...
call "%ROOT%components\latex_to_png.bat" "%ROOT%components\SSE.tex"
call "%ROOT%components\latex_to_png.bat" "%ROOT%components\residual_trunk.tex"
call "%ROOT%components\latex_to_png.bat" "%ROOT%components\output_head.tex"
call "%ROOT%components\latex_to_png.bat" "%ROOT%components\dual_perspective_flowchart.tex"
echo All conversions completed.

echo Compiling BandDPER.tex to PDF...
@REM pdflatex -shell-escape -interaction=nonstopmode "%ROOT%BandDPER.tex"
lualatex "%ROOT%BandDPER.tex"
if errorlevel 1 (
    echo pdflatex failed. Check the LaTeX source and try again.
    exit /b 1
)
echo Compilation complete.
