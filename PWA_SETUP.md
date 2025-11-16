# Progressive Web App (PWA) Setup Guide

This application is now configured as a Progressive Web App (PWA), making it installable on mobile devices and desktop browsers.

## Features

✅ **Installable** - Users can install the app on their devices  
✅ **Offline Support** - Service worker caches assets for offline use  
✅ **App-like Experience** - Standalone display mode  
✅ **Fast Loading** - Cached resources load instantly  
✅ **Push Notifications Ready** - Infrastructure in place for future notifications

## Installation Steps

### 1. Install Dependencies

```bash
cd frontend
npm install
```

This will install:
- `@angular/pwa` - PWA schematics and tools
- `@angular/service-worker` - Service worker functionality

### 2. Generate App Icons

You need to create icon files for different device sizes. Use one of these methods:

#### Option A: Using the provided script (requires ImageMagick)

**Linux/macOS:**
```bash
cd frontend
chmod +x generate-icons.sh
./generate-icons.sh
```

**Windows:**
```cmd
cd frontend
generate-icons.bat
```

#### Option B: Using Online Tools

1. Visit https://realfavicongenerator.net/ or https://www.pwabuilder.com/imageGenerator
2. Upload your `frontend/src/assets/logo.png`
3. Generate and download the icon set
4. Extract to `frontend/src/assets/icons/`

#### Option C: Manual Creation

Create PNG files in these sizes and save them to `frontend/src/assets/icons/`:
- `icon-72x72.png`
- `icon-96x96.png`
- `icon-128x128.png`
- `icon-144x144.png`
- `icon-152x152.png`
- `icon-192x192.png` (required)
- `icon-384x384.png`
- `icon-512x512.png` (required)

### 3. Build the Application

```bash
cd frontend
npm run build
```

The build process will:
- Generate the service worker (`ngsw-worker.js`)
- Copy the manifest file
- Include icons in the build

### 4. Deploy

After building, deploy the `dist/frontend` directory to your web server.

**Important:** PWAs require HTTPS (except for localhost). Make sure your production server uses HTTPS.

## Testing PWA Installation

### Desktop (Chrome/Edge)

1. Open the application in Chrome or Edge
2. Look for the install icon (⊕) in the address bar
3. Click "Install" when prompted
4. The app will open in a standalone window

### Mobile (Android)

1. Open the application in Chrome
2. Tap the menu (three dots)
3. Select "Add to Home screen" or "Install app"
4. Confirm installation
5. The app icon will appear on your home screen

### Mobile (iOS Safari)

1. Open the application in Safari
2. Tap the Share button
3. Select "Add to Home Screen"
4. Customize the name if needed
5. Tap "Add"
6. The app icon will appear on your home screen

## Service Worker Configuration

The service worker is configured in `frontend/src/ngsw-config.json`:

- **App Assets**: Prefetched for instant loading
- **Static Assets**: Cached with 30-day expiration
- **API Calls**: Freshness strategy (checks server first, falls back to cache)

You can customize caching strategies in `ngsw-config.json` based on your needs.

## Manifest Configuration

The web app manifest is in `frontend/src/manifest.webmanifest`:

- **Name**: "Vertex Digital Systems - Employee Management"
- **Short Name**: "VDS EMS"
- **Theme Color**: Blue (#2563eb)
- **Display Mode**: Standalone (app-like experience)

## Troubleshooting

### Icons Not Showing

- Ensure all icon files exist in `src/assets/icons/`
- Check that icons are square (1:1 aspect ratio)
- Verify file paths in `manifest.webmanifest`

### Service Worker Not Registering

- Check browser console for errors
- Ensure you're using HTTPS (or localhost)
- Verify `ngsw-worker.js` is generated in the build output

### App Not Installable

- Check that HTTPS is enabled (required for PWA)
- Verify manifest.json is accessible
- Check browser console for manifest errors
- Ensure service worker is registered

### Clearing Cache

If you need to clear the service worker cache:

1. Open browser DevTools
2. Go to Application tab
3. Click "Service Workers"
4. Click "Unregister"
5. Click "Clear storage" → "Clear site data"

## Production Checklist

- [ ] All icon files generated and in place
- [ ] HTTPS enabled on production server
- [ ] Manifest file accessible at `/manifest.webmanifest`
- [ ] Service worker generated (`ngsw-worker.js` in build output)
- [ ] Test installation on Android device
- [ ] Test installation on iOS device
- [ ] Test installation on desktop browser
- [ ] Verify offline functionality works

## Future Enhancements

- Push notifications for leave approvals, document expiry alerts
- Background sync for offline actions
- App shortcuts for quick actions
- Share target API for document sharing

## Resources

- [Angular PWA Documentation](https://angular.io/guide/service-worker-intro)
- [Web App Manifest](https://web.dev/add-manifest/)
- [Service Worker API](https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API)
- [PWA Checklist](https://web.dev/pwa-checklist/)

