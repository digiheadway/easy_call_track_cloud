<?php


function isBotDetected()
{

  if (preg_match('/abacho|accona|AddThis|AdsBot|ahoy|AhrefsBot|AISearchBot|alexa|altavista|anthill|appie|applebot|arale|araneo|AraybOt|ariadne|arks|aspseek|ATN_Worldwide|Atomz|baiduspider|baidu|bbot|bingbot|bing|Bjaaland|BlackWidow|BotLink|bot|boxseabot|bspider|calif|CCBot|ChinaClaw|christcrawler|CMC\/0\.01|combine|confuzzledbot|contaxe|CoolBot|cosmos|crawler|crawlpaper|crawl|curl|cusco|cyberspyder|cydralspider|dataprovider|digger|DIIbot|DotBot|downloadexpress|DragonBot|DuckDuckBot|dwcp|EasouSpider|ebiness|ecollector|elfinbot|esculapio|ESI|esther|eStyle|Ezooms|facebookexternalhit|facebook|facebot|fastcrawler|FatBot|FDSE|FELIX IDE|fetch|fido|find|Firefly|fouineur|Freecrawl|froogle|gammaSpider|gazz|gcreep|geona|Getterrobo-Plus|get|girafabot|golem|googlebot|\-google|grabber|GrabNet|griffon|Gromit|gulliver|gulper|hambot|havIndex|hotwired|htdig|HTTrack|ia_archiver|iajabot|IDBot|Informant|InfoSeek|InfoSpiders|INGRID\/0\.1|inktomi|inspectorwww|Internet Cruiser Robot|irobot|Iron33|JBot|jcrawler|Jeeves|jobo|KDD\-Explorer|KIT\-Fireball|ko_yappo_robot|label\-grabber|larbin|legs|libwww-perl|linkedin|Linkidator|linkwalker|Lockon|logo_gif_crawler|Lycos|m2e|majesticsEO|marvin|mattie|mediafox|mediapartners|MerzScope|MindCrawler|MJ12bot|mod_pagespeed|moget|Motor|msnbot|muncher|muninn|MuscatFerret|MwdSearch|NationalDirectory|naverbot|NEC\-MeshExplorer|NetcraftSurveyAgent|NetScoop|NetSeer|newscan\-online|nil|none|Nutch|ObjectsSearch|Occam|openstat.ru\/Bot|packrat|pageboy|ParaSite|patric|pegasus|perlcrawler|phpdig|piltdownman|Pimptrain|pingdom|pinterest|pjspider|PlumtreeWebAccessor|PortalBSpider|psbot|rambler|Raven|RHCS|RixBot|roadrunner|Robbie|robi|RoboCrawl|robofox|Scooter|Scrubby|Search\-AU|searchprocess|search|SemrushBot|Senrigan|seznambot|Shagseeker|sharp\-info\-agent|sift|SimBot|Site Valet|SiteSucker|skymob|SLCrawler\/2\.0|slurp|snooper|solbot|speedy|spider_monkey|SpiderBot\/1\.0|spiderline|spider|suke|tach_bw|TechBOT|TechnoratiSnoop|templeton|teoma|titin|topiclink|twitterbot|twitter|UdmSearch|Ukonline|UnwindFetchor|URL_Spider_SQL|urlck|urlresolver|Valkyrie libwww\-perl|verticrawl|Victoria|void\-bot|Voyager|VWbot_K|wapspider|WebBandit\/1\.0|webcatcher|WebCopier|WebFindBot|WebLeacher|WebMechanic|WebMoose|webquest|webreaper|webspider|webs|WebWalker|WebZip|wget|whowhere|winona|wlm|WOLP|woriobot|WWWC|XGET|xing|yahoo|YandexBot|YandexMobileBot|yandex|yeti|Zeus/i', $_SERVER['HTTP_USER_AGENT'])) {
    return true; // 'Above given bots detected'

  }

  return false;
} // End :: isBotDetected()

