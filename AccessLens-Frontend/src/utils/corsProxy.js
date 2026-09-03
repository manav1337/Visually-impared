export const corsProxy = (url) => {
  return `https://api.allorigins.win/raw?url=${encodeURIComponent(url)}`;
};

export function generateErrorHtml(error) {
  return `
    <div class="error" style="
      background: #FF4A3;
      color: #2D3748;
      padding: 1rem;
      border-radius: 4px;
      margin: 1rem 0;
      font-family: sans-serif;
    ">
      <strong>Web Accessibility Tool Error:</strong>
      <div>${error.message || 'Unknown error'}</div>
    </div>
  `;
}