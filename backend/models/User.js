const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
    firstName: { type: String, required: true },
    lastName: { type: String, required: true },
    email: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    phone: { type: String, required: true, unique: true },
    birthDate: { type: String },
    preferences: { type: [String], default: [] },
    type: {
        type: String,
        default: 'Traveler',
        enum: ['Traveler', 'Admin', 'Authority'],
    }

});



module.exports = mongoose.model('User', userSchema);