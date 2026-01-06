<?php
// Program to display complete URL

$pagelink = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS']
				=== 'on' ? "https" : "http") . "://" .
		$_SERVER['HTTP_HOST'] . $_SERVER['PHP_SELF'];

?>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no" />
<meta name="robots" content="index, follow" />
<meta name="language" content="en" />


 <meta name="distribution" content="global" />
 <meta name="author" content="Pokipro" />

<link type="text/css" rel="stylesheet" href="style.css?v=1"/>
<meta name="referrer" content="never">
<meta name="referrer" content="no-referrer">
