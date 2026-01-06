
I am a Real Estate Agent. I Need a Web App to Manage Properties,Persons and Buyers.

It has 4 main pages. 
1. Properties
2. Persons
3. Buying Requirements
4. Matches

It will the installable web app pwa. So i can install it on my phone and use it. Make sure it is responsive and works on all devices. I will use it on my phone and tablet most of the time. if you can make the table and card view both that will be great. It is usable very easily and user friendly and optimize for minimum clicks and look good. the ui should be tailwind and colorful and modern. 



The Web App should have the following features:

- Manage/Add/Filter Out/View/Edit/Delete a Properties
- Manage/Add/Filter Out/View/Edit/Delet a Person Which Can Be Buyer,Seller,Dealer,Developer, Builder,Agent,Other, Related Person, Personal Network, etc.
- Manage/Add/Filter Out/View/Edit/Delet a Buying Requirement
- See Matching a Buying Requirement to a Property.


The Flow of the App is as follows:

Properties:
- We will see the list of Properties with search, filter and sort options. 
- Filter will have options like Property Type, Property Status, Property Price Range Slider, Property Location, Property Size Range Slider, Highlights, Tags etc.
- We will have a floating button to add a new property at bottom right of the list.
- in the List of Properties, we will see the Property Title, Property Type, Property Status, Property Price Range, Property Location, Property Size, Property Highlights, Property Tags etc.
- When we click on a property, we will see the details of the property which is in a modal comes from bottom to top and the modal will have the details of the property at using around 90% of the screen height and the modal will have the property details.
- The Model will have the all property details which can be edited by double clicking the details which allow edit the individual details and save and cancel button inline with the input. 
- The Detailed can be text, dropdown, slider, tags type and can be added by clicking on the add button and can be removed by clicking on the remove button.
- It will show the number of persons related to the property and option to assign new person to the property. which open a modal to select the person from the list of persons with search option and add new person option. 
- it will show list of people with their names, role on that property, call button, whatsapp btn, and clicking on the person name will open the person details modal same modal like we open from person list.
- After Person details list it will show the list of matching buying requirements with the property with title. match score, budget range, people name, call button whatsapp btn. Also clicking on the card will open the buying requirement details modal same modal like we open from buying requirement list.


Buying Requirements:

- We will see the list of Buying Requirements with search, filter and sort options
- We will see the list of Buying Requirements with title, budget range, property type, property location, property size, highlights, tags etc.
- When we click on a buying requirement, we will see the details of the buying requirement which is in a modal comes from bottom to top and the modal will have the details of the buying requirement at using around 90% of the screen height and the modal will have the buying requirement details.
- The Model will have the all buying requirement details which can be edited by double clicking the details which allow edit the individual details and save and cancel button inline with the input. 
- The Detailed can be text, dropdown, slider, tags type and can be added by clicking on the add button and can be removed by clicking on the remove button.
- It will show the number of properties related to the buying requirement and option to assign new person to the buying requirement. which open a modal to select the person from the list of person with search option and add new person option. 


Persons:

- We will see the list of Persons with search, filter and sort options
- We will see the list of Persons with name, role, connected roles, buying requirement, call button, whatsapp btn.
- When we click on a person, we will see the details of the person which is in a modal comes from bottom to top and the modal will have the details of the person at using around 90% of the screen height and the modal will have the person details.
- The Model will have the all person details which can be edited by double clicking the details which allow edit the individual details and save and cancel button inline with the input. 
- It will show the list of properties related to the person with title, property type, property status, property price range, property location, property size, property highlights, property tags etc.
- It will show the list of buying requirements related to the person with title, budget range, property type, property location, property size, property highlights, property tags etc.


The tags, highlights, locations, should be like tags and can be added like we add labels like click on add new then in input box and suggations will be shown and can be selected from the suggations and filtering out as we type. the suggations comes from the data we have already loaded from the json of properties or other similar data. like tags in properties suggestion comes from the tags in other properties.

The Budget and Price is always in Lakhs and indian rs (use symbol only) Pirce min : 5 means 5 Lakhs. 

The Php Api File is as below to understand the Structure is as follows:

api endpoint: https://prop.digiheadway.in/api/network/v2.php?resource=properties
<?php

error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/api_errors.log');

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: *");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') exit(http_response_code(200));

// Database Connection
const DB = [
    'host' => 'localhost',
    'name' => 'u240376517_propdb',
    'user' => 'u240376517_propdb',
    'pass' => 'Y*Q;5gIOp2'
];

