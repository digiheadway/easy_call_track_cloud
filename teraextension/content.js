// Check the URL to see if it matches the required pattern
if (window.location.href.includes('share/filelist?surl=')) {
    // alert('Matched URL: ' + window.location.href); // Alert if URL matches
    checkElementAndSendData(); // Run the function if matched
} else {
    // alert('Not Matched URL: ' + window.location.href); // Alert if URL does not match
}

function checkElementAndSendData() {
    const interval = setInterval(() => {
        const element = document.querySelector('#openNaBtn');

        if (element) {
            console.log('Element found:', element); // Log element if found
            clearInterval(interval); // Stop the interval

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
                        console.error('Failed to send data');
                    }
                })
                .catch(error => console.error('Error:', error));
            }

            element.click();

            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            console.log('Element not found'); // Log if element isn't found
        }
    }, 1000);
}

setInterval(() => {
    document.dispatchEvent(new MouseEvent('mousemove', {
        bubbles: true,
        cancelable: true,
    }));
}, 1000);
