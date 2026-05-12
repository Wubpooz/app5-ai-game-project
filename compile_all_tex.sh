#!/bin/bash
# Convert all the relevant .tex files to PNG images
echo "Converting LaTeX files to PNG images..."
./docs/latex_to_png.sh "docs/SSE.tex"
./docs/latex_to_png.sh "docs/residual_trunk.tex"
./docs/latex_to_png.sh "docs/output_head.tex"
./docs/latex_to_png.sh "docs/dual_perspective_flowchart.tex"
echo "Conversion complete."

# Compile the main BandDPER.tex file to PDF
echo "Compiling BandDPER.tex to PDF..."
pdflatex -shell-escape -interaction=nonstopmode "BandDPER.tex"
echo "Compilation complete."