try {
    $pdo = new PDO("mysql:host=" . DB['host'] . ";dbname=" . DB['name'] . ";charset=utf8", DB['user'], DB['pass'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);
} catch (PDOException $e) {
    error_log("DB Connection Failed: " . $e->getMessage());
    exit(json_encode(['success' => false, 'message' => 'Database connection failed']));
}

// API Routing
$routes = ['as_buyers', 'as_connections', 'as_leads', 'as_persons', 'as_properties', 'as_vw_matches','vw_properties'];
$resource = $_GET['resource'] ?? '';
$id = $_GET['id'] ?? null;
$method = $_SERVER['REQUEST_METHOD'];
$data = json_decode(file_get_contents('php://input'), true) ?? [];

if (!in_array($resource, $routes)) exit(json_encode(['success' => false, 'message' => 'Endpoint not found']));


handleRequest($resource, $method, $id, $data, $pdo);

function handleRequest($resource, $method, $id, $data, $pdo) {
    $table = $resource;
    
    switch ($method) {
        case 'GET':
            $stmt = $pdo->prepare("SELECT * FROM $table" . ($id ? " WHERE id = ?" : ""));
            $stmt->execute($id ? [$id] : []);
            sendResponse(200, true, 'Success', $id ? $stmt->fetch() : $stmt->fetchAll());
            break;
        case 'POST':
            validateRequired($data, getRequiredFields($resource));
            $keys = array_keys($data);
            $stmt = $pdo->prepare("INSERT INTO $table (" . implode(',', $keys) . ") VALUES (" . rtrim(str_repeat('?,', count($keys)), ',') . ")");
            $stmt->execute(array_values($data));
            sendResponse(201, true, 'Created', ['id' => $pdo->lastInsertId()]);
            break;
        case 'PUT':
        case 'PATCH':
            if (!$id) sendResponse(400, false, 'Missing ID');
            $updates = implode(',', array_map(fn($key) => "$key = ?", array_keys($data)));
            $stmt = $pdo->prepare("UPDATE $table SET $updates WHERE id = ?");
            $stmt->execute([...array_values($data), $id]);
            sendResponse(200, true, 'Updated');
            break;
        case 'DELETE':
            if (!$id) sendResponse(400, false, 'Missing ID');
            $pdo->prepare("DELETE FROM $table WHERE id = ?")->execute([$id]);
            sendResponse(200, true, 'Deleted');
            break;
        default:
            sendResponse(405, false, 'Method not allowed');
    }
}

function validateRequired($data, $fields) {
    foreach ($fields as $field) if (empty(trim($data[$field] ?? ''))) sendResponse(400, false, "Missing field: $field");
}

function getRequiredFields($resource) {
    return match ($resource) {
        'properties' => ['title', 'price_min', 'price_max'],
        'buyers' => ['title', 'budget_min', 'budget_max', 'type'],
        'persons' => ['name', 'phone', 'roles'],
        'connections' => ['person_id', 'role'],
        default => []
    };
}

function sendResponse($code, $success, $message, $data = null) {
    http_response_code($code);
    echo json_encode(['success' => $success, 'message' => $message, 'data' => $data, 'timestamp' => time()]);
    exit;
}
?>

The Db export file is as below to understand the Structure is as follows:



--
-- Table structure for table `as_buyers`
--

CREATE TABLE `as_buyers` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `budget_min` decimal(12,2) NOT NULL,
  `budget_max` decimal(12,2) NOT NULL,
  `type` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `preferred_areas` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`preferred_areas`)),
  `excluded_areas` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`excluded_areas`)),
  `excluded_properties` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`excluded_properties`)),
  `intent` enum('Urgent','Passive') DEFAULT NULL,
  `source` enum('Dealer','Buyer','Channel Partner','Other') DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ;

-- --------------------------------------------------------

--
-- Table structure for table `as_connections`
--

CREATE TABLE `as_connections` (
  `id` int(11) NOT NULL,
  `property_id` int(11) DEFAULT NULL,
  `buyer_id` int(11) DEFAULT NULL,
  `person_id` int(11) NOT NULL,
  `role` enum('Buyer','Dealer','Developer','POC','Salesman','Channel Partner','Other') DEFAULT NULL,
  `remark` varchar(100) NOT NULL
) ;

-- --------------------------------------------------------

--
-- Table structure for table `as_leads`
--

CREATE TABLE `as_leads` (
  `id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `lead_stage` enum('fresh','unable to connect yet','wrong number','Talked','more then 3 talks','may visit','visit scheduled','visit done','negotiation','other','Moved to big CRM','trash','duplicate','need follow up','do not follow up','pending move to big crm') NOT NULL DEFAULT 'fresh',
  `potential` varchar(50) DEFAULT NULL,
  `budget` varchar(255) NOT NULL,
  `personal_note` text DEFAULT NULL,
  `follow_up_note` text DEFAULT NULL,
  `follow_up_on` datetime DEFAULT NULL,
  `interested_in` varchar(255) DEFAULT NULL,
  `source` varchar(100) DEFAULT NULL,
  `referrer` varchar(255) DEFAULT NULL,
  `data1` varchar(255) DEFAULT '',
  `data2` varchar(255) DEFAULT '',
  `data3` varchar(255) DEFAULT '',
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `automations` tinyint(1) NOT NULL DEFAULT 0,
  `listname` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Triggers `as_leads`
--
DELIMITER $$
CREATE TRIGGER `after_lead_update` AFTER UPDATE ON `as_leads` FOR EACH ROW BEGIN
    -- Check if the phone number already exists
    IF NEW.lead_stage = 'Moved to big CRM' AND 
       NOT EXISTS (SELECT 1 FROM as_persons WHERE phone = NEW.phone) THEN
        INSERT INTO as_persons (name, phone, source, notes)
        VALUES (NEW.name, NEW.phone, NEW.source, NEW.personal_note);
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `as_persons`
--

CREATE TABLE `as_persons` (
  `id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `roles` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`roles`)),
  `description` text DEFAULT NULL,
  `bond_strength` enum('High','Medium','Low') DEFAULT NULL,
  `source` varchar(100) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `as_properties`
