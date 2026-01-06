# 🎬 MoviePlanet - Movie & WebSeries Search Platform

A PHP-based movie and web series search platform that provides users with streaming and download options through TeraBox integration. The platform supports multiple subdomains for different movie niches and uses Google Custom Search API for dynamic poster fetching.

---

## 📁 Project Structure

```
movieplanet-23-1/
├── assets/                    # Static assets (CSS, JS, images)
├── content/                   # Domain-specific content files
│   ├── default/              # Default content
│   ├── harleywives/          # Domain-specific content
│   ├── moviesda10/           # Domain-specific content
│   └── olamovies/            # Domain-specific content
├── inc/                       # PHP includes and core functionality
├── pages/                     # Static pages (About, Contact, Privacy, etc.)
├── index.php                  # Homepage
├── msearch.php                # Main search results page
├── msearch2.php               # Alternate search results page
├── category_search.php        # Category-based search page
├── url_shortner.php           # URL shortener functionality
├── 404.php                    # Custom 404 error page
├── not_tera.php               # Fallback for non-TeraBox content
└── uninstall_tera.php         # TeraBox uninstall guidance page
```

---

## 🌐 Website Pages

### **Main Pages**

| Page | File | Description |
|------|------|-------------|
| **Homepage** | `index.php` | Landing page with search functionality, featured movies, and latest addons |
| **Search Results** | `msearch.php` | Displays search results with movie poster, description, and download options |
| **Category Search** | `category_search.php` | Shows popular searches when a category keyword is detected |
| **URL Shortener** | `url_shortner.php` | Generates shortened URLs for sharing |

### **Static Pages** (`/pages/`)

| Page | File | Description |
|------|------|-------------|
| About Us | `about-us.php` | Information about the platform |
| Contact Us | `contact-us.php` | Contact form and information |
| Privacy Policy | `privacy-policy.php` | Privacy and data handling policies |
| Terms of Service | `terms-of-services.php` | Terms and conditions |
| Content Takedown | `taken-down.php` | DMCA/Takedown request information |

---

## 🔍 Search Algorithm

The search functionality uses a **multi-tiered lookup system** with fallbacks:

### **Search Flow**

```
User Search Query
       │
       ▼
┌─────────────────────────────────────────┐
│           1. Query Correction           │
│  • Remove stop words (full, hd, watch)  │
│  • Clean non-alphanumeric characters    │
│  • Trim to 50 characters                │
│  • Check for category keywords          │
└─────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│        2. Cookie Cache Check            │
│  • Return cached image if available     │
└─────────────────────────────────────────┘
       │ (Miss)
       ▼
┌─────────────────────────────────────────┐
│         3. Database Lookup              │
│  • Query 'images' table                 │
│  • Query 'queries' table (approved)     │
└─────────────────────────────────────────┘
       │ (Miss)
       ▼
┌─────────────────────────────────────────┐
│      4. Unapproved Queries Table        │
│  • Check for partial matches            │
└─────────────────────────────────────────┘
       │ (Miss)
       ▼
┌─────────────────────────────────────────┐
│     5. Google Custom Search API         │
│  • Cycle through API keys               │
│  • Search for movie poster              │
│  • Cache result in database             │
└─────────────────────────────────────────┘
       │ (Miss)
       ▼
┌─────────────────────────────────────────┐
│         6. Fallback Image               │
│  • Return default not-found image       │
└─────────────────────────────────────────┘
```

### **Query Correction** (`inc/correction.php`)

- **Stop Words Removal**: Removes common words like "full", "hd", "watch", "latest", "download"
- **Character Cleaning**: Strips non-alphanumeric characters
- **Category Detection**: Redirects generic searches (e.g., "bollywood", "hollywood", "horror movies") to category page
- **Spelling Correction**: Checks database for stored corrections

### **Image Search** (`inc/search.php`)

1. **Cookie Caching**: Returns cached image URLs (TTL: ~1 hour)
2. **Database Lookup**: Checks `images` and `queries` tables
3. **API Key Rotation**: Cycles through multiple Google API keys ordered by usage
4. **Rate Limiting**: Marks exhausted keys and restores them after 20 minutes
5. **Error Handling**: Logs failures and returns fallback image

---

## 👤 User-Based Actions

### **On Homepage**

| Action | Description | Analytics Event |
|--------|-------------|-----------------|
| Search | Search for movies/series | `searched_from_home` |
| Featured Click | Click on featured movie poster | Navigates to search |

### **On Search Results Page**

| Action | Description | Analytics Event |
|--------|-------------|-----------------|
| Watch Online | Opens popup with streaming options | `watch_play_dialog`, `view_cart` |
| Download Now | Opens popup with download options | `new_btns_tera` |
| Open TeraBox App | Launches TeraBox Android app | `play_on_tera` |
| View in Browser | Opens TeraBox link in browser | `play_via_url` |
| Mirror Link | Opens alternate download link | `play_via_url` |
| Share | Shares current page URL | Uses Web Share API |
| Not This? | Shows search box to refine query | `not_this` |

