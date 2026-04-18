#!/bin/bash

# Icon creation script for LeechText
# Creates platform-specific icons from leechtext.jpg

ICON_DIR="/Users/haipham22/Workspace/leech/leechtext-java/src/main/resources/icons"
cd "$ICON_DIR" || exit 1

echo "Creating platform-specific icons..."

# Clean up existing icons
rm -f leechtext.icns leechtext.png leechtext.ico
rm -rf leechtext.iconset

# 1. Create ICNS for macOS
echo "Creating macOS ICNS..."
mkdir leechtext.iconset
sips -s format png leechtext.jpg --resampleHeightWidth 16 16 --out leechtext.iconset/icon_16x16.png >/dev/null 2>&1
sips -s format png leechtext.jpg --resampleHeightWidth 32 32 --out leechtext.iconset/icon_16x16@2x.png >/dev/null 2>&1
sips -s format png leechtext.jpg --resampleHeightWidth 32 32 --out leechtext.iconset/icon_32x32.png >/dev/null 2>&1
sips -s format png leechtext.jpg --resampleHeightWidth 64 64 --out leechtext.iconset/icon_32x32@2x.png >/dev/null 2>&1
sips -s format png leechtext.jpg --resampleHeightWidth 128 128 --out leechtext.iconset/icon_128x128.png >/dev/null 2>&1
sips -s format png leechtext.jpg --resampleHeightWidth 256 256 --out leechtext.iconset/icon_128x128@2x.png >/dev/null 2>&1
sips -s format png leechtext.jpg --resampleHeightWidth 256 256 --out leechtext.iconset/icon_256x256.png >/dev/null 2>&1
sips -s format png leechtext.jpg --resampleHeightWidth 512 512 --out leechtext.iconset/icon_256x256@2x.png >/dev/null 2>&1
sips -s format png leechtext.jpg --resampleHeightWidth 512 512 --out leechtext.iconset/icon_512x512.png >/dev/null 2>&1
sips -s format png leechtext.jpg --resampleHeightWidth 1024 1024 --out leechtext.iconset/icon_512x512@2x.png >/dev/null 2>&1
iconutil -c icns leechtext.iconset
rm -rf leechtext.iconset
echo "  ✓ Created leechtext.icns"

# 2. Create PNG for Linux
echo "Creating Linux PNG..."
sips -s format png leechtext.jpg --resampleHeightWidth 512 512 --out leechtext.png >/dev/null 2>&1
echo "  ✓ Created leechtext.png (512x512)"

# 3. Create ICO for Windows
echo "Creating Windows ICO..."
# Note: Windows ICO creation requires ImageMagick or PIL
# For now, we'll create a placeholder
if command -v convert >/dev/null 2>&1; then
    convert leechtext.jpg -define icon:auto-resize=256,128,96,64,48,32,16 leechtext.ico
    echo "  ✓ Created leechtext.ico using ImageMagick"
else
    echo "  ⚠ ImageMagick not found"
    echo "    To create Windows ICO, use online tool:"
    echo "    https://www.icoconverter.com/"
    echo "    Or install: brew install imagemagick"
fi

echo ""
echo "Icons summary:"
ls -lh leechtext.* | grep -E '\.(icns|png|ico)$' | awk '{printf "  %-30s %6s\n", $9, $5}'
