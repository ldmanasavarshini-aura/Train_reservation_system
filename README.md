# 🚆 NexusRail Enterprise - Train Reservation System

## Overview
Developed a full-stack Train Reservation System that enables passengers to book train tickets and administrators to manage train schedules through a web-based interface. The application uses a Java backend with REST-like endpoints and a responsive frontend built with HTML, CSS, and JavaScript.

## Key Features
- Train ticket booking system
- Train schedule management (Admin Panel)
- Real-time seat availability validation
- Dynamic client-server communication using Fetch API
- Responsive and professional user interface
- In-memory data management using Java Collections

## Technologies Used
- Java
- HTML5
- CSS3
- JavaScript
- Java HTTP Server API
- HashMap Collections Framework

## Run Locally

Install Java and Node.js, then run:

```sh
npm run dev
```

Open http://localhost:8080 in your browser. The server keeps reservation data in memory while it is running.

## Deploy to Vercel

Import this repository as a Vercel project and deploy the `main` branch. The static site is served from the repository root, and the booking endpoints run as Vercel Node.js functions. Vercel's project must be connected to this GitHub repository for pushes to create deployments.

Reservations use in-memory storage, so data is temporary and may reset when the server or function instance restarts. Use a persistent database before relying on this app for real reservations.

## Skills Demonstrated
- Object-Oriented Programming (OOP)
- Backend Development
- Frontend Development
- Client-Server Architecture
- Form Handling & Validation
- API Integration
- Responsive UI Design

## Author
**Manasavarshini L.D.**
