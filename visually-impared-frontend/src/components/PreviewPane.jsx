import { useEffect, useRef } from 'react';
import '../styles/components/preview.css';

function PreviewPane({ url, settings }) {
  const iframeRef = useRef(null);

  useEffect(() => {
    if (iframeRef.current && url) {
      // Add settings as query params
      const params = new URLSearchParams({
        url,
        contrastMode: settings.contrastMode,
        fontSize: settings.fontSize
      });
      
      iframeRef.current.src = `/api/proxy/accessible-view?${params.toString()}`;
    }
  }, [url, settings]);

  return (
    <section className="preview-pane">
      <h2>Website Preview</h2>
      <div className="iframe-container">
        {url ? (
          <iframe
            ref={iframeRef}
            title="Website Preview"
            sandbox="allow-same-origin allow-scripts"
            aria-label="Preview of the analyzed website"
            allow="fullscreen"
          />
        ) : (
          <p className="placeholder">Enter a URL to see preview</p>
        )}
      </div>
    </section>
  );
}

export default PreviewPane;