--

CREATE TABLE `as_properties` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `price_min` decimal(12,2) NOT NULL,
  `price_max` decimal(12,2) NOT NULL,
  `size_min` decimal(8,2) DEFAULT NULL,
  `size_max` decimal(8,2) DEFAULT NULL,
  `type` enum('Residential','Commercial','Plot') NOT NULL,
  `locality` varchar(100) DEFAULT NULL,
  `latitude` decimal(9,6) DEFAULT NULL,
  `longitude` decimal(9,6) DEFAULT NULL,
  `landmark_distance` decimal(6,2) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `highlights` text DEFAULT NULL,
  `status` enum('Available','Sold','Under Negotiation') DEFAULT 'Available',
  `rating` tinyint(4) DEFAULT NULL,
  `tags` longtext DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `city` varchar(50) DEFAULT 'Panipat',
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ;

-- --------------------------------------------------------

--
-- Stand-in structure for view `as_vw_matches`
-- (See below for the actual view)
--
CREATE TABLE `as_vw_matches` (
`buyer_id` int(11)
,`buyer_title` varchar(255)
,`property_id` int(11)
,`property_title` varchar(255)
,`match_score` int(3)
);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `as_buyers`
--
ALTER TABLE `as_buyers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_as_buyers_type` (`type`(768));

--
-- Indexes for table `as_connections`
--
ALTER TABLE `as_connections`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_connections_property` (`property_id`),
  ADD KEY `fk_connections_person` (`person_id`),
  ADD KEY `fk_connections_buyer` (`buyer_id`);

--
-- Indexes for table `as_leads`
--
ALTER TABLE `as_leads`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `as_persons`
--
ALTER TABLE `as_persons`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `phone` (`phone`);

--
-- Indexes for table `as_properties`
--
ALTER TABLE `as_properties`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_as_properties_price` (`price_min`,`price_max`),
  ADD KEY `idx_as_properties_type` (`type`),
  ADD KEY `idx_as_properties_area` (`locality`);

--
-- Indexes for table `persons`
--
ALTER TABLE `persons`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_phone` (`phone`);

--
-- Indexes for table `properties`
--
ALTER TABLE `properties`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `webhook_data`
--
ALTER TABLE `webhook_data`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `as_buyers`
--
ALTER TABLE `as_buyers`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `as_connections`
--
ALTER TABLE `as_connections`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `as_leads`
--
ALTER TABLE `as_leads`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `as_persons`
--
ALTER TABLE `as_persons`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `as_properties`
--
ALTER TABLE `as_properties`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `persons`
--
ALTER TABLE `persons`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `properties`
--
ALTER TABLE `properties`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `webhook_data`
--
ALTER TABLE `webhook_data`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `as_connections`
--
ALTER TABLE `as_connections`
  ADD CONSTRAINT `fk_connections_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `as_buyers` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_connections_person` FOREIGN KEY (`person_id`) REFERENCES `as_persons` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_connections_property` FOREIGN KEY (`property_id`) REFERENCES `as_properties` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;



there is one more view to fetch the connected persons to the property along with the properties: vw_properties

select `p`.`id` AS `id`,`p`.`title` AS `title`,`p`.`price_min` AS `price_min`,`p`.`price_max` AS `price_max`,`p`.`size_min` AS `size_min`,`p`.`size_max` AS `size_max`,`p`.`type` AS `type`,`p`.`locality` AS `locality`,`p`.`latitude` AS `latitude`,`p`.`longitude` AS `longitude`,`p`.`landmark_distance` AS `landmark_distance`,`p`.`description` AS `description`,`p`.`highlights` AS `highlights`,`p`.`status` AS `status`,`p`.`rating` AS `rating`,`p`.`tags` AS `tags`,`p`.`notes` AS `notes`,`p`.`city` AS `city`,`p`.`created_at` AS `created_at`,(select json_arrayagg(json_object('id',`per`.`id`,'name',`per`.`name`,'phone',`per`.`phone`,'roles',`per`.`roles`,'description',`per`.`description`,'bond_strength',`per`.`bond_strength`,'notes',`per`.`notes`,'role_on_this',`con`.`role`,'remark',`con`.`remark`)) from (`u240376517_propdb`.`as_connections` `con` join `u240376517_propdb`.`as_persons` `per` on(`con`.`person_id` = `per`.`id`)) where `con`.`property_id` = `p`.`id`) AS `connected_persons` from `u240376517_propdb`.`as_properties` `p`