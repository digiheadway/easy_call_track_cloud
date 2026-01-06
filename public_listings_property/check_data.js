
const API_BASE = 'https://prop.digiheadway.in/api/dealer_network';
const AUTH_TOKEN = '0cb564624a871dcde498cbb908ceda48';

async function fetchProperties() {
    const response = await fetch(
        `${API_BASE}/fetch.php?list=others&page=1&limit=100`,
        {
            headers: {
                accept: 'application/json, text/plain, */*',
                authorization: `Bearer ${AUTH_TOKEN}`,
            },
        }
    );
    const data = await response.json();
    const withLocation = data.data.find(p => p.location !== null || p.landmark_location !== null);
    console.log(JSON.stringify(withLocation, null, 2));
}

fetchProperties();
