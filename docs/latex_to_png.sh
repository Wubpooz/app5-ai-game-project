#!/bin/bash
# This script converts the LaTeX diagrams into PNG images using pdflatex and magick

# Check if the required tools are installed
if ! command -v pdflatex &> /dev/null
then
    echo "pdflatex could not be found. Please install MiKTeX: winget install MiKTeX.MiKTeX or visit https://miktex.org/download"
    exit 1
fi

if ! command -v magick &> /dev/null
then
    echo "magick could not be found. Please install ImageMagick: winget install ImageMagick.Q16 or visit https://imagemagick.org/script/download.php"
    exit 1
fi

# Input handling
if [ "$#" -ne 1 || ! -f "$1" ]; then
    echo "Usage: $0 <tex_file>"
    exit 1
fi

echo "Converting $1 to PNG image..."

echo "Converting $1 to $1.png..."
# Export the tex file in input to pdf
pdflatex -shell-escape -interaction=nonstopmode "$1"

# Convert the pdf to png using magick
if ! magick -density 500 "$(basename "$1" .tex).pdf" -quality 100 "$(basename "$1" .tex).png"; then
    echo
    echo "ImageMagick conversion failed. Ghostscript may be absent or the PDF delegate could not run."
    echo "Please install Ghostscript: winget install --id Ghostscript.Ghostscript -e or visit https://ghostscript.com/download.html"
    echo "Review the error output above for the real cause."
    exit 1
fi
