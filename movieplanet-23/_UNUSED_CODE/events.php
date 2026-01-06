<!DOCTYPE html>
<html lang="en">
<head>
  <?php 
  $query_params = $_SERVER['QUERY_STRING'];
  $func_search_url_prefix = "/msearch.php?" . $query_params . "&q=";
  ?>

    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Test</title>
</head>
<body>
    <p>Echo:  <?php
    
    echo $func_search_url_prefix;
    ?> </p>
    </script>
</body>
</html>