if (!isBotDetected()) {
  if (!$sn) {
    $sn = $_GET['sn'];
    if (!$sn) {
      $sn = $_COOKIE['sn'];
      if (!$sn) {
        $sn = "Pokipro";
      }
    }
  }
  $sn = ucwords($sn);
  setcookie("sn", $sn, time() + 2 * 24 * 60 * 60);
}
if (isBotDetected()) {
  $sn = "Pokipro";
}

?>
<html>

<head>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Importance of Health Insurance</title>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />

  <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-5104426911267208" crossorigin="anonymous" type="a2b6f2ccadb89523dc5c1dd6-text/javascript"></script>

  <!-- Global site tag (gtag.js) - Google Analytics -->
  <script async src="https://www.googletagmanager.com/gtag/js?id=UA-158287663-1" type="a2b6f2ccadb89523dc5c1dd6-text/javascript"></script>
  <script type="a2b6f2ccadb89523dc5c1dd6-text/javascript">
    window.dataLayer = window.dataLayer || [];

    function gtag() {
      dataLayer.push(arguments);
    }
    gtag('js', new Date());

    gtag('config', 'UA-158287663-1');
  </script>
  <!-- Google tag (gtag.js) -->
  <script async src="https://www.googletagmanager.com/gtag/js?id=G-XDLCL2193Z"></script>
  <script>
    window.dataLayer = window.dataLayer || [];

    function gtag() {
      dataLayer.push(arguments);
    }
    gtag('js', new Date());

    gtag('config', 'G-QSHDNY5VLD');
    gtag('config', 'G-XDLCL2193Z');
  </script>
  <script charset="UTF-8" src="//web.webpushs.com/js/push/0ef2574609392dcfa38619bf7473762a_1.js" async type="a2b6f2ccadb89523dc5c1dd6-text/javascript"></script>
  <!-- Facebook Pixel Code -->
  <script type="a2b6f2ccadb89523dc5c1dd6-text/javascript">
    ! function(f, b, e, v, n, t, s) {
      if (f.fbq) return;
      n = f.fbq = function() {
        n.callMethod ?
          n.callMethod.apply(n, arguments) : n.queue.push(arguments)
      };
      if (!f._fbq) f._fbq = n;
      n.push = n;
      n.loaded = !0;
      n.version = '2.0';
      n.queue = [];
      t = b.createElement(e);
      t.async = !0;
      t.src = v;
      s = b.getElementsByTagName(e)[0];
      s.parentNode.insertBefore(t, s)
    }(window, document, 'script',
      'https://connect.facebook.net/en_US/fbevents.js');
    fbq('init', '354981835460414');
    fbq('track', 'PageView');
  </script>
  <noscript><img height="1" width="1" style="display:none" src="https://www.facebook.com/tr?id=354981835460414&ev=PageView&noscript=1" /></noscript>
  <!-- End Facebook Pixel Code -->


  <!-- Global site tag (gtag.js) - Google Analytics -->
  <script async src="https://www.googletagmanager.com/gtag/js?id=UA-151945844-3" type="a2b6f2ccadb89523dc5c1dd6-text/javascript"></script>
  <script type="a2b6f2ccadb89523dc5c1dd6-text/javascript">
    window.dataLayer = window.dataLayer || [];

    function gtag() {
      dataLayer.push(arguments);
    }
    gtag('js', new Date());

    gtag('config', 'UA-151945844-3');
  </script>

  <!-- Global site tag (gtag.js) - Google Analytics -->
  <script async src="https://www.googletagmanager.com/gtag/js?id=G-YVJN4ZG7FK" type="a2b6f2ccadb89523dc5c1dd6-text/javascript"></script>
  <script type="a2b6f2ccadb89523dc5c1dd6-text/javascript">
    window.dataLayer = window.dataLayer || [];

    function gtag() {
      dataLayer.push(arguments);
    }
    gtag('js', new Date());

    gtag('config', 'G-YVJN4ZG7FK');
  </script>

  <!-- Google Tag Manager -->
  <script type="a2b6f2ccadb89523dc5c1dd6-text/javascript">
    (function(w, d, s, l, i) {
      w[l] = w[l] || [];
      w[l].push({
        'gtm.start': new Date().getTime(),
        event: 'gtm.js'
      });
      var f = d.getElementsByTagName(s)[0],
        j = d.createElement(s),
        dl = l != 'dataLayer' ? '&l=' + l : '';
      j.async = true;
      j.src =
        'https://www.googletagmanager.com/gtm.js?id=' + i + dl;
      f.parentNode.insertBefore(j, f);
    })(window, document, 'script', 'dataLayer', 'GTM-PH959KW');
  </script>
  <!-- End Google Tag Manager -->
  <style>
    body {
      color: #000000;
      font-size: 12px;
      font-family: verdana, "Comic Sans MS", Helvetica, sans-serif;
      background-color: #fff;
      margin: 0;
    }

    a:link,
    a:active,
    a:visited {
      color: red;
      text-decoration: none;
    }

    a:hover,
    a:focus {
      color: #000000;
    }

    img {
      border-style: none;
    }

    .cen {
      text-align: center;
    }


    p {
      margin: 4px;
      padding: 0 3px;
      font-family: HelveticaNeue-Light, Helvetica Neue Light, Helvetica Neue, Helvetica, Arial, sans-serif;
      border: 1px dotted #f4f4f4;
      font-size: 12px
    }

    .style24 {
      color: #CE3B3B;
    }

    .logovoom {
      text-align: center;
      color: #ffffff;
      background: #ffeaf5;
      padding: 2px;
    }

    .pink a {
      color: #fff;
      font-weight: bold;
    }

    .kaimvoom {
      background-color: #FFFF96;
      color: #333333;
      padding-top: 1px;
      padding-bottom: 1px;
      padding-left: 2px;
      padding-right: 2px;
      font-weight: normal;
      font-size: small;
      border-width: 1px;
      border-color: #e1e1ff;
      border-style: solid;
      margin-top: 1px;
      margin-bottom: 1px;
      margin-left: 0px;
      margin-right: 0px;
    }

    .subox {
      background-color: #094E84;
      border-bottom: 1px #094E84 solid;
      padding: 6px 6px;
      color: #ffffff;
      font-weight: bold;
      text-align: left;
    }

    .info {
      background-color: #FFD94E;
      border-bottom: 1px #FFD94E solid;
      padding: 6px 6px;
      color: #ffffff;
      font-weight: bold;
      text-align: left;
    }

    .dlcm {
      background-color: #FA80DB;
      border-bottom: 1px #FA80DB solid;
      padding: 6px 6px;
      color: #ffffff;
      font-weight: bold;
      text-align: left;
    }

    h1,
    h2 {
      color: black;
      background: #;
      border-top: 1px solid #;
      border-bottom: 1px solid #;
      font-size: 100%;
      font-weight: 900;
      padding: 0px;
      margin: 0;
    }

    .des h1,
    .des h2 {
      font-size: 18px;
      font-weight: 700;
    }


    .link {
      color: #EF0404;
      font-weight: bold;
      font-size: 13px;
      font-family: Arial, Helvetica, sans-serif;
      border-bottom: 1px solid #DDDDDD;
      padding: 5px;
    }

    .link:hover {
      background: #FAFCFC url(/files/images/arrow.jpg) no-repeat right;
    }

    .link a {
      color: #EF0404;

    }

    .link a:hover {
      color: #005D92;
      text-decoration: underline;
    }

    .footer {
      background-color: #4E3B3B;
      border-bottom: 1px #4E3B3B solid;
      padding: 7px 7px;
      color: #ffffff;
      font-weight: bold;
      text-align: center;
    }

    .ac,
    .ai,



    .ac img {
      border: 1px solid #fff;
      border-radius: 7px;
      height: 290px !important;
      margin: 2px 2px;
      width: 230px !important;
    }


    .ddt {
      padding: 3px;
      border-bottom: 1px dashed #d1d8df;
    }

    .download {
      padding: 3px;
      border-bottom: 1px dashed white;
    }

    .updates {
      background-color: #fafafa;
      border-bottom: 2px dashed #bbb;
      padding: 5px;
      font-weight: bold;
    }

    .updates a {
      color: #FF0000;
    }

    .search {
      background-color: #fade8b;
      color: red;
      border-bottom-width: 0px;
      border-bottom-color: #f6960d;
      border-bottom-style: solid;
      padding-top: 5px;
      padding-bottom: 5px;
      padding-left: 2px;
      font-weight: bold;
      font-size: small;
    }

    .pgn a {
      border: 1px solid #ccc;
      background: #f5f5f5;
      color: #FFFFFF;
    }

    .pgn a,
    .pgn span {
      border: 1px solid #0902C6;
      padding: 5px 5px;
      background: #0902C6;
      border-radius: 5px;
      text-decoration: none;
      font-weight: bold;
    }

    .pgn a:hover,
    .pgn span {
      background: #000000;
      color: #fff;
      border-color: #2565a4;
    }


    .dtype {
      color: #333333;
      padding: 5px 5px 5px 8px;
      background: #f4f4f4 url(/files/images/mast.png) repeat-x top;
      border: 1px solid #ccc;
      font-weight: bold;
      margin-top: 0px;
      text-transform: capitalize;
    }

    .pb1 {
      background-color: #FFFAFA;
      padding:
        5px;
      border-bottom: 1px #E4C9C6 solid;
    }

    .pb1:hover {
      background-color: #FDD6DF;
    }

    a.pb1 img {
      vertical-align: middle;
      bottom: 1px;
      margin: 0px 2px 4px 0px;
    }

    .navi {
      background-color: #F8D0FF;
      color: # 990000;
      border-width: 1px;
      border-color: # 8E8388;
      border-style: solid;
      margin-top: 0px;
      margin-bottom: margin-right: 0px;
      margin- left: 0px;
      padding-top: 9px;
      padding-right: 4px;
      padding-left: 4px;
      padding-bottom: 7px;
      font-weight: normal;
      font-size: normal;
    }

    .des {
      padding: 10px;
      background: #F8F8F8;
      margin: 10px;
      font-size: 5px !important;
    }

    .Fun {
      border-bottom: 1px dotted #cacaca;
      margin-bottom: 1px;
      padding: 4px;
    }

    .Blue {
      border-left: 4px solid #2006E0;
      color: #757575;
      margin-top: 2px;
      margin-left: 4px;
      padding-left: 3px;
    }

    .updates {
      background-color: #FFF8DC;
      border-bottom: 1px dashed #bbb;
      padding: 3px;
      font-weight: bold;
    }

    .updates a {
      color: #FF0000;
    }

    .mCover,
    .mInfo,
    .mCoverSmall {
      float: left;
    }

    .mCover img {
      border: 1px solid #ccc;
      border-radius: 7px;
      height: 188px !important;
      margin: 4px 2px;
      width: 170px !important;
    }

    .mCoverSmall img {
      width: 120px;
      height: 120px;
      border: solid 1px #fff;
    }

    .tag {
      background-color: #e8d7c7;
      padding: 4px;
      margin-top: 2px;
      margin-bottom: 2px;
      border-top-width: 1px;
      border-top-color: #a38e7a;
      border-top-style: solid;
    }

    .pgn {
      text-align: center;
      padding-top: 10px;
      background: #E6E6FA;
      border: 1px solid #abc;
    }

    .pgn a,
    .pgn span {
      border: 1px solid #ccc;
      padding: 2px 5px;
      margin: 0 2px;
      background: #ddd;
      -moz-border-radius: 3px;
      -webkit-border-radius: 3px;
      border-radius: 3px;
      text-decoration: none;
      font-weight: bold;
    }

    .pgn a:hover,
    .pgn span {
      background: #ddd;
      border-color: #ddd;
    }

    .pgn div {
      padding-top: 5px;
    }

    .headblack {
      background-color: #000000;
      border-bottom: 1px #000000 solid;
      padding: 6px 6px;
      color: #ffffff;
      font-weight: bold;
      text-align: left;
    }

    .up {
      color: #fff;
      position: relative;
      top: 11px;
      padding: 3px;
      background: url(/images/navis-red.gif) no-repeat;
      font-weight: bold;
    }

    .up1 {
      border-bottom: 1px dotted #cacaca;
      margin-bottom: 1px;
      padding: 4px;
    }

    .M2 {
      color: #960000;
      background: #ffffff;
      border-bottom: 1px solid #ede9dc;
      text-decoration: none;
      font-weight: bold;
      text-align: left;
    }

    .M2 a {
      display: block;
      padding: 7px;
    }

    .M2: hover {
      background-color: #e6e6e6;
    }

    .M1 {
      color: #ff0000;
      background: #ffffff;
      border-bottom: 1px solid #ede9dc;
      text-decoration: none;
      font-weight: bold;
      text-align: left;
    }

    .M1 a {
      display: block;
      padding: 5px;
    }

    .M1:hover {
      background-color: #FEF6C4;
    }

    .M2 {
      color: #960000;
      background: #eeecec url(/images/arrow.gif) no-repeat right;
      border-bottom: 1px solid #ede9dc;
      text-decoration: none;
      font-weight: bold;
      text-align: left;
    }

    .M2 a {
      display: block;
      padding: 5px;
    }

    .M2:hover {
      background: #FEF6C4 url(/images/arrow.gif) no-repeat right;
    }

    .M2 img {
      vertical-align: middle;
      bottom: 1px;
      margin: 0px 2px 4px 0px;
    }

    .M1 img {
      vertical-align: middle;
      bottom: 1px;
      margin: 0px 2px 4px 0px;
    }


    .M1 {
      background-color: #fafafa;
      padding:
        4px;
      border-bottom: 1px #ecd5d2 solid;
    }

    .M1 img {
      vertical-align: middle;
      bottom: 1px;
      margin: 0px 2px 4px 0px;
    }

    .mylist {
      color: #EF0404;
      font-weight: bold;
      font-size: 12px;
      font-family: Arial, Helvetica, sans-serif;
      border-bottom: 1px solid #DDDDDD;
      padding: 5px;
    }

    .mylist:hover {
      background: #FAFCFC url(/images/arrow.jpg) no-repeat right;
    }

    .mylist a {
      color: #EF0404;

    }

    .mylist a:hover {
      color: #005D92;
      text-decoration: underline;
    }

    .x2 a {
      padding: 3px 3px;
      text-align: center;
      font-size: 12px;
      margin: 1px 1px;
      background: #0A39D7;
      color: #FFF;
      border-radius: 4px;
      transition-duration: 0.2s;
      cursor: pointer;
      margin: 2px
    }

    .x2 a:hover {
      border: 2px solid #0A39D7;
      background: #C0C6C6;
      color: black;
    }
  </style>
