import React, { useState } from 'react';
import PreviewPane from './PreviewPane';
import '../styles/components/results.css';

function AccessibilityResults({ data }) {
  // Extract the background and text colors from the analysis data
  const background = data.colorAnalysis?.primaryColors?.background?.value || '#ffffff';
  const textColor = data.colorAnalysis?.primaryColors?.text?.value || '#000000';
  const contrastRatio = data.colorAnalysis?.contrast?.ratio || 0;
  const wcagRating = data.colorAnalysis?.contrast?.rating || 'Unknown';
  
  // Add accessibilitySettings state
  const [accessibilitySettings, setAccessibilitySettings] = useState({
    fontSize: 'medium',
    highlightLinks: false,
    textToSpeech: false
  });

  return (
    <div className="results-container">
      <div className="results-grid">
        <section className="analysis-results">
          <h2>Accessibility Analysis</h2>
          
          <div className="result-card contrast-card">
            <h3>Color Contrast</h3>
            <div className="color-samples">
              <div className="color-sample" style={{ backgroundColor: background }}>
                <span>Background: {background}</span>
              </div>
              <div className="color-sample" style={{ 
                backgroundColor: textColor, 
                color: background 
              }}>
                <span>Text: {textColor}</span>
              </div>
            </div>
            <p>Contrast Ratio: {contrastRatio}</p>
            <p className={`compliance ${wcagRating.toLowerCase()}`}>
              WCAG Rating: {wcagRating}
            </p>
          </div>

          <div className="result-card recommendations-card">
            <h3>Recommendations</h3>
            <ul>
              {data.accessibility?.warnings?.map((warning, index) => (
                <li key={index}>{warning}</li>
              ))}
            </ul>
          </div>
        </section>

        {/* Updated PreviewPane with new props */}
        <PreviewPane 
          url={data.url} 
          analysisData={data} // Add this prop to pass the analysis data
          accessibilitySettings={{
            ...accessibilitySettings,
            onFontSizeChange: (size) => setAccessibilitySettings(prev => ({...prev, fontSize: size})),
            onHighlightLinksChange: (checked) => setAccessibilitySettings(prev => ({...prev, highlightLinks: checked})),
            onTextToSpeechChange: (checked) => setAccessibilitySettings(prev => ({...prev, textToSpeech: checked}))
          }} 
        />
      </div>
    </div>
  );
}

export default AccessibilityResults;