

 <?php


 list($subdomain, $host) = explode('.', $_SERVER["SERVER_NAME"]);


 $name = ucfirst($subdomain);
 $url1 = "$subdomain" . ".com";
 $url2 = "$subdomain" . ".pz";
 $url3 = "$subdomain" . ".pe";
 $url4 = "$subdomain" . ".pl";
 $url5 = "#";
 $can1 = "https://$subdomain.pokipro.com";
 $jt = "https://be6.in/tjoin";
 $anoti = "#";
 $appdl = "https://be6.in/unexpexted";
 $sitelink1 = "$subdomain" . "-com";
 $sitelink2 = "$subdomain" . "-pz";
 $sitelink3 = "$subdomain" . "-pe";
 $sitelink4 = "$subdomain" . "-pl";
 $sitelink5 = "#";
 $shrinklink = "https://be6.in/unexpexted";


 $subdomain_landed = $_COOKIE['subdomain'];
 if (!$subdomain_landed) {
   if (!$name) {
     $subdomain_landed = "Movies";
   } else {
     $subdomain_landed = $name;
   }
 }
 ?>
