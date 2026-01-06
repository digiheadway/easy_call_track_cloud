# Unused Code Archive

This folder contains files and directories that were identified as unused or deprecated during a cleanup of the codebase on 2025-12-22.

## Files moved here:

- **` firebase-messaging-sw.js`**: Legacy Firebase service worker (note leading space in filename).
- **`f-firebase-messaging-sw.js`**: Another unused Firebase service worker.
- **`index-oldgp.php`**: Deprecated index file (backup/old version).
- **`test.txt`**: Temporary test file.
- **`movietypesamelink.php`**: Unused movie type code (superseded by `movietype.php`).
- **`searchresultp.php`**: Unused search result template (superseded by `searchresult.php`).
- **`sw.js`**: Universal service worker loader (associated with removed push notification services).

## Directories moved here:

- **`go/`**: Contains `index.php` which appears to be a cloaking/redirect script with "Health Insurance" text used for SEO purposes or bot redirection. Not part of the core movie site functionality.
- **`gru/`**: Contains `original-agarwal-packers-and-movers.html`, which is an SEO spam page irrelevant to the movie website.

## Usage
These files are not currently used by the live application. If you find that something has broken after the cleanup, verify if one of these files was actually needed and move it back to the root directory.
