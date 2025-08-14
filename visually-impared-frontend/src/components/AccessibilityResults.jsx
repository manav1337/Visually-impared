import CustomizationPanel from './CustomizationPanel';
import PreviewPane from './PreviewPane';
import '../styles/components/results.css';

function AccessibilityResults({ data }) {
  return (
    <div className="results-container">
      <div className="results-grid">
        <section className="analysis-results">
          <h2>Accessibility Analysis</h2>
          
          <div className="result-card contrast-card">
            <h3>Color Contrast</h3>
            <div className="color-samples">
              <div className="color-sample" style={{ backgroundColor: data.background }}>
                <span>Background: {data.background}</span>
              </div>
              <div className="color-sample" style={{ 
                backgroundColor: data.textColor, 
                color: data.background 
              }}>
                <span>Text: {data.textColor}</span>
              </div>
            </div>
            <p>Contrast Ratio: {data.contrastRatio}</p>
            <p className={`compliance ${data.wcagCompliance.toLowerCase()}`}>
              WCAG Compliance: {data.wcagCompliance}
            </p>
          </div>

          <div className="result-card recommendations-card">
            <h3>Recommendations</h3>
            <ul>
              {data.recommendations.map((item, index) => (
                <li key={index}>{item}</li>
              ))}
            </ul>
          </div>
        </section>

        <CustomizationPanel />
        <PreviewPane url={data.url} />
      </div>
    </div>
  );
}

export default AccessibilityResults;