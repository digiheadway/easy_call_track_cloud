export function formatPhoneNumber(phone: string): string {
  const cleaned = phone.replace(/\D/g, '');

  // If 12 digits and starts with 91, strip +91
  if (cleaned.length === 12 && cleaned.startsWith('91')) {
    return cleaned.slice(2); // return last 10 digits
  }

  // If already 10-digit number
  if (cleaned.length === 10) {
    return cleaned;
  }

  return ''; // invalid case returns empty string
}

export function isValidPhoneNumber(phone: string): boolean {
  const cleaned = phone.replace(/\D/g, '');
  return cleaned.length === 10 && /^[6-9]/.test(cleaned); // Indian numbers only
}


export async function getClipboardText(): Promise<string> {
  try {
    return await navigator.clipboard.readText();
  } catch (error) {
    console.error('Failed to read clipboard:', error);
    return '';
  }
}

export async function setClipboardText(text: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(text);
  } catch (error) {
    console.error('Failed to write to clipboard:', error);
  }
}