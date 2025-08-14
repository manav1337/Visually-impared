import { useState } from 'react';
import UrlInput from './components/UrlInput';
import AccessibilityResults from './components/AccessibilityResults';
import './App.css';

function App() {
  const [results, setResults] = useState(null);

  return (
    <div className="app-container">
      <header>
        <h1>Web Accessibility Tool</h1>
      </header>
      <main>
        <UrlInput onAnalysisComplete={setResults} />
        {results && <AccessibilityResults data={results} />}
      </main>
    </div>
  );
}

export default App;