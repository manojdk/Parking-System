# UI/UX Design Documentation - Parking System

**Version:** 1.0  
**Last Updated:** June 7, 2026  
**Target Audience:** Frontend developers, UI/UX designers, product managers  

---

## Table of Contents

1. [Design System Overview](#design-system-overview)
2. [User Journey Maps](#user-journey-maps)
3. [Information Architecture](#information-architecture)
4. [Wireframes](#wireframes)
5. [Visual Design Guidelines](#visual-design-guidelines)
6. [Component Library](#component-library)
7. [Interaction Patterns](#interaction-patterns)
8. [Responsive Design](#responsive-design)
9. [Accessibility Standards](#accessibility-standards)
10. [Performance Guidelines](#performance-guidelines)

---

## Design System Overview

### Design Principles

```
1. SIMPLICITY
   └─ Minimal yet functional interface
   └─ Clear navigation and workflows
   └─ Reduce cognitive load

2. CLARITY
   └─ Explicit feedback for user actions
   └─ Clear status indicators
   └─ Understandable error messages

3. CONSISTENCY
   └─ Uniform component behavior
   └─ Consistent typography and spacing
   └─ Predictable navigation

4. EFFICIENCY
   └─ Minimize steps to complete tasks
   └─ Quick access to frequent actions
   └─ Smart defaults and suggestions

5. ACCESSIBILITY
   └─ WCAG 2.1 AA compliance
   └─ Keyboard navigation support
   └─ Screen reader compatibility
```

### Color Palette

```
PRIMARY COLORS:
├─ Primary Blue       #0066CC    (Main CTA, headings)
├─ Secondary Green    #2ECC40    (Success, available spaces)
└─ Accent Orange      #FF7F00    (Important dates, warnings)

SEMANTIC COLORS:
├─ Success Green      #27AE60    (Successful actions)
├─ Error Red          #E74C3C    (Errors, occupied spaces)
├─ Warning Orange     #F39C12    (Warnings, pending)
└─ Info Blue          #3498DB    (Information, hints)

NEUTRAL COLORS:
├─ Background White   #FFFFFF    (Main background)
├─ Light Gray         #F5F5F5    (Secondary background)
├─ Medium Gray        #CCCCCC    (Borders, dividers)
├─ Dark Gray          #666666    (Secondary text)
└─ Text Black         #1A1A1A    (Primary text)

USAGE EXAMPLES:
Status Indicators:
├─ Available Space    ● #2ECC40 (Green)
├─ Occupied Space     ● #E74C3C (Red)
├─ Reserved Space     ● #F39C12 (Orange)
└─ Maintenance        ● #999999 (Gray)
```

### Typography

```
FONT FAMILY:
├─ Headings:  Segoe UI, Tahoma, Arial (Sans-serif)
├─ Body:      Segoe UI, Tahoma, Arial (Sans-serif)
└─ Mono:      Monaco, 'Courier New' (Code snippets)

FONT SIZES:
├─ H1 (Page Title):       32px, weight 700, line-height 1.2
├─ H2 (Section Header):   24px, weight 700, line-height 1.3
├─ H3 (Sub Header):       18px, weight 600, line-height 1.3
├─ Body (Regular):        14px, weight 400, line-height 1.5
├─ Body (Small):          12px, weight 400, line-height 1.4
└─ Button Text:           14px, weight 600, line-height 1.2

RESPONSIVE SCALING:
Desktop:   As specified above
Tablet:    Scale down 10-15%
Mobile:    Scale down 20-25%
```

### Spacing Scale

```
xs: 4px     (Tight spacing)
sm: 8px     (Small spacing)
md: 16px    (Standard spacing)
lg: 24px    (Large spacing)
xl: 32px    (Extra large spacing)
xxl: 48px   (Huge spacing)

USAGE:
├─ Component padding:    md (16px)
├─ Section margin:       lg/xl (24-32px)
├─ Element gutter:       sm/md (8-16px)
└─ Page padding:         xl/xxl (32-48px)
```

---

## User Journey Maps

### User Persona 1: Regular Commuter (Priya)

```
Goal: Park vehicle quickly, get to office on time

Touchpoint 1: MORNING ARRIVAL
┌─────────────────────────────────────────┐
│ Priya arrives at parking entrance       │
│                                         │
│ ┌─ Current Pain:                        │
│ │  Unsure about available space         │
│ │  Manual space search is time-consuming│
│ │                                       │
│ └─ Solution:                            │
│    Mobile app shows available spaces    │
│    Recommended space with walking time  │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│ Priya opens Parking App                 │
│                                         │
│ ✓ Quick Login (via saved credentials)   │
│ ✓ Dashboard shows available spaces      │
│ ✓ Recommended parking spot (near exit)  │
│ ✓ Real-time occupancy rate (85%)        │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│ Priya books space A1-005                │
│                                         │
│ ✓ Confirmation notification             │
│ ✓ Parking ticket displayed              │
│ ✓ Navigation arrow to space             │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│ Priya parks vehicle                     │
│                                         │
│ ✓ Automatic entry recording             │
│ ✓ Parking started notification          │
│ ✓ Real-time duration timer              │
└─────────────────────────────────────────┘

Touchpoint 2: END OF DAY RETURN
┌─────────────────────────────────────────┐
│ Priya returns to vehicle                │
│                                         │
│ ┌─ Current Pain:                        │
│ │  Calculate fees mentally              │
│ │  Forgetting cost details              │
│ │                                       │
│ └─ Solution:                            │
│    Clear bill display with breakdown    │
│    Multiple payment options             │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│ Record exit in app                      │
│                                         │
│ ✓ Automatic exit recording              │
│ ✓ Duration: 8h 32m                      │
│ ✓ Bill generated instantly              │
│ ✓ Amount breakdown shown                │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│ Make payment                            │
│                                         │
│ ✓ UPI, Credit Card, Wallet options      │
│ ✓ One-tap payment                       │
│ ✓ Confirmation & receipt                │
│ ✓ Email/SMS receipt                     │
└─────────────────────────────────────────┘
```

### User Persona 2: Occasional Visitor (Rajesh)

```
Goal: Quick parking, minimal hassle

Touchpoint 1: FIRST-TIME VISIT
┌─────────────────────────────────────────┐
│ Rajesh arrives with no prior knowledge  │
│                                         │
│ ┌─ Current Pain:                        │
│ │  No account, manual search for spots  │
│ │  Confused about pricing               │
│ │                                       │
│ └─ Solution:                            │
│    Quick registration with email        │
│    Clear pricing display upfront        │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│ Quick Sign-up Flow (< 2 minutes)        │
│ ✓ Email or mobile number                │
│ ✓ Verify OTP                            │
│ ✓ Create password                       │
│ ✓ Add vehicle details                   │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│ Browse Available Spaces                 │
│                                         │
│ ✓ Map view of parking lot               │
│ ✓ Color-coded availability              │
│ ✓ Price per hour per slot type          │
│ ✓ Distance to facilities display        │
└─────────────────────────────────────────┘
```

### User Persona 3: Admin/Manager (Amit)

```
Goal: Monitor operations, manage parking spaces

Touchpoint 1: DAILY OPERATIONS MONITORING
┌─────────────────────────────────────────┐
│ Amit opens Admin Dashboard              │
│                                         │
│ At-a-Glance Metrics:
│ ├─ Current Occupancy:    75% (150/200)  │
│ ├─ Available Spaces:     50 slots       │
│ ├─ Occupied Spaces:      150 slots      │
│ ├─ Revenue Today:        ₹15,000        │
│ └─ Active Users:         1,200          │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│ Real-time Monitoring Features           │
│                                         │
│ ✓ Heat map: Occupancy by zone           │
│ ✓ Alerts: Anomalies detected            │
│ ✓ Analytics: Historical trends          │
│ ✓ Payment status: Pending/Paid          │
└─────────────────────────────────────────┘
```

---

## Information Architecture

### User-Centric IA Diagram

```
PARKING APP
│
├─ AUTHENTICATION
│  ├─ Login
│  └─ Register
│
├─ HOME DASHBOARD
│  ├─ Quick Stats (Occupancy, Revenue)
│  └─ Quick Actions (Find Space, Record Entry)
│
├─ PARKING MODULE
│  ├─ Find Parking
│  │  ├─ Map View
│  │  ├─ List View
│  │  ├─ Filter Options
│  │  └─ Book Space
│  │
│  ├─ Active Parking
│  │  ├─ Current Status
│  │  ├─ Duration Timer
│  │  └─ Record Exit
│  │
│  └─ History
│     ├─ Past Sessions
│     ├─ Analytics
│     └─ Receipt Download
│
├─ BILLING & PAYMENTS
│  ├─ Pending Bills
│  ├─ Payment History
│  ├─ Make Payment
│  └─ Download Invoices
│
├─ RESERVATIONS
│  ├─ Make Reservation
│  ├─ My Reservations
│  └─ Manage Bookings
│
├─ PROFILE & VEHICLES
│  ├─ Profile Settings
│  ├─ My Vehicles
│  ├─ Add Vehicle
│  └─ Preferences
│
└─ ADMIN PANEL (For Managers)
   ├─ Dashboard
   ├─ Space Management
   ├─ User Management
   ├─ Reports
   └─ Settings
```

---

## Wireframes

### 1. Login Screen

```
┌─────────────────────────────────────────┐
│                                         │
│          PARKING SYSTEM                 │
│          [Logo]                         │  Header (60px)
│                                         │
├─────────────────────────────────────────┤
│                                         │
│   ┌───────────────────────────────────┐ │  
│   │                                   │ │  Content Area
│   │   Username/Email                  │ │  
│   │   [_____________________]          │ │  
│   │                                   │ │  
│   │   Password                        │ │  
│   │   [_____________________]          │ │  
│   │                                   │ │  
│   │   [ Remember Me ]  Forgot?        │ │  
│   │                                   │ │  
│   │   ┌─────────────────────────────┐ │ │  
│   │   │   LOGIN                     │ │ │  Button (44px)
│   │   └─────────────────────────────┘ │ │  
│   │                                   │ │  
│   │   Don't have account? Sign Up    │ │  Link
│   │                                   │ │  
│   └───────────────────────────────────┘ │  
│                                         │
└─────────────────────────────────────────┘

Key Elements:
- Centered form with max-width 400px
- Clear visual hierarchy
- Error messages below input fields
- Loading state on button (spinner)
```

### 2. Home Dashboard (Mobile)

```
┌────────────────────────────────┐
│ ≡  Parking System    ⚙️  🔔  │ ─┐ Header
├────────────────────────────────┤ │
│                                │ │
│        TODAY'S STATS           │ │
│  ┌─────────────────────────┐   │ │ Card 1
│  │ Occupancy: 75%         │   │ │
│  │ Available: 50 slots    │   │ │
│  │ Revenue: ₹15,000       │   │ │
│  └─────────────────────────┘   │ │
│                                │ │
│         QUICK ACTIONS          │ │
│  ┌──────────────────────────┐  │ │ Card 2
│  │ 🅿️  Find Parking        │  │ │
│  │                         │  │ │
│  │ 📍 Record Entry        │  │ │
│  │                         │  │ │
│  │ ⏱️  Active Parking      │  │ │
│  │                         │  │ │
│  │ 💳 My Bills            │  │ │
│  └──────────────────────────┘  │ │
│                                │ │
│     ACTIVE PARKING SESSION      │ │
│  ┌──────────────────────────┐  │ │ Card 3
│  │ Space: A1-005           │  │ │
│  │ Time Parked: 2h 15m     │  │ │
│  │ [Record Exit]           │  │ │
│  └──────────────────────────┘  │ │
│                                │ │
├────────────────────────────────┤ │ Footer
│ 🏠  🅿️  📊  👤               │ │
└────────────────────────────────┘ ┘
```

### 3. Find Parking - Map View

```
DESKTOP VIEW:
┌──────────────────────────────────────────────────┐
│ ≡  PARKING SYSTEM            Search  ⚙️  🔔     │
├──────────────────────────────────────────────────┤
│  │ FILTERS                                       │
│  │ ┌──────────────────────────────┐              │
│  │ │ Vehicle Type: [All ▼]        │              │
│  │ │ Space Type: [All ▼]          │              │
│  │ │ Max Price: [50-500 ▼]        │              │
│  │ │ Floor: [All ▼]               │              │
│  │ │                              │              │
│  │ │ [ Apply Filters ]            │              │
│  │ └──────────────────────────────┘              │
│  │                                               │
│  │ AVAILABLE: 50                                  │
│  │ ┌──────────────────┐                          │
│  │ │ A1-001 ● NORMAL │ ₹50/hr                   │
│  │ │ A1-002 ● NORMAL │ ₹50/hr                   │
│  │ │ A1-003 ● VIP    │ ₹100/hr                  │
│  │ └──────────────────┘                          │
│  │                                               │
│                                                  │
│     ┌──────────────────────────────────────┐    │
│     │                                      │    │
│     │         PARKING LOT MAP              │    │
│     │                                      │    │
│     │   ┌────────────┐  ┌────────────┐    │    │
│     │   │ A1 SECTION │  │ B1 SECTION │    │    │
│     │   │ ● ● ● ● ●  │  │ ● ● ● ● ●  │    │    │
│     │   │ ● ◎ ● ◎ ●  │  │ ● ● ◎ ● ●  │    │    │
│     │   └────────────┘  └────────────┘    │    │
│     │                                      │    │
│     └──────────────────────────────────────┘    │
│                                                  │
└──────────────────────────────────────────────────┘

Legend:
● = Available (Green)
◎ = Occupied (Red)
★ = Reserved (Orange)
```

### 4. Record Exit Screen

```
┌────────────────────────────────────┐
│ ⬅  Record Exit                     │
├────────────────────────────────────┤
│                                    │  
│  PARKING SESSION DETAILS           │
│  ┌──────────────────────────────┐  │
│  │ Parking Ticket               │  │
│  │ Reference: TKT-ABC123DEF456  │  │
│  │                              │  │
│  │ Space: A1-005                │  │
│  │ Entry Time: 09:00 AM         │  │
│  │ Exit Time: 05:32 PM          │  │
│  │ Duration: 8h 32m             │  │
│  │ Rate: ₹50/hr                 │  │
│  │                              │  │
│  │ ┌──────────────────────────┐ │  
│  │ │ Fee Calculation:        │ │  
│  │ │ 8.53h × ₹50 = ₹426.50  │ │  
│  │ │ Tax (18%): ₹76.77       │ │  
│  │ │ ─────────────────────   │ │  
│  │ │ TOTAL: ₹503.27          │ │  
│  │ └──────────────────────────┘ │  
│  │                              │  
│  │ [Take Exit Photo]            │  
│  │ [Confirm Exit]               │  
│  └──────────────────────────────┘  │
│                                    │
└────────────────────────────────────┘
```

### 5. Payment Screen

```
PAYMENT FLOW:
┌────────────────────────────────────┐
│ ⬅  Make Payment                    │
├────────────────────────────────────┤
│                                    │
│  BILL SUMMARY                      │
│  ┌──────────────────────────────┐  │
│  │ Invoice: INV-2026-06-07-001 │  │
│  │ Amount: ₹503.27              │  │
│  │ Status: PENDING              │  │
│  │ Due Date: 2026-06-09         │  │
│  └──────────────────────────────┘  │
│                                    │
│  SELECT PAYMENT METHOD             │
│  ┌──────────────────────────────┐  │
│  │ ◯ UPI                        │  │
│  │ ◯ Credit Card                │  │
│  │ ◯ Debit Card                 │  │
│  │ ◯ Digital Wallet             │  │
│  │ ◯ Bank Transfer              │  │
│  └──────────────────────────────┘  │
│                                    │
│  [PROCEED TO PAYMENT]              │
│                                    │
│  SECURE PAYMENT                    │
│  🔒 Encrypted & Secure             │
│                                    │
└────────────────────────────────────┘

After Payment:
┌────────────────────────────────────┐
│ ✓ Payment Successful               │
├────────────────────────────────────┤
│                                    │
│  Order ID: ORD-123456789           │
│  Amount: ₹503.27                   │
│  Payment Method: UPI               │
│  Date: 2026-06-07 05:35 PM         │
│                                    │
│  [Download Receipt]  [Share]       │
│  [Back to Home]                    │
│                                    │
└────────────────────────────────────┘
```

### 6. Admin Dashboard

```
DESKTOP LAYOUT:
┌─────────────────────────────────────────────────────┐
│ ADMIN PANEL                    [User] ⚙️  Logout    │
├─────────────────────────────────────────────────────┤
│                                                     │
│  DASHBOARD > OVERVIEW                              │
│                                                     │
│  TODAY'S METRICS                                    │
│  ┌───────────────┬───────────────┬───────────────┐ │
│  │ Transactions  │ Revenue       │ Users         │ │
│  │ 1,250         │ ₹87,500       │ 5,480         │ │
│  │ ↑12% vs YD    │ ↑8% vs YD     │ ↑15% vs YD    │ │
│  └───────────────┴───────────────┴───────────────┘ │
│                                                     │
│  ┌─────────────────────────┬──────────────────────┐ │
│  │ OCCUPANCY TIMELINE      │ REVENUE TREND       │ │
│  │ (Line Chart)            │ (Bar Chart)         │ │
│  │ 100% ┌────────┐         │ ₹100K ├──────┤     │ │
│  │  75% │    ┌──┐╱└─────   │  ₹75K ├─────┤┌──   │ │
│  │  50% │─────   └────────  │  ₹50K ├──┤┌─┤├──  │ │
│  │   0% └──────────────────  │   ₹0K └─────┘────  │ │
│  └─────────────────────────┴──────────────────────┘ │
│                                                     │
│  ACTIVE SESSIONS                                    │
│  ┌──────────────────────────────────────────────┐  │
│  │ Space │ Vehicle │ Entry Time │ Duration │ Fee│  │
│  ├──────────────────────────────────────────────┤  │
│  │ A1-005│ KA-01   │ 09:00 AM   │ 2h 15m   │... │  │
│  │ B1-003│ KA-02   │ 10:30 AM   │ 1h 45m   │... │  │
│  └──────────────────────────────────────────────┘  │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## Visual Design Guidelines

### Button Styles

```
PRIMARY BUTTON (CTA):
┌──────────────────────┐
│   BOOK PARKING       │  Background: #0066CC
│                      │  Text: White (#FFFFFF)
└──────────────────────┘  Hover: #0052A3 (Darker)
                         Padding: 12px 24px
                         Border Radius: 4px
                         Font Weight: 600

SECONDARY BUTTON:
┌──────────────────────┐
│   CANCEL             │  Background: #F5F5F5
│                      │  Text: #1A1A1A
└──────────────────────┘  Hover: #E5E5E5
                         Border: 1px #CCCCCC

DANGER BUTTON:
┌──────────────────────┐
│   DELETE             │  Background: #E74C3C
│                      │  Text: White
└──────────────────────┘  Hover: #C0392B
                         Requires confirmation
```

### Card Component

```
┌─────────────────────────────────────────┐
│                                         │
│  CARD TITLE                             │  Padding: 16px
│  ┌─────────────────────────────────┐   │  
│  │                                 │   │  Content Area
│  │  Card Content                   │   │  
│  │                                 │   │  
│  ┌─────────────────────────────────────┐ │  
│  │ [Action Button]  [Secondary]       │ │  Footer
│  └─────────────────────────────────────┘ │  
│                                         │
└─────────────────────────────────────────┘

Properties:
- Background: #FFFFFF
- Border: 1px #CCCCCC
- Border Radius: 8px
- Box Shadow: 0 2px 4px rgba(0,0,0,0.1)
- Margin Bottom: 16px
```

### Form Input

```
┌──────────────────────────────────────┐
│ Label                                │
│ ┌─────────────────────────────────┐  │  
│ │ Placeholder text                │  │  Height: 44px
│ └─────────────────────────────────┘  │  Padding: 12px
│ ✓ Helper text below                  │  Border: 1px #CCCCCC
└──────────────────────────────────────┘  

States:
- Default: Border #CCCCCC, Background #FFFFFF
- Focus: Border #0066CC, Shadow: 0 0 0 3px rgba(0,102,204,0.1)
- Disabled: Opacity 0.5, Background #F5F5F5
- Error: Border #E74C3C, Icon ⚠️ (Red)
- Success: Border #27AE60, Icon ✓ (Green)
```

---

## Component Library

### Notification/Toast

```
SUCCESS:
┌─────────────────────────────────────┐
│ ✓ Parking slot booked successfully  │
└─────────────────────────────────────┘
Position: Top-right corner
Background: #27AE60
Text: White
Duration: 3-5 seconds
Auto-dismiss

ERROR:
┌─────────────────────────────────────┐
│ ✗ Failed to process payment         │
│   Please try again later            │
└─────────────────────────────────────┘
Position: Top-right corner
Background: #E74C3C
Text: White
Duration: 5 seconds (user must dismiss)

INFO:
┌─────────────────────────────────────┐
│ ℹ  Your parking will expire in 30m  │
└─────────────────────────────────────┘
Position: Top-right corner
Background: #3498DB
Text: White
Duration: 5 seconds
```

### Modal/Dialog

```
┌──────────────────────────────────────────┐
│  Confirm Action                       × │ Title + Close
├──────────────────────────────────────────┤
│                                          │
│  Are you sure you want to             │ Max-width: 500px
│  record exit for parking session?     │
│                                          │
│  Space: A1-005                         │ Content Area
│  Duration: 8h 32m                      │
│                                          │
│  ┌──────────────────┬──────────────────┐ │
│  │  [Cancel]        │  [Confirm Exit]  │ │ Footer (Buttons)
│  └──────────────────┴──────────────────┘ │
│                                          │
└──────────────────────────────────────────┘

Backdrop:
- Background: rgba(0,0,0,0.5)
- Prevent body scroll when open
```

### Status Indicator

```
AVAILABLE:  ● ● ● (Green pulse animation)
OCCUPIED:   ● ● ● (Static red)
RESERVED:   ● ● ● (Orange, pulsing)
MAINTENANCE: ● ● ● (Gray, striped pattern)

Animation Duration: 1.5s
```

---

## Interaction Patterns

### Loading States

```
1. SKELETON LOADING:
┌─────────────────────────────┐
│ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ Loading   │
│                             │
│ Skeleton of actual content  │
│ with animated shimmer       │
│ effect (0.5s animation)     │
└─────────────────────────────┘

2. SPINNER:
   ↻ Processing your request...
   Indeterminate spinner with text
   
3. PROGRESS BAR:
┌──────────────────────────────┐
│ ████████░░░░░░░░░░░░░░░░░░ │
│ 38% Complete                 │
└──────────────────────────────┘
```

### Form Validation

```
REAL-TIME VALIDATION:
┌────────────────────────────────────┐
│ Email Address                      │
│ ┌──────────────────────────────┐   │
│ │ user@example.com             │   │ 
│ └──────────────────────────────┘   │
│ ✓ Email format is valid             │ Auto-checked after 500ms delay
│                                    │ Debounced requests to server
│                                    │
│ SUBMISSION ERRORS:                 │
│ ┌────────────────────────────────┐ │
│ │ Username                       │ │
│ │ ┌──────────────────────────┐   │ │
│ │ │ john_doe                 │   │ │
│ │ └──────────────────────────┘   │ │
│ │ ⚠ Username already taken        │ │ Highlight field
│                                    │
└────────────────────────────────────┘
```

### Pagination

```
← | 1 | 2 | [3] | 4 | 5 | ... | 20 | →

Mobile:
← Page 3 of 20 →

With limits:
Show [10 ▼] items per page
Current: 1-10 of 250 results
```

---

## Responsive Design

### Breakpoints

```
Mobile:        < 768px
Tablet:        768px - 1024px
Desktop:       > 1024px
Large Desktop: > 1440px

Grid System:
Mobile:   4 columns
Tablet:   8 columns
Desktop:  12 columns
```

### Mobile-First Layouts

```
MOBILE (360px):
┌────────────────┐
│ Header (56px)  │
├────────────────┤
│                │
│  Content       │
│  Full Width    │
│                │
│                │
├────────────────┤
│ Footer Nav     │
├────────────────┤

TABLET (768px):
┌─────────────────────────────┐
│ Header                      │
├────────────┬────────────────┤
│ Sidebar    │                │
│ 240px      │  Content       │
│            │  (flex-grow)   │
│            │                │
└────────────┴────────────────┘

DESKTOP (1024px+):
┌──────────────────────────────────────┐
│ Header                               │
├──────────┬───────────┬────────────────┤
│ Sidebar  │ Content   │ Right Panel    │
│ 240px    │ (flex)    │ 300px          │
│          │           │                │
└──────────┴───────────┴────────────────┘
```

---

## Accessibility Standards

### WCAG 2.1 AA Compliance

```
1. COLOR CONTRAST:
   Default: 4.5:1 minimum (Large: 3:1)
   
   Examples:
   ✓ Black (#1A1A1A) on White (#FFFFFF): 21:1
   ✓ Dark Gray (#666666) on White: 8:1
   ✗ Light Gray (#CCCCCC) on White: 1.3:1

2. KEYBOARD NAVIGATION:
   ✓ Tab through all interactive elements
   ✓ Tab order follows visual flow (left→right, top→bottom)
   ✓ Focus indicator clearly visible (4px outline)
   ✓ Logical focus management in modals/popups

3. SCREEN READER SUPPORT:
   ✓ Semantic HTML (button, nav, main, etc.)
   ✓ aria-label for icon buttons
   ✓ aria-describedby for error messages
   ✓ Role="alert" for dynamic notifications
   ✓ List markup for lists

4. FORM ACCESSIBILITY:
   ✓ Explicit label associations (<label for="id">)
   ✓ Error messages linked to fields (aria-labelledby)
   ✓ Required field indicator
   ✓ Clear focus states

5. TEXT ALTERNATIVES:
   ✓ Alt text for all images
   ✓ Captions for images showing status
   ✓ Text descriptions for icons

6. MOTION & ANIMATIONS:
   ✓ prefers-reduced-motion respected
   ✓ No auto-playing videos/GIFs
   ✓ Animations < 5 seconds
```

### Dark Mode Support

```
CSS Custom Properties:
:root {
  --bg-primary: #FFFFFF;
  --text-primary: #1A1A1A;
  --accent: #0066CC;
}

@media (prefers-color-scheme: dark) {
  :root {
    --bg-primary: #1A1A1A;
    --text-primary: #FFFFFF;
    --accent: #4D94FF;
  }
}
```

---

## Performance Guidelines

### Image Optimization

```
RESPONSIVE IMAGES:
<picture>
  <source media="(min-width: 1024px)" srcset="image-1024w.jpg">
  <source media="(min-width: 768px)" srcset="image-768w.jpg">
  <img src="image-360w.jpg" alt="Parking lot">
</picture>

FILE SIZES:
- Desktop (1024px):   200-300 KB
- Tablet (768px):     150-200 KB
- Mobile (360px):     80-120 KB

Format: WebP (primary) + JPEG (fallback)
Lazy Loading: Implement for images below fold
```

### Performance Targets

```
Metric               Target    Tool
─────────────────────────────────────
First Contentful    < 1.8s    Lighthouse
Largest Contentful  < 2.5s    Lighthouse
Cumulative Layout   < 0.1     Lighthouse
Shift

Load Time:          < 3s      (3G)
Time to Interactive: < 5s     (3G)
JavaScript Bundle:  < 150KB   (gzipped)
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-07 | Initial UI/UX documentation |

---

**Document Prepared By:** UI/UX Design Team  
**Last Updated:** 2026-06-07  
**Next Review:** 2026-09-07

**End of Document**

