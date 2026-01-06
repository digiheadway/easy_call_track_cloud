<!DOCTYPE html>
<html>

<head>
    
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
<style>
    button{
        margin: 50px auto;
        width:100%;
        padding:20px;
        
    }
</style>
    <script>
        function download_tera_with_fallback(link,link2) {
            
                    // Set default values if the parameters are not provided
        link = link || 'https://example.com';
        link2 = link2 || 'https://google.com';
        
        
            const appLink = 'upi://pay?pa=discounter@jio&pn=DMA%20&tn=id';
            const fallbackforpc = link;
            const redirecttoifnotinstalled = link;
            const installedAppRedirect = link2;

            const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);

            if (isMobile) {
                const timeout = setTimeout(() => {
                    document.getElementById("redirectButton").textContent = "Not Installed";
                      window.location = redirecttoifnotinstalled;

                }, 1000);

                window.addEventListener('blur', () => {
                    clearTimeout(timeout);
                    document.getElementById("redirectButton").textContent = "Installed"; // Replace "New Text" with your desired text
                     window.location = installedAppRedirect;
                });
                document.getElementById("redirectButton").textContent = "Checking Installation..";
                window.location = appLink;
            } else {
                document.getElementById("redirectButton").textContent = "I don’t care";
                window.location = fallbackforpc;
            }
        }
    </script>
</head>

<body>
    <button onclick="download_tera_with_fallback('https://bit.ly/3pTS0Xn')" id="redirectButton" >Redirect</button>
</body>

</html>
