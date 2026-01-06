function checkElementAndSendData() {
    const interval = setInterval(() => {
      // Locate the element by ID
      const element = document.querySelector('#openNaBtn');
  
      if (element) {
        // Stop the interval
        clearInterval(interval);
  
        // Get the value of the data-clipboard-text attribute
        const dataClipboardText = element.getAttribute('data-clipboard-text');
  
        if (dataClipboardText) {
          // Extract link_id and surl using URLSearchParams
          const urlParams = new URLSearchParams(dataClipboardText);
          const linkId = urlParams.get('tera_link_id');
          const surl = urlParams.get('surl');
  
          // Log extracted values
          console.log('Link ID:', linkId);
          console.log('SURL:', surl);
  
          // Send the data to the webhook
          fetch(
            `https://123movies.moviesda10.com/tj2/webhook_link_id.php?link_id=${linkId}&surl=${surl}`,
            {
              method: 'GET',
              headers: {
                'Content-Type': 'application/json',
              },
            }
          )
            .then((response) => {
              if (response.ok) {
                console.log('Data sent successfully:', dataClipboardText);
              } else {
                console.error('Failed to send data');
              }
            })
            .catch((error) => console.error('Error:', error));
        }
  
        // Click the button
        element.click();
  
        // Refresh the page after 2 seconds
        setTimeout(() => {
          location.reload();
        }, 1000);
      }
    }, 1000); // Run every 1 second
  }
  
  // Call the function
  checkElementAndSendData();
  