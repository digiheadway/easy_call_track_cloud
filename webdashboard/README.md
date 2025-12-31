# CallCloud Admin Dashboard

A modern, premium admin panel for CallCloud with comprehensive organization and call management features.

## 🚀 Features

### Authentication
- **Login & Signup** - Beautiful authentication pages with animated backgrounds
- **Organization Creation** - Create organizations with unique 6-character IDs
- **Secure Access** - Protected dashboard routes

### Dashboard Pages

#### 📊 Overview
- Real-time statistics cards (Total Calls, Active Employees, Avg Duration, Recordings)
- Interactive call trends chart
- Recent activity feed
- Quick action buttons

#### 👥 Employees Management
- Add, edit, and delete employees
- Search and filter employees
- Employee statistics (Total, Active, Calls Today)
- Detailed employee information table
- Department and role assignment

#### 📞 Calls
- View all call logs
- Filter by direction (Inbound/Outbound)
- Call statistics dashboard
- Search functionality
- Call status indicators (Completed, Missed, Rejected)
- Recording availability indicators

#### 🎙️ Recordings
- Audio player with controls
- Upload recordings
- Download and share functionality
- Recording metadata (duration, file size, timestamp)
- Tags for organization
- Beautiful grid layout

#### 📈 Reports
- Key metrics with trend indicators
- Call volume charts
- Top performers ranking with scores
- Department breakdown
- Export to PDF/CSV

#### ⚙️ Settings
- Organization information management
- Account settings
- Password change
- User preferences with toggles
- Email notifications
- Auto-record calls
- Danger zone (Export/Delete data)

## 🎨 Design Features

- **Premium Dark Theme** - Modern dark color palette with vibrant accents
- **Glassmorphism** - Beautiful frosted glass effects
- **Smooth Animations** - Micro-interactions and transitions
- **Gradient Elements** - Rich gradient backgrounds and buttons
- **Responsive Design** - Works on all screen sizes
- **Modern Typography** - Inter font for clean readability
- **Custom Scrollbars** - Styled scrollbars matching the theme
- **Hover Effects** - Interactive feedback on all elements

## 🛠️ Technology Stack

- **React 18** with TypeScript
- **Vite** for fast development and building
- **CSS3** with Custom Properties (CSS Variables)
- **Modern ES6+** JavaScript features

## 📦 Installation

1. Navigate to the webdashboard directory:
```bash
cd webdashboard
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm run dev
```

4. Open your browser and navigate to:
```
http://localhost:5173
```

## 🔧 Development

### File Structure
```
webdashboard/
├── src/
│   ├── components/
│   │   ├── Auth/
│   │   │   ├── Login.tsx
│   │   │   ├── Signup.tsx
│   │   │   └── Auth.css
│   │   └── Dashboard/
│   │       ├── Dashboard.tsx
│   │       ├── Dashboard.css
│   │       ├── Overview.tsx
│   │       ├── Employees.tsx
│   │       ├── Calls.tsx
│   │       ├── Recordings.tsx
│   │       ├── Reports.tsx
│   │       ├── Settings.tsx
│   │       └── Pages.css
│   ├── App.tsx
│   ├── App.css
│   ├── index.css
│   └── main.tsx
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

### Building for Production

```bash
npm run build
```

The optimized files will be in the `dist` directory.

## 🎯 Usage

### Demo Credentials
You can create a new account or use these credentials for testing:
- Email: any valid email
- Password: any password (8+ characters)

### Creating an Organization
1. Click "Create Account" on the login page
2. Fill in organization details
3. Click "Generate" to create a unique 6-character Organization ID
4. Complete the signup form
5. Access the full dashboard

### Managing Employees
1. Navigate to "Employees" in the sidebar
2. Click "Add Employee"
3. Fill in employee details
4. Assign department and role
5. Save to add to the organization

### Viewing Call Logs
1. Navigate to "Calls" in the sidebar
2. Use filters to view specific call types
3. Search for specific contacts or employees
4. View call details and listen to recordings

### Generating Reports
1. Navigate to "Reports" in the sidebar
2. Select report type and date range
3. View key metrics and charts
4. Export to PDF or CSV

## 🔮 Future Enhancements

- Integration with backend API
- Real-time call notifications
- Advanced analytics and insights
- Call recording transcription
- AI-powered call insights
- Multi-language support
- Role-based access control
- Two-factor authentication
- Mobile app version

## 🎨 Customization

### Colors
Edit the CSS custom properties in `src/index.css`:
```css
:root {
  --primary: #6366f1;
  --secondary: #8b5cf6;
  --accent: #ec4899;
  /* ... more colors */
}
```

### Adding New Pages
1. Create a new component in `src/components/Dashboard/`
2. Add navigation item in `Dashboard.tsx`
3. Include routing logic

## 📝 License

This project is part of the CallCloud application suite.

## 🤝 Support

For support and questions, please refer to the main CallCloud documentation.

---

**Built with ❤️ for CallCloud**
