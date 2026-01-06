export function formatPhoneNumber(phone: string): string {
  // Allow + at the start, remove other non-digits
  const hasPlus = phone.startsWith('+');
  const cleaned = phone.replace(/\D/g, '');

  // If 12 digits and starts with 91, strip 91 (common Indian format)
  if (cleaned.length === 12 && cleaned.startsWith('91')) {
    return cleaned.slice(2); // return last 10 digits
  }

  // Allow 6 to 15 digits
  if (cleaned.length >= 6 && cleaned.length <= 15) {
    return hasPlus ? '+' + cleaned : cleaned;
  }

  return ''; // invalid case returns empty string
}

export function isValidPhoneNumber(phone: string): boolean {
  const cleaned = phone.replace(/\D/g, '');
  return cleaned.length >= 6 && cleaned.length <= 15;
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

export function getWhatsAppUrl(phone: string): string {
  let cleanPhone = phone.trim();
  // Check if it starts with +
  if (!cleanPhone.startsWith('+')) {
    cleanPhone = '+91' + cleanPhone;
  }
  // Remove + and other non-digits for the actual URL
  const digits = cleanPhone.replace(/\D/g, '');
  return `https://wa.me/${digits}`;
}