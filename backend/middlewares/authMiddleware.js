const jwt = require('jsonwebtoken');
const User = require('../models/User');

const authMiddleware = async(req, res, next) => {
    const authHeader = req.header('Authorization');

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ message: 'No token provided' });
    }

    const token = authHeader.replace('Bearer ', '');

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        const user = await User.findById(decoded.id).select('-password');

        if (!user) {
            return res.status(404).json({ message: 'User not found' });
        }

        req.user = user; // שמור את המשתמש לבקשה
        next();
    } catch (err) {
        res.status(401).json({ message: 'Invalid token', error: err.message });
    }
};

module.exports = authMiddleware;