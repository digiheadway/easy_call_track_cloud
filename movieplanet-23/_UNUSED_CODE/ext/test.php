<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <style>
        * {
	box-sizing: border-box;
	margin: 0;
	padding: 0;
}

button {
	margin-top: 20px;
}

#popup-background {
	position: fixed;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	background-color: rgba(0, 0, 0, 0.5);
	display: none;
}

#popup {
	position: absolute;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -50%);
	background-color: white;
	padding: 20px;
	box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.15);
	border-radius: 10px;
	max-width: 500px;
	width: 90%;
	display: flex;
	flex-direction: column;
}

#popup-header {
	display: flex;
	align-items: center;
	padding: 10px;
}

#popup-header img {
	width: 20px;
	height: 20px;
	margin-right: 10px;
}

#popup-header h2 {
	font-size: 18px;
}

#popup-content {
	display: flex;
	flex-direction: row;
	margin-top: 20px;
}

#popup-content-left {
	display: flex;
	justify-content: center;
	align-items: center;
}

#popup-content-left img {
	width: 50px;
	height: 50px;
}

#popup-content-right {
	margin-left: 20px;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
}

#popup-content-right h3 {
	font-size: 20px;
	margin-bottom: 10px;
}

.btn-download-android {
	background-color: #3DDC84;
	color: white;
	padding: 10px;
	border-radius: 10px;
	text-align: center;
	margin-bottom: 10px;
	text-decoration: none;
}

.btn-download-android:hover {
	background-color: #31C573;
}

.btn-download-iphone-pc {
	background-color: white;
	color: #7E7

/* Pop up */
.popup {
  position: fixed;
  width: 90%;
  max-width: 600px;
  margin: 0 auto;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 9999;
  background-color: #ffffff;
  border-radius: 10px;
  padding: 20px;
  box-sizing: border-box;
  text-align: center;
}

/* Pop up header */
.popup-header {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px 0 20px 0;
}

.popup-header svg {
  height: 20px;
  width: 20px;
  margin-right: 10px;
}

/* Pop up content */
.popup-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.popup-content img {
  width: 50px;
  height: 50px;
}

.popup-content p {
  font-size: 16px;
  margin: 10px 0;
  color: #4c4c4c;
}

.popup-content button {
  background-color: #07c;
  border: none;
  color: #fff;
  padding: 10px 20px;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.popup-content button:hover {
  background-color: #00b3ff;
}

.popup-content .loading {
  display: none;
  margin-top: 20px;
}

.popup-content .loading img {
  width: 50px;
  height: 50px;
}

.popup-content .loading p {
  font-size: 16px;
  margin-top: 10px;
  color: #4c4c4c;
}

.popup-content .try-again {
  display: none;
  margin-top: 20px;
}

.popup-content .try-again button {
  background-color: #ff5722;
  border: none;
  color: #fff;
  padding: 10px 20px;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.popup-content .try-again button:hover {
  background-color: #f44336;
}

    </style>
</head>
<body>
<button onclick="openPopup()">Open Popup</button>
	<div id="popup-background">
		<div id="popup">
			<div id="popup-header">
				<img src="lock.svg" alt="Lock Icon">
				<h2>Link is Locked</h2>
			</div>
			<div id="popup-content">
				<div id="popup-content-left">
					<img src="download.svg" alt="Download Icon">
				</div>
				<div id="popup-content-right">
					<h3>Download App to Unlock the Link</h3>
					<a href="#" class="btn-download-android">Download for Android</a>
					<a href="#" class="btn-download-iphone-pc">Download for iPhone/PC</a>
					<div id="popup-loading">
						<img src="loading.gif" alt="Loading Icon">
						<p>Waiting for App Installation<span class="dots"></span></p>
						<button onclick="resetPopup()">Failed? Try Downloading Again</button>
					</div>
				</div>
			</div>
		</div>
	</div>
</body>
<script>
// Get the pop up element
var popup = document.getElementById("popup");

// Get the download buttons
var androidBtn = document.getElementById("android-btn");
var iosPcBtn = document.getElementById("ios-pc-btn");

// Get the loading and try again elements
var loading = document.getElementById("loading");
var tryAgain = document.getElementById("try-again");

// Function to show loading state
function showLoading() {
  // Hide the download buttons
  androidBtn.style.display = "none";
  iosPcBtn.style.display = "none";

  // Show the loading element
  loading.style.display = "block";
}

// Function to show try again state
function showTryAgain() {
  // Hide the loading element
  loading.style.display = "none";

  // Show the try again element
  tryAgain.style.display = "block";
}

// Function to reset the pop up state
function resetPopup() {
  // Show the download buttons
  androidBtn.style.display = "block";
  iosPcBtn.style.display = "block";

  // Hide the loading and try again elements
  loading.style.display = "none";
  tryAgain.style.display = "none";
}

// Add click event listeners to the download buttons
androidBtn.addEventListener("click", function() {
  showLoading();

  // Simulate a delay before showing the loading state
  setTimeout(function() {
    showTryAgain();
  }, 3000);
});

iosPcBtn.addEventListener("click", function() {
  showLoading();

  // Simulate a delay before showing the loading state
  setTimeout(function() {
    showTryAgain();
  }, 3000);
});

// Add click event listener to the try again button
tryAgain.addEventListener("click", function() {
  resetPopup();
});

</script>
</html>