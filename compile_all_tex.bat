echo off
set "ROOT=%~dp0"
echo Converting LaTeX diagrams to PNG images...
call "%ROOT%docs\latex_to_png.bat" "%ROOT%docs\SSE.tex"
call "%ROOT%docs\latex_to_png.bat" "%ROOT%docs\residual_trunk.tex"
call "%ROOT%docs\latex_to_png.bat" "%ROOT%docs\output_head.tex"
call "%ROOT%docs\latex_to_png.bat" "%ROOT%docs\dual_perspective_flowchart.tex"
echo All conversions completed.

echo Compiling BandDPER.tex to PDF...
pdflatex -shell-escape -interaction=nonstopmode "%ROOT%BandDPER.tex"
if errorlevel 1 (
    echo pdflatex failed. Check the LaTeX source and try again.
    exit /b 1
)
echo Compilation complete.
