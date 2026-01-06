<!DOCTYPE html>
<html lang="en">
	<head>
		<?php include '../inc/head-global.php'; ?>
		<title>About Us - <?php echo $subdomain; ?></title>
<style>
	ul,
li,
p,
h1,
h2,
h3 {
    color: #b6b6b6;
}

</style>		
	</head>

	<body>
		<?php include '../inc/header.php'; ?>

		<main>
			<section class="hero-section">
				<div class="container">
					<div class="sub-container">
						<div class="latest-movies-link">
							<h1 style="margin-top: 20px;" class="content_main_heading">
								About Us
							</h1>

							<p class="content_para">
								<?php echo $subdomain; ?>
								is Website where you can browse latest movies.
							</p>
							<p class="content_para">
								Contact us by Going to
								<a href="/pages/contact-us.php">Contact Us Page</a>
							</p>
						</div>

						<?php include '../inc/footer.php'; ?>
					</div>
				</div>
			</section>
		</main>
	</body>
</html>
