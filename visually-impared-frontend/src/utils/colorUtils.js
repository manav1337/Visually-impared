// src/utils/colorUtils.js

// Function to determine contrasting text color (black or white)
export const getContrastingTextColor = (bgColor) => {
  // Convert hex to RGB
  const hex = bgColor.replace('#', '');
  const r = parseInt(hex.substring(0, 2), 16);
  const g = parseInt(hex.substring(2, 4), 16);
  const b = parseInt(hex.substring(4, 6), 16);
  
  // Calculate luminance
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.5 ? '#000000' : '#FFFFFF';
};

// Function to adjust colors for accessibility
export const adjustColorForAccessibility = (color, isBackground = false) => {
  // Add your actual color adjustment logic here
  // This is a placeholder implementation
  if (isBackground) {
    return color; // Return modified background color
  }
  return getContrastingTextColor(color); // Return contrasting text color
};