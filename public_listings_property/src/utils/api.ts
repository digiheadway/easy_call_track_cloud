import { PropertyResponse } from '../types/property';

const API_BASE = 'https://prop.digiheadway.in/api/dealer_network';
const AUTH_TOKEN = '0cb564624a871dcde498cbb908ceda48';

export const fetchProperties = async (
  page: number = 1,
  limit: number = 40
): Promise<PropertyResponse> => {
  const response = await fetch(
    `${API_BASE}/fetch.php?list=others&page=${page}&limit=${limit}`,
    {
      headers: {
        accept: 'application/json, text/plain, */*',
        authorization: `Bearer ${AUTH_TOKEN}`,
      },
    }
  );

  if (!response.ok) {
    throw new Error('Failed to fetch properties');
  }

  return response.json();
};
