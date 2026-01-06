<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Database connection parameters
$servername = "localhost"; // Replace with your server name or IP address
$username = "fmyfzvvwud";
$password = "MG5xCnA8Pt";
$database = "fmyfzvvwud";

// Create a connection
$conn = new mysqli($servername, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Check if form is submitted
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    if (isset($_POST["approve"])) {
        // Approve button clicked
        $id = $_POST["id"];
        $sql = "UPDATE queries SET approved = 1 WHERE id = $id";
        if ($conn->query($sql) === TRUE) {
            echo "Query Approved successfully";
            // Redirect to a different page to prevent resubmission
            header("Location: /inc/img_status.php"); // Change 'success.php' to the appropriate URL
            exit();
        } else {
            echo "Error: " . $conn->error;
        }
    } elseif (isset($_POST["disapprove"])) {
        // Disapprove button clicked
        $id = $_POST["id"];
        $sql = "UPDATE queries SET approved = 0 WHERE id = $id";
        if ($conn->query($sql) === TRUE) {
            echo "Query Disapproved successfully";
            // Redirect to a different page to prevent resubmission
            header("Location: /inc/img_status.php"); // Change 'success.php' to the appropriate URL
            exit();
        } else {
            echo "Error: " . $conn->error;
        }
    }
}
// Check if both 'id' and 'newImageUrl' are set in the GET request
if (isset($_GET["id"]) && isset($_GET["newImageUrl"])) {
    $newImageUrl = $_GET["newImageUrl"];
    $id = $_GET["id"];

    // Sanitize the id to prevent SQL injection
    $id = intval($id);

    $sql = "UPDATE queries SET imageUrl = '$newImageUrl', not_this = 0  WHERE id = $id";
    if ($conn->query($sql) === TRUE) {
        // Image URL updated successfully
        header("Location: /inc/img_status.php?q=".$_GET['q']); // Change 'success.php' to the appropriate URL
        exit();
    } else {
        echo "Error updating image URL: " . $conn->error;
    }
}

// Check if both 'id' and 'correct' are set in the GET request
if (isset($_GET["id"]) && isset($_GET["correct"])) {
    $correct = $_GET["correct"];
    $correct = strtolower($correct);

    $id = $_GET["id"];
    $ImageUrl = $_GET["ImageUrl"];


    // Sanitize the id to prevent SQL injection
    $id = intval($id);

    $sql = "UPDATE queries SET correct = '$correct' WHERE id = $id";
    if ($conn->query($sql) === TRUE) {


        $conn->query("INSERT IGNORE INTO queries (query, imageUrl, approved) VALUES ('$correct', '$ImageUrl',1)");
        // Image URL updated successfully
        header("Location: /inc/img_status.php?q=" . $_GET['correct']); // Change 'success.php' to the appropriate URL
        exit();
    } else {
        echo "Error updating image URL: " . $conn->error;
    }
}


// Fetch queries with no value in the 'approved' column
$sql = "SELECT * FROM queries WHERE approved IS NULL AND correct IS NULL Order by hits DESC limit 1";

if (isset($_GET['no']) || isset($_COOKIE['no'])) {
    $sql = "SELECT * FROM queries WHERE (not_this / hits) > 0.05 AND hits > 20 ORDER BY (not_this / hits) DESC limit 1";
}
if (isset($_GET['q'])) {
    $sql = "SELECT * FROM queries WHERE query = '" . $_GET['q'] . "'";
}
$result = $conn->query($sql);

if ($result->num_rows > 0) {
    $row = $result->fetch_assoc();
    $id = $row["id"];
    $query = $row["query"];
    $imageUrl = $row["imageUrl"];
    $hits = $row["hits"];
    $correct = $row["correct"];
    $not_this = $row["not_this"];
    $down_tried = $row["down_tried"];
    $approved = $row["approved"];



} else {
    echo "No queries to approve or disapprove.";
    die();
}


$conn->close();
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Image Queries Approval </title>
<style>
body{
    margin:20px;
    padding:20px;
}
.btn{
    margin:10px auto;
    width:100%;
    background:black;
    font-size:20px;
    color:white;
    padding:20px;
    border-radius:5px;
    font-weight: bold;
}

img{
    margin:20px auto;
     display: block; /* Add this line to center the image horizontally */
     max-width:100%;

}
h1,p{
    text-align:center;
}
</style>
</head>
<body>
    <div>
        <img src="<?php echo $imageUrl; ?>" alt="" height='40%' >
        <a href="https://www.google.com/search?q=<?php echo $query; ?>+movie/web+series+poster&tbm=isch" target="_blank"><h1><?php echo $query; ?></h1></a>
        <p>Hits: <?php echo $hits; ?></p>
         <p>Not This: <?php echo $not_this; ?></p>
          <p>Down Tried: <?php echo $down_tried; ?></p>
                    <p><?php echo $correct." - ". $approved; ?> </p>


        <form method='post'  onsubmit="return confirmApproveAction(this);">
            <input type='hidden' name='id' value='<?php echo $id; ?>'>
            <input type='submit' class="btn" name='approve' value='Approve' >
            <input type='submit'  class="btn" name='disapprove' value='Disapprove'>
        </form>
        <br>
        
         <p onclick='editImageUrl()'>Edit Image URL</p>
         <br>
         <p onclick='editcorrect()'>Edit Correct Name</p>


    </div>
     <script>

        var $imageUrl = "<?php echo $imageUrl; ?>";
       function editImageUrl() {
    let newUrl = prompt("What's the new Image URL?");
    if (newUrl !== null && newUrl !== "") {
        let id = '<?php echo $id; ?>';
        let query = '<?php echo $query; ?>';
        window.location.href = '/inc/img_status.php?id=' + id + '&newImageUrl=' + newUrl + '&q=' + query;
    }
}
function confirmApproveAction(form) {
        if (document.activeElement.name === 'approve') {
            return confirm('Are you sure you want to Approve this?');
        }
        return true; // No confirmation for Disapprove
    }

         function editcorrect() {
    let correct_name = prompt("What's the correct name?");
    if (correct_name !== null && correct_name !== "") {
        let id = '<?php echo $id; ?>';
        let query = '<?php echo $query; ?>';
        window.location.href = '/inc/img_status.php?id=' + id + '&correct=' + correct_name + '&q=' + query + '&ImageUrl=' + $imageUrl;
    }
}
    </script>
</body>
</html>