</head>

<body>
  <center>
    <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-5104426911267208" crossorigin="anonymous" type="60401fed773cd9f75e65b2c3-text/javascript"></script>
    <!-- Go -->
    <ins class="adsbygoogle" style="display:block" data-ad-client="ca-pub-5104426911267208" data-ad-slot="7351088471" data-ad-format="auto" data-full-width-responsive="true"></ins>
    <script type="60401fed773cd9f75e65b2c3-text/javascript">
      (adsbygoogle = window.adsbygoogle || []).push({});
    </script>
  </center>

  <a href="?src=header">
    <div align="center">
      <h1 style="font-size:50px;color:red;"><?php echo $sn; ?></h2>
    </div>
  </a>
  <center>
    <div align="center" style="color:green; font-weight:600;">New Look</div>

    <br />

  </center>
  <?php if (!isBotDetected()) {
    echo '
<div class="search"><br><form action="searchresult.php" method="POST">Search  : <input type="text" name="query" id="find" value="" size="10" />
<input type="hidden" name="per_page" value="1" />
<input type="submit" value="Search" /></form></div>
           </div></div>
           ';
  } ?>
  <h2 class="subox">
    <font color="white"><b>Select Link Content Type?</b></font>
  </h2>

  <div class="catList">
    <div class="M1"><a href="https://spiritualitism.in/insurance/slink.php">Family Friendly Content</a></div>

    <div class="M1"><a href="https://spiritualitism.in/insurance/slink.php">Mature Content</a></div>
    <div class="M1"><a href="https://spiritualitism.in/insurance/slink.php">Other Type of Content</a></div>

    <center>
      <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-5104426911267208" crossorigin="anonymous" type="60401fed773cd9f75e65b2c3-text/javascript"></script>
      <!-- Go -->
      <ins class="adsbygoogle" style="display:block" data-ad-client="ca-pub-5104426911267208" data-ad-slot="7351088471" data-ad-format="auto" data-full-width-responsive="true"></ins>
      <script type="60401fed773cd9f75e65b2c3-text/javascript">
        (adsbygoogle = window.adsbygoogle || []).push({});
      </script>
    </center>
    <?php if (!isBotDetected()) {
      echo '
<h2 class="subox"><font color="white"><b>New Announcement on <?php echo $sn; ?></b></font></h2>
<div class="updates"> <b>Now You Must Go Through 2-3 Steps to Access <?php echo $sn; ?> Website Because of Ban on <?php echo $sn; ?> Domain again and again </b></div>
<div class="subox"><left><b><font color="white">Other Websites</font><b></left></div>
<div class="updates">
<a href="?sn=yts">YTS</a> | <a href="?sn=yifymovies">Yiffy Movies</a> | <a href="?sn=xpau">Xpau</a> | <a href="?sn=worldfree4u">worldfree4u</a> | <a href="?sn=telugurockers">telugurockers</a> | <a href="?sn=telugupalaka">Telugupalaka</a> | <a href="?sn=tamilrockers">Tamilrockers</a> | <a href="?sn=tamilmv">Tamilmv</a> | <a href="?sn=tamilgun">Tamilgun</a> | <a href="?sn=sdmoviespoint">sdmoviespoint</a> | <a href="?sn=pagalmovies">pagalmovies</a> | <a href="?sn=mp4moviez">mp4moviez</a> | <a href="?sn=moviescouch">moviescouch</a> | <a href="?sn=moviesada">moviesada</a> | <a href="?sn=movierulz">movierulz</a> | <a href="?sn=moviepur">moviepur</a> | <a href="?sn=moviepoint">moviepoint</a> | <a href="?sn=movieninja">movieninja</a> | <a href="?sn=mkvcage">mkvcage</a> | <a href="?sn=<?php echo $sn; ?>"><?php echo $sn; ?></a> | <a href="?sn=khatirmaza">khatirmaza</a> | <a href="?sn=katmovie">katmovie</a> | <a href="?sn=jiorockers">jiorockers</a> | <a href="?sn=isaimini">isaimini</a> | <a href="?sn=ipagal">ipagal</a> | <a href="?sn=hdpopcorn">hdpopcorn</a> | <a href="?sn=hdfriday">hdfriday</a> | <a href="?sn=filmyzilla">filmyzilla</a> | <a href="?sn=filmywap">filmywap</a> | <a href="?sn=filmyhit">filmyhit</a> | <a href="?sn=divxcrawler">divxcrawler</a> | <a href="?sn=coolmoviez">coolmoviez</a> | <a href="?sn=cinemavilla">cinemavilla</a> | <a href="?sn=cinebloom">cinebloom</a> | <a href="?sn=besthdmovies">besthdmovies</a> | <a href="?sn=9xmovies">9xmovies</a> | <a href="?sn=7starhd">7starhd</a> | <a href="?sn=1337x">1337x</a> | <a href="?sn=123movies">123movies</a> |
<a href="?sn=pagalworld">pagalworld </a> |
<a href="?sn=9xmovie">9xmovie </a> |
<a href="?sn=movierulz-com">movierulz.com </a> |
<a href="?sn=tamilrockers-com">tamilrockers.com </a> |
<a href="?sn=bolly4u">bolly4u </a> |
<a href="?sn=movie-rules">movie rulz </a> |
<a href="?sn=300mbmovies">300 Mb Movies </a> |
<a href="?sn=jio-rockers">Jio Rockers </a> |
<a href="?sn=tamilrockers-telugu">Tamilrockers telugu </a> |
<a href="?sn=tamilrockers-cl">tamilrockers.cl </a> |
<a href="?sn=tamilrockers-co">tamilrockers.co </a> |
<a href="?sn=filmywap-com">filmuywap.com </a> |
<a href="?sn=123movies">123movies </a> |
<a href="?sn=movierulz-pz">movierulz.pz </a> |
<a href="?sn=tamilrockers-gr">tamilrockers.gr </a> |
<a href="?sn=tamilrockers-li">tamilrockers.li </a> |
<a href="?sn=tamilrockers-gs">tamilrockers.gs </a> |
<a href="?sn=fmovies-to">fmovies.to </a> |
<a href="?sn=skymovies">skymovies </a> |
<a href="?sn=movierulz-pl">movierulz.pl </a> |
<a href="?sn=putlocker">putlocker </a> |
<a href="?sn=putlockers">putlockers </a> |
<a href="?sn=kuttymovies">kuttymovies </a> |
</div>
<div class="search"><form action="searchresult.php" method="POST">Search Movie : <input type="text" name="query" id="find" value="" size="10" />
<input type="hidden" name="per_page" value="1" />
<input type="submit" value="Search" /></form></div>

';
    } ?>
    <div class="des">
      <h2>Health Insurance</h2>



      <p>Medical costs are sky-rocketing! Get medical insurance policies to your healthcare outlay. With cashless centre, remain tension-free. Manufactured by IRDAI, PolicyBazaar makes it possible to compare and get the very best health program.<br>
        Health insurance is a health insurance policy that provides monetary policy for medical expenses once the policyholder is hospitalised. </p>


      <p>The insurance provider provides the insured with all the centre of cashless Injuries at a community hospital or supplies a compensation to the incurred costs. </p>


      <p>Individual Strategies Cashless Hospitalization: Medical costs are sky-rocketing! Get medical insurance policies. With cashless centre, remain tension-free. Manufactured by IRDAI, PolicyBazaar makes it possible to compare and get the very best health program.<br>
        Medical costs are sky-rocketing! Get medical insurance policies. With cashless centre, remain tension-free. Manufactured by IRDAI, PolicyBazaar makes it possible to compare and get the very best health program.<br>
        Medical insurance is a health insurance policy that provides monetary policy for medical expenses once the policyholder is hospitalised. Health Insurance Companies insure the insured together using the center of cashless hospitalization in a community hospital or offer a settlement to the incurred costs.</p>


      <h2>Importance of Health Insurance in India</h2>



      <p>Health insurance in India is among the fastest growing businesses. But this broad range for expansion suggests that the restricted penetration of health insurance one of the Indian people. Furthermore, just 18 percent of their entire population residing in metropolitan areas and 14 percent of their entire population living in rural regions had any kind of health insurance policy.Therefore, there's not any denying the significance of getting insurance from a country like India where medical costs are sky-rocketing. Everyone must purchase a fantastic medical insurance plan that includes medical expenses, hospitalisation expenses, medication and lab evaluation expenses, including serious illness. Do not get confused with queries such as -- Which health plan to purchase? Does this cover every eventuality? What disorders are excluded from this pay? PolicyBazaar is here in order to solve all such confusions.<br></p>
    </div>

    <!-- Google Tag Manager (noscript) -->
    <noscript><iframe src="https://www.googletagmanager.com/ns.html?id=GTM-WR6993X" height="0" width="0" style="display:none;visibility:hidden"></iframe></noscript>
    <!-- End Google Tag Manager (noscript) -->
    <div class="copyright">
      <a href="/about-us/?r=footer">About Us</a> |
      <a href="/contact-us/?r=footer">Contact Us</a> |
      <a href="/privacy-policy/?r=footer">Privacy Policy</a> |
      <a href="/terms/?r=footer">Term of Use</a> |


    </div>
  </div>
  </div>
  </div>



</html>

</body>

</html>
