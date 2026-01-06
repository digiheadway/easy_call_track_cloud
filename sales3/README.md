# Uptown Properties CRM

A fully responsive Real Estate CRM (Customer Relationship Management) system built with React, TypeScript, and Tailwind CSS. This application provides comprehensive lead management, task tracking, and property matching capabilities for real estate professionals.

## Features

### 🏠 Lead Management
- **Comprehensive Lead Tracking**: Store and manage detailed lead information including contact details, property requirements, and preferences
- **Advanced Filtering**: Filter leads by stage, priority, source, budget range, and more
- **Search Functionality**: Quick search across lead names, phone numbers, locations, and requirements
- **Lead Details View**: Detailed view with editable forms for all lead information
- **Stage Management**: Track leads through different stages of the sales pipeline

### 📋 Task Management
- **Task Creation & Tracking**: Create and manage tasks with due dates and priorities
- **Status-based Organization**: Organize tasks by Today, Upcoming, Overdue, and Closed
- **Task Actions**: Complete, delete, and manage task status
- **Lead Association**: Link tasks to specific leads for better organization

### 🔍 Find Match
- **Property Matching**: Match leads with suitable properties
- **Task-based Workflow**: Manage property matching as tasks with deadlines
- **Status Tracking**: Track matching progress and completion

### 📅 Calendar Integration
- **Weekly View**: Calendar view showing tasks and appointments
- **Task Scheduling**: Schedule site visits and meetings
- **Date Navigation**: Easy navigation between weeks and months
- **Task Details**: View detailed task information for selected dates

### 📱 Progressive Web App (PWA)
- **Offline Support**: Works offline with cached data
- **Installable**: Can be installed as a native app on mobile devices
- **Responsive Design**: Optimized for all screen sizes
- **Fast Loading**: Optimized performance with modern web technologies

### 🎨 Modern UI/UX
- **Clean Design**: Modern, professional interface
- **Responsive Layout**: Works perfectly on desktop, tablet, and mobile
- **Dark Sidebar**: Professional navigation with clear visual hierarchy
- **Interactive Elements**: Hover effects, smooth transitions, and intuitive interactions

## Technology Stack

- **Frontend**: React 18 with TypeScript
- **Styling**: Tailwind CSS for responsive design
- **Icons**: Lucide React for beautiful icons
- **Date Handling**: date-fns for date manipulation
- **Routing**: React Router for navigation
- **Build Tool**: Vite for fast development and building
- **PWA**: Vite PWA plugin for progressive web app features

## Getting Started

### Prerequisites
- Node.js 16+ 
- npm or yarn

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd real-estate-crm
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start the development server**
   ```bash
   npm run dev
   ```

4. **Open your browser**
   Navigate to `http://localhost:3000` to view the application

### Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint
- `npm run lint:fix` - Fix ESLint errors

## Project Structure

```
src/
├── components/          # Reusable UI components
│   └── Layout.tsx      # Main layout with sidebar
├── contexts/           # React contexts for state management
│   ├── LeadContext.tsx # Lead state management
│   └── TaskContext.tsx # Task state management
├── data/               # Dummy data and mock APIs
│   └── dummyData.ts    # Sample leads and tasks
├── pages/              # Page components
│   ├── Leads.tsx       # Lead management page
│   ├── LeadDetails.tsx # Individual lead details
│   ├── Tasks.tsx       # Task management page
│   ├── FindMatch.tsx   # Property matching page
│   ├── Calendar.tsx    # Calendar view
│   └── ...            # Other pages
├── types/              # TypeScript type definitions
│   └── index.ts        # Interface definitions
├── App.tsx             # Main application component
├── main.tsx            # Application entry point
└── index.css           # Global styles
```

## Dummy Data

The application comes with 10 sample leads and 5 sample tasks to demonstrate functionality:

### Sample Leads
- Deepak (Delhi) - Looking for shop in mandi
- Monu (Panipat) - Shop plot on Ujha Road
- Rinku Sarpanch (Panipat) - VIP client looking for plot
- Sandeep Chough (Panipat) - Residential plot in Sector 24
- Vishal (Panipat) - House in good area
- And 5 more with varied requirements

### Sample Tasks
- Modal town property showing task
- Kothi dundni h (bungalow finding)
- Clinic location scouting
- Property viewing tasks
- Follow-up calls

## Features in Detail

### Lead Management
- **Contact Information**: Name, phone, alternate contact, address
- **Property Requirements**: Type, budget, location, size, purpose
- **Lead Status**: Stage tracking, priority levels, source information
- **Additional Data**: Tags, segments, notes, and custom fields

### Task Management
- **Task Types**: Calls, meetings, site visits, follow-ups
- **Priority Levels**: High, Medium, Low, General
- **Due Dates**: Date and time tracking
- **Status Management**: Today, Upcoming, Overdue, Closed

### Calendar Features
- **Weekly View**: 7-day calendar layout
- **Task Display**: Visual representation of tasks on calendar
- **Navigation**: Previous/Next week, Today button
- **Task Details**: Expandable task information

## Responsive Design

The application is fully responsive and optimized for:
- **Desktop**: Full-featured interface with sidebar navigation
- **Tablet**: Adapted layout with collapsible sidebar
- **Mobile**: Mobile-first design with touch-friendly interactions

## PWA Features

- **Offline Support**: Works without internet connection
- **Installable**: Add to home screen on mobile devices
- **Fast Loading**: Optimized bundle size and caching
- **Native Feel**: App-like experience on mobile devices

## Future Enhancements

- **API Integration**: Connect to backend services
- **Real-time Updates**: WebSocket integration for live updates
- **Advanced Analytics**: Dashboard with charts and metrics
- **Email Integration**: Send emails directly from the CRM
- **Document Management**: Upload and manage property documents
- **Team Collaboration**: Multi-user support with roles
- **Property Database**: Comprehensive property listing system

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please contact the development team or create an issue in the repository.

---

**Built with ❤️ for Uptown Properties** 