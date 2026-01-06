<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Link Action</title>
    <script async src="https://www.googletagmanager.com/gtag/js?id=UA-XXXXX-Y"></script>
    <style>
        html,
        body {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 90vh;
            display: flex;
            flex-direction: row;
            justify-content: center;
            align-items: center;
            background: #000;
        }

        .glow-on-hover {
            width: 220px;
            height: 50px;
            border: none;
            outline: none;
            color: #fff;
            background: #111;
            cursor: pointer;
            position: relative;
            z-index: 0;
            border-radius: 10px;

        }

        .glow-on-hover:before {
            content: '';
            background: linear-gradient(45deg, #ff0000, #ff7300, #fffb00, #48ff00, #00ffd5, #002bff, #7a00ff, #ff00c8, #ff0000);
            position: absolute;
            top: -2px;
            left: -2px;
            background-size: 400%;
            z-index: -1;
            filter: blur(5px);
            width: calc(100% + 4px);
            height: calc(100% + 4px);
            animation: glowing 20s linear infinite;
            opacity: 1;
            transition: opacity .3s ease-in-out;
            border-radius: 10px;
        }

        .glow-on-hover:active {
            color: #000
        }

        .glow-on-hover:active:after {
            background: transparent;
        }

        .glow-on-hover:hover:before {
            opacity: 1;
        }

        .glow-on-hover:after {
            z-index: -1;
            content: '';
            position: absolute;
            width: 100%;
            height: 100%;
            background: #111;
            left: 0;
            top: 0;
            border-radius: 10px;
        }

        @keyframes glowing {
            0% {
                background-position: 0 0;
            }

            50% {
                background-position: 400% 0;
            }

            100% {
                background-position: 0 0;
            }
        }
    </style>
    <script>
        window.dataLayer = window.dataLayer || [];
        function gtag() { dataLayer.push(arguments); }
        gtag('js', new Date());
        gtag('config', 'UA-XXXXX-Y');  // Replace with your Google Analytics ID
    </script>
</head>

<body>
    <button id="action_btn" class="glow-on-hover" onclick="getLinkAndRedirect()">Download TeraBox 2</button>
</body>
<script>
    const tera_main_link = "https://g.com";
    function getLinkAndRedirect() {
    // Make an HTTP request to the webhook
    fetch('https://123movies.olamovies.in/tj2/get_link_id.php', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        // Check if link_id exists in the response
        if (data.link_id) {
            var link_id = data.link_id;
            // If a link_id is found, redirect to abc.com/{link}
            // window.location.href = `https://abc.com/?${data.link_id}`;
            var marketurl = `market://details?id=com.dubox.drive&referrer=utm_source=tera_sharelink&utm_campaign=header_button&surl=L2GEqKHfs9pzwGD9OQ3i0g&tera_link_id=${link_id}&tera_link_type=1&c=https://www.1024tera.com/wap/share/filelist?surl=L2GEqKHfs9pzwGD9OQ3i0g&af_dp=dubox://share_detail?shareid=58712493527&uk=4398511686747&share_create_time=1734217962&randsk=brxzEUa2yGI6EcWGDP7XmhzSzTmSpCxEVB1x%20viMXdU=&share_expired_time=0&source=af_link&share_file_count=1&surl=L2GEqKHfs9pzwGD9OQ3i0g&agency=j1__0`;
            var textToCopy = `&surl=L2GEqKHfs9pzwGD9OQ3i0g&tera_link_id=${link_id}&tera_link_type=1`;
            navigator.clipboard.writeText(textToCopy);
            window.location.href = marketurl;
        } else {
        window.open(tera_main_link, "_blank");
    }
    })
    .catch(error => {
        // If there's an error with the request, redirect to g.com
        console.error('Error fetching link_id:', error);
        window.open(tera_main_link, "_blank");

    });
}

</script>
</html>