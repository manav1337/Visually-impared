import axios from 'axios';

const api = axios.create({
  baseURL: process.env.NODE_ENV === 'development' 
    ? 'http://localhost:8080' 
    : '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

export default {
  analyzeWebsite(url) {
    return api.post('/analyze', { url });
  },
  saveSettings(settings) {
    return api.post('/settings', settings);
  },
  // Add more API methods as needed
};