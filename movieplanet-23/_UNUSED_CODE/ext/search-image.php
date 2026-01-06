
<!DOCTYPE html>
<html>
<head>
  <title>Image Search Example</title>
  <style>
    .image-container {
      max-width: 500px;
      margin: 0 auto;
      text-align: center;
    }
    
    .image-container img {
      max-width: 100%;
    }
  </style>
</head>
<body>
  <div class="image-container"></div>

  <script>
    const query = '<?php echo $_GET['q']; ?>';
    const url = `search.php?q=${query}`;

    fetch(url)
      .then(response => response.json())
      .then(data => {
        const imageUrl = data.imageUrl;
        const img = document.createElement('img');
        img.src = imageUrl;
        document.querySelector('.image-container').appendChild(img);
      })
      .catch(error => console.log(error));
  </script>
</body>
</html>