### **User Tracking & State**

- **Cookies Used**:
  - `q` - Stores last search query (30 days)
  - `dm` - Stores subdomain/referrer (1 hour)
  - `lang` - User language preference
  - `quality` - Preferred video quality
  - `from` - Tracks image source (api/cache/database)

---

## 🔗 Data Fetching & APIs

### **External APIs**

| Service | Purpose | File |
|---------|---------|------|
| **Google Custom Search API** | Fetches movie poster images | `inc/search.php` |
| **Google Analytics 4** | User tracking and event logging | `inc/head-global.php` |

### **Database Tables**

| Table | Purpose |
|-------|---------|
| `images` | Curated movie poster URLs |
| `queries` | User search queries with approval status and poster URLs |
| `api_keys` | Google API keys with usage tracking and status |
| `extra_info` | Logging and debugging information |

### **External Link Generation** (`inc/tera_link_gen.php`)

Generates TeraBox links with:
- **Unique Link ID**: UTC timestamp + random 8-digit number
- **Device Detection**: Different links for Android vs other devices
- **Deep Linking**: Market intent URLs for Android app
- **Timezone Detection**: Special handling for Indian users (UTC+5:30)

---

## 🌍 Multi-Subdomain Support

The platform supports **43+ subdomains** representing different movie niches:

```php
$allowed_subdomains = [
    'ibomma', 'movierulz', 'soap2day', 'filmyzilla', 'mp4moviez',
    'tamilyogi', 'bolly4u', 'katmoviehd', 'telegram-web', '123movies',
    'extramovies', 'khatrimaza', '9xmovies', 'tamilrockers', 'fmovies',
    'worldfree4u', 'gomovies', 'putlockers', 'yesmovies', 'tubi-tv',
    'vegamovies', 'hdhub4u', 'moviesda', 'tamilblasters', 'moviesflix'
    // ... and more
];
```

### **Subdomain Detection** (`inc/function.php`)

- Extracts subdomain from `$_SERVER["SERVER_NAME"]`
- Supports override via `dm` GET/Cookie parameter
- Loads domain-specific content from `/content/{domain}/`
- Redirects unauthorized subdomains to default

---

## 🎨 UI Components

### **Popup Modal** (`inc/popup-modal.php`)

A reusable component that displays:
- TeraBox App download option
- Browser viewing option
- Mirror links (configurable)
- "100% Working Links" badge

### **Header** (`inc/header.php`)

- Logo with subdomain name
- Search bar
- Navigation links

### **Footer** (`inc/footer.php`)

- Legal links
- Copyright information
- Social links

---

## 📊 Analytics & Tracking

### **GA4 Events**

| Event | Trigger | Parameters |
|-------|---------|------------|
| `no_query_found` | Empty search | category, label |
| `searched invalid query` | URL/TeraBox link in search | query value |
| `watch_play_dialog` | Popup opened | - |
| `view_cart` | Download popup shown | item_id, item_name, brand, price |
| `play_on_tera` | TeraBox app opened | category, value (0.8 INR) |
| `play_via_url` | Browser link clicked | category, value (0.4 INR) |

### **Hit Recording** (`inc/record_hits_for_find_in_db.php`)

Tracks which database table served the image (for optimization).

---

## ⚙️ Configuration

### **Environment Setup**

Database credentials are defined in `inc/mydb.php`:
```php
$servername = "your_server";
$username = "your_username";
$password = "your_password";
$database = "your_database";
```

### **TeraBox Links** (`inc/tera_link_gen.php`)

Configure download links:
```php
$browserTeraLink1 = "https://be6.in/Privatefiles";  // Android
$browserTeraLink = "https://be6.in/Movie65676-mirrors-adstr";
$secondaryLinks = "https://be6.in/mov5667y8_adst_17dec";
$terawatch = "https://be6.in/adstera_new-mov678";
```

---

## 🔧 Key Features

- ✅ **Multi-subdomain architecture** - Single codebase, multiple domains
- ✅ **Smart search** - Query correction, category detection, spelling suggestions
- ✅ **Caching system** - Cookie and database caching for fast responses
- ✅ **API key rotation** - Automatic cycling through Google API keys
- ✅ **Device detection** - Android-specific deep links and redirects
- ✅ **Analytics integration** - GA4 event tracking for conversions
- ✅ **SEO optimized** - Schema.org markup, meta tags, canonical URLs
- ✅ **Responsive design** - Mobile-first approach

---

## 🚀 Deployment Notes

1. Configure database credentials in `inc/mydb.php`
2. Add Google API keys to the `api_keys` database table
3. Set up domain DNS to point subdomains to the server
4. Configure `.htaccess` for URL rewriting
5. Ensure proper SSL certificates for HTTPS

---

## 📝 License

Private/Proprietary - All rights reserved.

---

*Last Updated: January 2026*
