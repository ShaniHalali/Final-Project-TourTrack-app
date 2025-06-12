const express = require('express');
const mongoose = require('mongoose');
const dotenv = require('dotenv');
const cors = require('cors');
const allPublicPosts = require('./routes/allPublicPosts');

// Load environment variables before using them
dotenv.config();

// Connect to MongoDB
const connectDB = require('./config/db');
connectDB();

// Create the Express app
const app = express();

// Middleware
app.use(cors());
app.use(express.json()); // Parse incoming JSON requests

// Routes
const userRoutes = require('./routes/userRoutes');
app.use('/api/users', userRoutes); // All routes under /api/users will be handled here

const routeRoutes = require('./routes/routeRoutes');
app.use('/api/routes', routeRoutes);

app.use('/api', allPublicPosts);



// Start the server localhost
/*
const PORT = process.env.PORT || 5000;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 Server running on port ${PORT}`);
*/

    module.exports = app;
    console.log(`✅ Connected to MongoDB and Vercel`);

