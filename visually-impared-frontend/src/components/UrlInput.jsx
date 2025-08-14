import { useState } from 'react';
import axios from 'axios';
import '../styles/components/urlinput.css';

function UrlInput({ onAnalysisComplete }) {
  const [url, setUrl] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyzeWebsite = async () => {
  try {
    const response = await axios.get(`/api/colors?url=${encodeURIComponent(url)}`, {
      timeout: 10000 // 10 second timeout
    });
    
    if (!response.data) {
      throw new Error('Empty response from server');
    }
    
    onAnalysisComplete(response.data);
  } catch (err) {
    console.error('Full error details:', {
      message: err.message,
      response: err.response?.data,
      status: err.response?.status
    });
    
    setError(err.response?.data?.message || 
            'Failed to analyze website. Please check the URL and try again.');
  }
};

  return (
    <div className="url-input-container">
      <div className="input-group">
        <input
          type="url"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="https://example.com"
          aria-label="Website URL to analyze"
          disabled={isLoading}
        />
        <button 
          onClick={analyzeWebsite}
          disabled={!url || isLoading}
          aria-busy={isLoading}
        >
          {isLoading ? 'Analyzing...' : 'Analyze'}
        </button>
      </div>
      {error && <p className="error-message">{error}</p>}
    </div>
  );
}

export default UrlInput;