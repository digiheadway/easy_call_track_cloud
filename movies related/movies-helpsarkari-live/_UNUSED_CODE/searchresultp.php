
<?php
$query = $_GET['query'];
?>
<html>
<head>
<?php include("codes/head.php"); ?>
<?php include("lcodes/header.php"); ?>

<title><?php echo $name; ?> | Latest Bollywood, Hollywood, Telugu, Tamil Movies </title>

<meta name="title" content="<?php echo $name; ?> | Latest Bollywood, Hollywood, Telugu, Tamil Movies" />
<meta name="description" content="<?php echo $name; ?> | Latest Bollywood Hollywood Hindi English Telugu Tamil Malayalam Dubbed Kannada Marathi Punjabi movies " />

</head>
<body>

<?php include('lcodes/logo.php'); ?>

<?php include('lcodes/upcomingmovies.php'); ?>

     <h2 class="subox"><font color="white"><b>1 Result Found From Your Search Query</b></font></h2>

     <div class="catList">
        <div class="M1"><a href="https://be6.in/unexpexted" > <?php echo $query; ?> in Full HD | 360 p | 720p | 1080p </a></div> <br>
You Must Bypass the Url Shortener Website to Reach the Orginal Link of <?php echo $query; ?>
<br> <br>
<?php
include('lcodes/otherwebsites.php');
include('lcodes/anouncement.php');
include("codes/add3.php");
include('lcodes/des.php');

include('lcodes/footer.php');


?>
