import { useEffect, useState } from 'react';
import '../styles/components/preview.css';
import { generateErrorHtml } from '../utils/corsProxy';
import { getContrastingTextColor, adjustColorForAccessibility } from '../utils/colorUtils';

function PreviewPane({ url, analysisData, accessibilitySettings }) {
  const [transformedHtml, setTransformedHtml] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Function to apply accessibility transformations
  const applyTransformations = (html) => {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    
    // Apply font size
    const fontSizeMap = {
      small: '14px',
      medium: '16px',
      large: '18px',
      'x-large': '20px'
    };
    doc.body.style.fontSize = fontSizeMap[accessibilitySettings.fontSize] || '16px';
    
    // Apply link highlighting
    if (accessibilitySettings.highlightLinks) {
      const links = doc.querySelectorAll('a');
      links.forEach(link => {
        link.style.backgroundColor = 'yellow';
        link.style.color = 'black';
        link.style.padding = '2px';
        link.style.fontWeight = 'bold';
      });
    }
    
    // Apply color transformations
    const bgColor = analysisData.colorAnalysis?.primaryColors?.background?.value || '#ffffff';
    const textColor = analysisData.colorAnalysis?.primaryColors?.text?.value || '#000000';
    
    doc.body.style.backgroundColor = bgColor;
    doc.body.style.color = textColor;
    
    // Create accessibility override styles
    const style = doc.createElement('style');
    style.textContent = `
      * {
        background-color: ${bgColor} !important;
        color: ${textColor} !important;
      }
      
      a {
        color: ${adjustColorForAccessibility(textColor, 'text')} !important;
        text-decoration: underline !important;
      }
      
      :focus {
        outline: 3px solid ${adjustColorForAccessibility(textColor, 'text')} !important;
        outline-offset: 2px !important;
      }
    `;
    doc.head.appendChild(style);
    
    return doc.documentElement.outerHTML;
  };

  const openTransformedWebsite = () => {
    if (!transformedHtml) return;
    
    const newWindow = window.open('', '_blank');
    newWindow.document.write(transformedHtml);
    newWindow.document.close();
    
    // Apply text-to-speech if enabled
    if (accessibilitySettings.textToSpeech) {
      newWindow.addEventListener('DOMContentLoaded', () => {
        const speak = (text) => {
          const utterance = new SpeechSynthesisUtterance(text);
          utterance.lang = 'en-US';
          window.speechSynthesis.speak(utterance);
        };
        
        // Speak the page title
        speak(newWindow.document.title);
        
        // Speak when elements are focused
        newWindow.document.body.addEventListener('focus', (e) => {
          if (e.target.ariaLabel) {
            speak(e.target.ariaLabel);
          } else if (e.target.innerText) {
            speak(e.target.innerText);
          }
        }, true);
      });
    }
  };

  useEffect(() => {
    if (url && analysisData) {
      setLoading(true);
      setError(null);
      
      const fetchAndTransform = async () => {
        try {
          const proxyUrl = `/api/accessibility/proxy?url=${encodeURIComponent(url)}`;
          const response = await fetch(proxyUrl);
          
          if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Proxy error (${response.status}): ${errorText}`);
          }
          
          const html = await response.text();
          const transformed = applyTransformations(html);
          setTransformedHtml(transformed);
        } catch (error) {
          console.error('Error fetching content:', error);
          setError(error.message);
          setTransformedHtml(generateErrorHtml(error.message));
        } finally {
          setLoading(false);
        }
      };
      
      fetchAndTransform();
    } else {
      setTransformedHtml('');
    }
  }, [url, analysisData, accessibilitySettings]);

  return (
    <section className="preview-pane">
      <h2>Accessible Website</h2>
      
      {loading && (
        <div className="loading-indicator">
          <div className="spinner"></div>
          <p>Applying accessibility transformations...</p>
        </div>
      )}
      
      {error && (
        <div className="error-message">
          <p>{error}</p>
        </div>
      )}
      
      {transformedHtml && !loading && (
        <div className="access-container">
          <button 
            className="open-website-btn"
            onClick={openTransformedWebsite}
            aria-label="Open accessible version in new tab"
            disabled={loading}
          >
            Open Accessible Website
          </button>
          
          <div className="preview-note">
            <p>This version includes:</p>
            <ul>
              <li>Enhanced color contrast</li>
              <li>Font size: {accessibilitySettings.fontSize}</li>
              <li>Link highlighting: {accessibilitySettings.highlightLinks ? 'On' : 'Off'}</li>
              <li>Text-to-speech: {accessibilitySettings.textToSpeech ? 'Enabled' : 'Disabled'}</li>
            </ul>
          </div>
        </div>
      )}
      
      {!transformedHtml && !loading && !error && (
        <p className="placeholder">Enter a URL to generate accessible version</p>
      )}
    </section>
  );
}

export default PreviewPane;