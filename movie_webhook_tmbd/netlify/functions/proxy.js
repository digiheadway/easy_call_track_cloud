const fetch = require("node-fetch");

exports.handler = async (event) => {
  try {
    // Extract the request path to forward to TMDb
    const tmdbPath = event.path.replace("/.netlify/functions/proxy", "");

    // Define the full URL to forward the request to
    const tmdbApiUrl = `https://api.themoviedb.org/3${tmdbPath}`;

    // Forward the request with the same method, headers, and body
    const response = await fetch(tmdbApiUrl, {
      method: event.httpMethod,
      headers: {
        ...event.headers,
        Authorization: `Bearer YOUR_TMDB_API_TOKEN`, // Replace with your actual TMDb token
      },
      body: event.httpMethod === "POST" ? event.body : undefined,
    });

    // Get the response body from TMDb
    const responseBody = await response.text();

    return {
      statusCode: response.status,
      body: responseBody,
      headers: {
        "Content-Type":
          response.headers.get("content-type") || "application/json",
      },
    };
  } catch (error) {
    return {
      statusCode: 500,
      body: JSON.stringify({
        error: "Internal Server Error",
        details: error.message,
      }),
    };
  }
};
