const express = require('express');
const router = express.Router();
const {
    updatePreferences,
    signUpUser,
    loginUser,
    getUserProfile,
    getPrivacySettings,
    savePrivacySettings
} = require('../controllers/userController');
const authMiddleware = require('../middlewares/authMiddleware');
const UserLocation = require('../models/UserLocation');

// Update preferences
router.put('/preferences', authMiddleware, updatePreferences);

// Register
router.post('/signup', signUpUser);

// Login
router.post('/login', loginUser);

// Get user profile
router.get('/profile', authMiddleware, getUserProfile);

// Get privacy settings
router.get('/privacy', authMiddleware, getPrivacySettings);

// Save or update privacy settings
router.put('/privacy', authMiddleware, savePrivacySettings);

// ✅ Update user location (GeoJSON format)
router.put('/location/:id', async(req, res) => {
    const { id } = req.params;
    const { lat, lng } = req.body;

    try {
        await UserLocation.findOneAndUpdate({ userId: id }, {
            userId: id,
            location: {
                type: 'Point',
                coordinates: [lng, lat] // GeoJSON: [longitude, latitude]
            },
            updatedAt: new Date()
        }, { upsert: true, new: true });
        res.sendStatus(200);
    } catch (error) {
        console.error('❌ Error saving location:', error);
        res.sendStatus(500);
    }
});

// ✅ Connected users nearby (based on location and last update)
router.get('/nearby-users/:id', async(req, res) => {
    const { id } = req.params;
    try {
        const currentUser = await UserLocation.findOne({ userId: id });
        if (!currentUser) return res.status(404).json({ message: 'User location not found' });

        const [lng, lat] = currentUser.location.coordinates;
        const timeLimit = new Date(Date.now() - 10 * 60 * 1000); // 10 minutes ago

        const nearbyUsers = await UserLocation.find({
            userId: { $ne: id },
            updatedAt: { $gte: timeLimit },
            location: {
                $near: {
                    $geometry: { type: "Point", coordinates: [lng, lat] },
                    $maxDistance: 1000 // 1 km radius
                }
            }
        });

        res.json(nearbyUsers);
    } catch (err) {
        console.error('❌ Error fetching nearby users:', err);
        res.status(500).json({ message: 'Server error' });
    }
});

module.exports = router;