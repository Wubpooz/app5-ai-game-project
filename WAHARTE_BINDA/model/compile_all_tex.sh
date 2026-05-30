#!/bin/bash
# Convert all the relevant .tex files to PNG images
echo "Converting LaTeX files to PNG images..."
./components/latex_to_png.sh "components/SSE.tex"
./components/latex_to_png.sh "components/residual_trunk.tex"
./components/latex_to_png.sh "components/output_head.tex"
./components/latex_to_png.sh "components/dual_perspective_flowchart.tex"
echo "Conversion complete."

# Compile the main BandDPER.tex file to PDF
echo "Compiling BandDPER.tex to PDF..."
pdflatex -shell-escape -interaction=nonstopmode "BandDPER.tex"
echo "Compilation complete."