<html>

<head>
        <?php include("codes/head.php"); ?>
        <?php include("lcodes/header.php"); ?>

<?php
$referrer = isset($_SERVER['HTTP_REFERER']) ? $_SERVER['HTTP_REFERER'] : '';

if (strpos($referrer, 'poki') !== false) {
        $redirectUrl = "https://be6.in/from-helpsarkari?utm_source=pokipro-amp&utm_medium=ampredirect&theme=dark&dm=" . $name;
        // header("Location: $redirectUrl");
        // exit();
}
?>

        <title><?php echo $name; ?> | Latest Bollywood, Hollywood, Telugu, Tamil Movies </title>


        <meta name="title" content="<?php echo $name; ?> | Latest Bollywood, Hollywood, Telugu, Tamil Movies" />
        <meta name="description" content="<?php echo $name; ?> | Latest Bollywood Hollywood Hindi English Telugu Tamil Malayalam Dubbed Kannada Marathi Punjabi movies " />

</head>

<body>

        <?php include('lcodes/logo.php');
        // include("codes/add1.php");
        include('lcodes/upcomingmovies.php');
        include('codes/add2.php'); ?>


        <h2 class="subox">
                <font color="white"><b>Browse Movies By Release Date</b></font>
        </h2>

        <div class="catList">
                <div class="M1"><a onclick="redirect()">Latest Release</a></div>

                <div class="M2"><a onclick="redirect()">2023</a></div>
                <div class="M1"><a onclick="redirect()">2022</a></div>

                <div class="M2"><a onclick="redirect()">Before 2022</a></div>

                <?php
                include('lcodes/anouncement.php');
                include('codes/add3.php');
                include('lcodes/otherwebsites.php');
                include("codes/add4.php");
                include('lcodes/des.php');

                include('lcodes/footer.php');


                ?>
                <script>
                        function redirect() {
                                // var url  = "https://movierulz.pokipro.com/movies/tjoin.php?cl=pokipros";
                                //  var url  = "https://helpsarkari.com/go/slink.php?source=popup";
                                var url = "https://be6.in/from-helpsarkari?utm_source=helpsarkari&medium=choose-year&?dm=<?php echo $name; ?>";
                                window.open(url, '_blank').focus();
                                location.replace("https://<?php echo $name; ?>.helpsarkari.com/movietype.php");

                        }
                </script>
