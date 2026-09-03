import { useState } from 'react';
import '../styles/components/customization.css';

function CustomizationPanel() {
  const [settings, setSettings] = useState({
    fontSize: 'medium',
    contrastMode: 'default',
    highlightLinks: true,
    textToSpeech: false
  });

  const handleSettingChange = (setting, value) => {
    setSettings(prev => ({
      ...prev,
      [setting]: value
    }));
    // Here you would also send these settings to the backend
    // or apply them to the preview pane
  };

  return (
    <section className="customization-panel">
      <h2>Accessibility Adjustments</h2>
      
      <div className="setting-group">
        <label>
          Font Size:
          <select 
            value={settings.fontSize}
            onChange={(e) => handleSettingChange('fontSize', e.target.value)}
          >
            <option value="small">Small</option>
            <option value="medium">Medium</option>
            <option value="large">Large</option>
            <option value="x-large">Extra Large</option>
          </select>
        </label>
      </div>

      <div className="setting-group">
        <label>
          Contrast Mode:
          <select 
            value={settings.contrastMode}
            onChange={(e) => handleSettingChange('contrastMode', e.target.value)}
          >
            <option value="default">Default</option>
            <option value="high-contrast">High Contrast</option>
            <option value="dark-mode">Dark Mode</option>
            <option value="light-mode">Light Mode</option>
          </select>
        </label>
      </div>

      <div className="setting-group checkbox-group">
        <label>
          <input 
            type="checkbox" 
            checked={settings.highlightLinks}
            onChange={(e) => handleSettingChange('highlightLinks', e.target.checked)}
          />
          Highlight Links
        </label>
        
        <label>
          <input 
            type="checkbox" 
            checked={settings.textToSpeech}
            onChange={(e) => handleSettingChange('textToSpeech', e.target.checked)}
          />
          Enable Text-to-Speech
        </label>
      </div>
    </section>
  );
}

export default CustomizationPanel;