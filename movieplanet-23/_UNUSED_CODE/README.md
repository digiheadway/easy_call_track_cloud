# Unused/Deprecated Code Archive

This folder contains files and directories that are **not currently used** in the active MoviePlanet-23 application. These files have been moved here to keep the main codebase clean and organized.

## 📁 Contents

### Test & Development Files
- **test.php** - Testing file for TeraBox link generation
- **test.txt** - Test text file
- **events.php** - Test page for query parameter testing

### Deprecated PHP Files
- **search.php** - Old search file (now redirects to home)
- **outside-search.php** - External search handler (wrapper for msearch.php)
- **search-ss.php** - Search with screenshots (deprecated)
- **url_shortner.php** - URL shortening service (commented out in active code)
- **comet-player.php** - Video player page (not currently used)

### Unused Features
- **not_tera.php** - Country-specific link switching logic (not integrated)
- **fillepagebybmylead.php** - Ad iframe page (not in use)
- **uninstall_tera.php** - TeraBox uninstall script
- **fm657876.php** - File manager (234KB - likely third-party)

### Deprecated HTML/JS
- **8c09f305217fe875796ce41d6ad822bc.html** - Unknown HTML file
- **sw-check-permissions-dcd21.js** - Service worker permission checker
- **sw.enot.js** - Service worker stub

### Directories

#### tj2/
Purpose: Unknown - possibly testing or third-party integration
Status: Not referenced in active code

#### epages/
Purpose: External pages (content unknown)
Status: Not actively used

#### ext/
Purpose: External resources (content unknown)
Status: Not actively used

#### teramovies/
Purpose: TeraBox movie-related files
Status: Not actively integrated

#### open_in_app/
Purpose: App deep linking functionality
Status: Not currently implemented

#### trash/
Purpose: Previously deleted/deprecated files
Status: Archived trash folder

## ⚠️ Important Notes

### Files That ARE Still Used (Not Moved)
- **msearch.php** - Main search page (ACTIVE - referenced in scriptglobal.js)
- **msearch2.php** - Alternative search (ACTIVE - referenced in scriptglobal.js)
- **category_search.php** - Category search (ACTIVE - used in correction.php)
- **404.php** - Error page (ACTIVE - referenced in .htaccess)

## 🔄 Restoration

If you need to restore any of these files:

```bash
# From the root directory
mv _UNUSED_CODE/filename.php ./

# For directories
mv _UNUSED_CODE/directory_name ./
```

## 🗑 Safe to Delete?

Most files in this folder can be safely deleted if:
1. You've confirmed they're not needed for your specific setup
2. You have a backup of the entire codebase
3. You've tested the application without them

**Recommendation**: Keep this folder for at least 30 days before permanent deletion to ensure nothing breaks.

## 📊 Statistics

- **PHP Files**: 11
- **Directories**: 6
- **Other Files**: 3
- **Total Items**: 20+

**Date Archived**: 2025-12-22  
**Archived By**: System Organization
