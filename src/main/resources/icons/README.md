# Application Icons

Platform-specific icons for LeechText native packages.

## Available Icons

- **`leechtext.icns`** - macOS icon (1.2MB)
  - Contains all required resolutions for macOS
  - Used by jpackage for macOS DMG builds

- **`leechtext.png`** - Linux icon (201KB)
  - 512x512 PNG format
  - Used by jpackage for Linux DEB/RPM builds

- **`leechtext.ico`** - Windows icon (needs to be created)
  - Multi-resolution ICO format
  - See creation instructions below

## Icon Design

The icon features:
- Dark navy blue background with rounded corners
- Open book with code tag symbol (`</>`) on left page
- Text/code lines on right page
- Blue download arrow integrated below the book
- Modern flat design with professional color scheme

## Creating Icons

### Quick Start
```bash
cd src/main/resources/icons
./create-icons.sh
```

This will create:
- `leechtext.icns` for macOS
- `leechtext.png` for Linux
- Instructions for Windows ICO

### Manual Creation

#### macOS ICNS (already created)
```bash
# Requires macOS with iconutil
mkdir leechtext.iconset
sips -s format png leechtext.jpg --resampleHeightWidth 16 16 --out leechtext.iconset/icon_16x16.png
# ... (create all required sizes)
iconutil -c icns leechtext.iconset
```

#### Linux PNG (already created)
```bash
sips -s format png leechtext.jpg --resampleHeightWidth 512 512 --out leechtext.png
```

#### Windows ICO
**Option 1: Use ImageMagick**
```bash
# Install ImageMagick first
brew install imagemagick

# Create ICO with multiple resolutions
convert leechtext.jpg -define icon:auto-resize=256,128,96,64,48,32,16 leechtext.ico
```

**Option 2: Online Tools**
- https://www.icoconverter.com/
- https://convertico.com/
- Upload `leechtext.jpg` and download the generated ICO

**Option 3: PhotoShop/GIMP**
- Open `leechtext.jpg`
- Create layers for 16x16, 32x32, 48x48, 256x256
- Export as ICO format

## Usage in Build

The icons are automatically used by jpackage:

```bash
# macOS
make package-mac    # Uses leechtext.icns

# Linux
make package-linux  # Uses leechtext.png

# Windows
make package-windows # Uses leechtext.ico (when created)
```

## Notes

- Icons are generated from `leechtext.jpg` source
- macOS and Linux icons are already created
- Windows ICO requires manual creation or ImageMagick
- Icon dimensions follow platform guidelines
- All icons use the same base design for consistency
