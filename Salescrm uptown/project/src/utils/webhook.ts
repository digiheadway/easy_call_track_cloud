import { Lead } from '../types/lead';

const WEBHOOK_BASE_URL = 'https://prop.digiheadway.in/api/create_contact.php?source=sales&';

// Fields that trigger webhook when updated
const TRACKED_FIELDS = [
  // Personal section
  'name',
  'phone',
  'address',
  'about_him',
  'alternative_contact_details',
  
  // Requirements section
  'requirement_description',
  'budget',
  'preferred_type',
  'purposes',
  'preferred_area'
] as const;

export const sendWebhook = async (lead: Partial<Lead>) => {
  try {
    const webhookUrl = new URL(WEBHOOK_BASE_URL);
    
    // Always include lead_id if available
    if (lead.id) {
      webhookUrl.searchParams.append('lead_id', lead.id.toString());
    }

    // Convert all values to strings, excluding null/undefined/empty values
    const cleanPayload = Object.entries(lead).reduce((acc, [key, value]) => {
      // Skip null, undefined, empty strings, and empty arrays
      if (value === null || value === undefined || value === '' || 
         (Array.isArray(value) && value.length === 0)) {
        return acc;
      }

      // Convert all values to strings, except keep id as number
      if (key === 'id') {
        acc[key] = value;
      } else if (Array.isArray(value)) {
        acc[key] = value.join(',');
      } else if (typeof value === 'boolean') {
        acc[key] = value ? '1' : '0';
      } else if (value instanceof Date) {
        acc[key] = value.toISOString();
      } else {
        acc[key] = value.toString();
      }

      return acc;
    }, {} as Record<string, any>);

    console.log('Sending webhook with payload:', cleanPayload);

    const response = await fetch(webhookUrl.toString(), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(cleanPayload),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(
        `Webhook failed with status: ${response.status}${
          errorData ? ` - ${JSON.stringify(errorData)}` : ''
        }`
      );
    }

    const responseData = await response.json().catch(() => null);
    console.log('Webhook sent successfully:', responseData);

    return true;
  } catch (error) {
    console.error('Webhook error:', error);
    throw error; // Re-throw to handle in the component
  }
};

export const shouldTriggerWebhook = (
  field: keyof Lead,
  isNewLead: boolean
): boolean => {
  // Always trigger for new leads
  if (isNewLead) return true;
  
  // For updates, only trigger for tracked fields
  return TRACKED_FIELDS.includes(field as typeof TRACKED_FIELDS[number]);
};