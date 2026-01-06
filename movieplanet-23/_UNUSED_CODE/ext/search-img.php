<?php
    $searchquery = $_GET['q'];
    if ($searchquery) {
        setcookie('q', $searchquery, time() + (86400 * 30), "/"); // 86400 = 1 day
    }
    ?>
    <!DOCTYPE html>
<html lang="en">
  <head>

    <?php include 'inc/head-global.php'; ?>
 
    <link
      href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap"
      rel="stylesheet"
    />
    <title>
      <?php echo $searchquery; ?> | Watch Or Download Latest Movies/Webseries
    </title>
    <meta
      name="description"
      content="(<?php echo $today; ?>) Download or Watch Movies and Webseries on <?php echo $subdomain; ?> | Latest Hollywood, English, Bollywood Hindi English Telugu Tamil Malayalam Dubbed Kannada Marathi Punjabi movies"
    />
    <meta name="robots" content="index, follow" />
    <meta name="language" content="en" />
    <meta name="distribution" content="global" />
    <meta name="author" content="IndianMoviesPlanet" />
    <link rel="canonical" href="
    <?php echo "https://" . $subdomainexactly . "." . $fulldomain; ?>
    " />
        <!-- <script src="/assets/js/script.js?v1"></script> -->
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
  </head>
  <body>
    <?php include 'inc/header.php'; ?>
    <main>
      <section class="downlode_section">
        <div class="container">
          <div class="downlode_container">
            <div class="search_result_main_head">
              <b>Found</b> <span>4</span> <b>Result</b>
            </div>
            <h1 class="downlode_main_hading">
              Search Result For: <b><?php echo $searchquery; ?> </b>
            </h1>
            <div style="display: flex; justify-content: center;" class="image-container">
              <img id="loader_img"
                src="https://i.pinimg.com/originals/49/23/29/492329d446c422b0483677d0318ab4fa.gif"
                height="400"
              />
            </div>
            <div class="downlode_btn_box">
              <a href="#" class="movies_downlode_btn r-flex ali-c jut-sb">
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | Hindi Dubbed</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#" class="movies_downlode_btn r-flex ali-c jut-sb">
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | Hindi Dubbed</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#" class="movies_downlode_btn r-flex ali-c jut-sb">
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | Hindi Dubbed</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
              <a href="#" class="movies_downlode_btn r-flex ali-c jut-sb">
                <span
                  ><?php echo $searchquery; ?>
                  | Full HD | Hindi Dubbed</span
                >
                <img src="/assets/img/downlode.svg" alt="downlode" />
              </a>
            </div>
          </div>

          <div class="donlode_privicy_policiy">
            <?php 
                    include 'inc/footer.php'; 
                    ?>
        