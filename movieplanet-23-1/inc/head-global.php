<?php
include 'php/function.php';
?>
<link
  href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap"
  rel="stylesheet"
/>

<meta charset="UTF-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<link rel="stylesheet" href="/assets/css/style.css?v=<?php echo filemtime($_SERVER['DOCUMENT_ROOT'] . '/assets/css/style.css'); ?>" />
<link rel="icon" type="image/x-icon" href="/assets/img/favicon.png" />
<meta name="referrer" content="never" />
<meta name="referrer" content="no-referrer" />
<meta name="theme-color" content="#161616" />
<meta name="language" content="en" />
<meta name="distribution" content="global" />
<meta name="author" content="Contributors to Wikimedia projects" />
<link rel="canonical" href="<?php echo $canonical; ?>" />
<script src="/assets/js/scriptglobal.js?v<?php echo filemtime($_SERVER['DOCUMENT_ROOT'] . '/assets/js/scriptglobal.js'); ?>"></script>
 <?php
 // Start the session
 session_start();

 // Check if 'utm_source' is in the query string
 if (isset($_GET['utm_source'])) {
   // Get the value of 'utm_source'
   $utm_source = $_GET['utm_source'];

   // Check if the value matches 'helpsarkari' or 'pokipro'
   if ($utm_source == 'helpsarkari' || $utm_source == 'pokipro') {
     // Set the session variable 'known_user' to true
     $_SESSION['known_user'] = true;
   }
 }

 ?>
<!-- Google tag (gtag.js) -->
<script
  async
  src="https://www.googletagmanager.com/gtag/js?id=G-6NGMZYQ1WQ"
></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag() {
    dataLayer.push(arguments);
  }
  gtag("js", new Date());

  gtag("config", "G-6NGMZYQ1WQ");
  gtag("config", "G-TMCFD5ECR8");

  function record(event_name, event_info) {
    gtag("event", event_name, {
      event_info: event_info,
    });
    console.log(event_name);
  }
</script>

<!-- Notix Web Push -->
<script id="script">
  var s = document.createElement("script")
  s.src = "https://notix.io/ent/current/enot.min.js"
  s.onload = function (sdk) {
    sdk.startInstall(
      {
        "appId": "10052ebe563441000003bfa0d956e92",
        "loadSettings": true
      }
    )
  }
  document.head.append(s)
</script>
<script type="text/javascript">
    (function(c,l,a,r,i,t,y){
        c[a]=c[a]||function(){(c[a].q=c[a].q||[]).push(arguments)};
        t=l.createElement(r);t.async=1;t.src="https://www.clarity.ms/tag/"+i;
        y=l.getElementsByTagName(r)[0];y.parentNode.insertBefore(t,y);
    })(window, document, "clarity", "script", "jyrsvad036");
</script>
<?php
if ($fulldomain === 'harleywives.com'): ?>
            <script
              async
              src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-3167708670200886"
              crossorigin="anonymous"
            ></script>

            <script
              async
              src="https://securepubads.g.doubleclick.net/tag/js/gpt.js"
            ></script>
            <script>
              window.googletag = window.googletag || { cmd: [] };
              googletag.cmd.push(function () {
                googletag
                  .defineSlot(
                    "/22904121374/Test_Ad3",
                    [
                      [200, 200],
                      [336, 280],
                      [250, 250],
                      [300, 250],
                    ],
                    "div-gpt-ad-1683164921086-0"
                  )
                  .addService(googletag.pubads());
                googletag.pubads().enableSingleRequest();
                googletag.pubads().collapseEmptyDivs();
                googletag.enableServices();
              });


            </script>

<?php endif; ?>
