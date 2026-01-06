
<!-- Global site tag (gtag.js) - Google Analytics -->
<script async src="https://www.googletagmanager.com/gtag/js?id=UA-145736982-1"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());

  gtag('config', 'UA-145736982-1');
</script>

<?php 

$get=$_GET['url'];
$url='http://'.$get;

// header('Location: '.$url);

?>
<meta http-equiv="refresh"
   content="0; url=<?php echo $url; ?>">
