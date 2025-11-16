# PWA Icons

This directory should contain the following icon files for Progressive Web App (PWA) support:

- `icon-72x72.png` - 72x72 pixels
- `icon-96x96.png` - 96x96 pixels
- `icon-128x128.png` - 128x128 pixels
- `icon-144x144.png` - 144x144 pixels
- `icon-152x152.png` - 152x152 pixels
- `icon-192x192.png` - 192x192 pixels (required)
- `icon-384x384.png` - 384x384 pixels
- `icon-512x512.png` - 512x512 pixels (required)

## Generating Icons

You can generate these icons from your logo using online tools or ImageMagick:

### Using ImageMagick (if installed):
```bash
# From the frontend/src/assets directory
convert logo.png -resize 72x72 icons/icon-72x72.png
convert logo.png -resize 96x96 icons/icon-96x96.png
convert logo.png -resize 128x128 icons/icon-128x128.png
convert logo.png -resize 144x144 icons/icon-144x144.png
convert logo.png -resize 152x152 icons/icon-152x152.png
convert logo.png -resize 192x192 icons/icon-192x192.png
convert logo.png -resize 384x384 icons/icon-384x384.png
convert logo.png -resize 512x512 icons/icon-512x512.png
```

### Using Online Tools:
- https://realfavicongenerator.net/
- https://www.pwabuilder.com/imageGenerator
- https://favicon.io/favicon-converter/

### Using Node.js script:
You can also use a package like `sharp` or `jimp` to generate icons programmatically.

## Icon Requirements:
- Format: PNG
- Square aspect ratio (1:1)
- Transparent background recommended
- High quality (no pixelation)
- The 192x192 and 512x512 icons are required for PWA installation

