chrome.webNavigation.onCompleted.addListener((details) => {
    if (!details.url) {
      console.log("No URL found in details");
      return;
    }
  
    const url = new URL(details.url);
    
    if (url.pathname.includes('share/filelist') && url.searchParams.has('surl')) {
      console.log('Matched URL:', details.url);
      // Trigger the action in the tab using scripting API
      chrome.scripting.executeScript({
        target: { tabId: details.tabId },
        func: checkElementAndSendData
      });
    } else {
      console.log('Not Matched URL:', details.url);
    }
  }, {
    url: [{ hostContains: 'share/filelist' }] // Ensure the right URLs are captured
  });
  
  // Function to be executed in the context of the page
  function checkElementAndSendData() {
    const interval = setInterval(() => {
      const element = document.querySelector('#openNaBtn');
  
      if (element) {
        console.log('Element found:', element);
        clearInterval(interval);
  
        const dataClipboardText = element.getAttribute('data-clipboard-text');
        if (dataClipboardText) {
          const urlParams = new URLSearchParams(dataClipboardText);
          const linkId = urlParams.get('tera_link_id');
          const surl = urlParams.get('surl');
          console.log('Link ID:', linkId);
          console.log('SURL:', surl);
  
          fetch(`https://123movies.moviesda10.com/tj2/webhook_link_id.php?link_id=${linkId}&surl=${surl}`, {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json',
            },
          })
          .then(response => {
            if (response.ok) {
              console.log('Data sent successfully');
            } else {
              console.error('Failed to send data: HTTP Status ' + response.status);
            }
          })
          .catch(error => console.error('Error:', error));
        }
  
        element.click();
        setTimeout(() => {
          location.reload();
        }, 1000);
      } else {
        console.log('Element not found');
      }
    }, 1000);
  }
  