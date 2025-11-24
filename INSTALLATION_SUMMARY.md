# Frontend Dependencies Installation Summary

## Installation Date
Generated: 2024

## Installed Components

### ✅ Node.js
- **Version**: v22.20.0
- **Status**: Installed and verified
- **Location**: C:\Program Files\nodejs\

### ✅ npm (Node Package Manager)
- **Version**: 10.9.3
- **Status**: Installed and verified
- **PowerShell Execution Policy**: Fixed (RemoteSigned)

### ✅ Angular CLI
- **Version**: 17.3.17 (matching project requirements)
- **Status**: Installed globally
- **Usage**: 
  - Global: `ng <command>`
  - Local: `npx ng <command>` (from frontend directory)

### ✅ Frontend Dependencies
All npm packages from `frontend/package.json` have been installed:
- **Location**: `frontend/node_modules/`

#### Core Dependencies:
- @angular/animations: ^17.3.0
- @angular/common: ^17.3.0
- @angular/compiler: ^17.3.0
- @angular/core: ^17.3.0
- @angular/forms: ^17.3.0
- @angular/platform-browser: ^17.3.0
- @angular/platform-browser-dynamic: ^17.3.0
- @angular/router: ^17.3.0
- @angular/service-worker: ^17.3.0
- chart.js: ^4.5.1
- rxjs: ~7.8.0
- tslib: ^2.3.0
- zone.js: ~0.14.3

#### Development Dependencies:
- @angular-devkit/build-angular: ^17.3.17
- @angular/cli: ^17.3.17
- @angular/compiler-cli: ^17.3.0
- @angular/pwa: ^17.3.0
- @types/jasmine: ~5.1.0
- @types/node: ^18.18.0
- jasmine-core: ~5.1.0
- karma: ~6.4.0
- karma-chrome-launcher: ~3.2.0
- karma-coverage: ~2.2.0
- karma-jasmine: ~5.1.0
- karma-jasmine-html-reporter: ~2.1.0
- typescript: ^5.4.2

## Important Note: Tesseract

**Tesseract is NOT a frontend dependency.** It is a **backend Java dependency** (Tess4J) used for OCR (Optical Character Recognition) functionality.

- **Backend Library**: Tess4J (Java wrapper for Tesseract OCR)
- **Location**: Defined in `pom.xml` as a Maven dependency
- **Purpose**: Document processing and text extraction from images/PDFs
- **Installation**: Handled automatically by Maven when building the backend

If you need to install Tesseract OCR engine for the backend:
1. **Windows**: Download from [GitHub Tesseract releases](https://github.com/UB-Mannheim/tesseract/wiki)
2. **Linux**: `sudo apt-get install tesseract-ocr` (Ubuntu/Debian)
3. **macOS**: `brew install tesseract`

The Java backend uses Tess4J which requires the Tesseract binary to be installed on the system.

## Verification Commands

### Check Node.js and npm:
```powershell
node --version
npm --version
```

### Check Angular CLI:
```powershell
ng version
# or
npx ng version
```

### Verify Frontend Dependencies:
```powershell
cd frontend
npm list --depth=0
```

### Test Frontend Build:
```powershell
cd frontend
npm run build
```

### Start Development Server:
```powershell
cd frontend
npm start
# or
ng serve
```

## Next Steps

1. **Verify Installation**:
   ```powershell
   cd frontend
   ng version
   ```

2. **Start Development Server** (if needed):
   ```powershell
   cd frontend
   npm start
   ```
   The app will be available at `http://localhost:4200`

3. **Build for Production**:
   ```powershell
   cd frontend
   npm run build
   ```

4. **Run Tests**:
   ```powershell
   cd frontend
   npm test
   ```

## Troubleshooting

### If Angular CLI is not found:
- Use `npx ng` instead of `ng` (uses local version)
- Or reinstall globally: `npm install -g @angular/cli@17.3.17`

### If npm install fails:
- Clear npm cache: `npm cache clean --force`
- Delete `node_modules` and `package-lock.json`
- Run `npm install` again

### If PowerShell scripts are blocked:
- Run: `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`

## System Information

- **OS**: Windows 10 (Build 22631)
- **Shell**: PowerShell
- **Project Path**: C:\Users\dev\IdeaProjects\Demo-EMS

---

**All frontend dependencies have been successfully installed!** ✅

