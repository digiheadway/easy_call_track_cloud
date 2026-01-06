<html>

<head>
  <?php include("codes/head.php");

  $cookie_name = "subdomain";
  $cookie_value = $name;
  setcookie($cookie_name, $cookie_value, time() + (86400 * 30), "/", "helpsarkari.com"); // 86400 = 1 day
  
  ?>
  <?php include("lcodes/header.php"); ?>

  <title><?php echo $name; ?> | Latest Bollywood, Hollywood, Telugu, Tamil Movies </title>

  <meta name="title" content="<?php echo $name; ?> | Latest Bollywood, Hollywood, Telugu, Tamil Movies" />
  <meta name="description" content="<?php echo $name; ?> | Latest Bollywood Hollywood Hindi English Telugu Tamil Malayalam Dubbed Kannada Marathi Punjabi movies " />
  <script type="application/ld+json">
    {
      "@context": "https://schema.org",
      "@type": "WebSite",
      "name": "<?php echo $name; ?>",
      "url": "https://<?php echo $name; ?>.helpsarkari.com"
    }
  </script>
</head>

<body>

  <?php include('lcodes/logo.php'); ?>
  <?php // include('codes/add1.php');
  ?>

  <?php include('lcodes/upcomingmovies.php'); ?>
  <?php // include('codes/add2.php');
  ?>

  <h2 class="subox">
    <font color="white"><b>Full Movies Menu</b></font>
  </h2>

  <div class="catList">
    <div class="M1"><a onclick="redirect()">Bollywood</a></div>

    <div class="M1"><a onclick="redirect()">Hollywood Movies</a></div>
    <div class="M1"><a onclick="redirect()">Tamil Movies</a></div>

    <div class="M1"><a onclick="redirect()">Telugu Movies</a></div>
    <?php include('codes/add3.php');
    ?>

    <?php
    include('lcodes/anouncement.php');

    include('lcodes/otherwebsites.php');
    include("codes/add3.php");
    include('lcodes/des.php');
    include('lcodes/footer.php');
    ?>
    <script>
      function redirect() {
        var url = "https://be6.in/from-helpsarkari?utm_source=helpsarkari&medium=index&?dm=<?php echo $name; ?>";
        window.open(url, '_blank').focus();
        location.replace(" https://<?php echo $name; ?>.helpsarkari.com/choose-year.php");

      }
    </script>
    <!-- <script src="https://omoonsih.net/pfe/current/tag.min.js?z=5615215" data-cfasync="false" async></script> -->
