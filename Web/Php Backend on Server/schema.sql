-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1:3306
-- Generation Time: Jan 01, 2026 at 11:38 AM
-- Server version: 11.8.3-MariaDB-log
-- PHP Version: 7.2.34

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `u542940820_easycalls`
--

-- --------------------------------------------------------

--
-- Table structure for table `calls`
--

CREATE TABLE `calls` (
  `id` int(11) NOT NULL,
  `unique_id` char(50) NOT NULL,
  `org_id` varchar(20) NOT NULL,
  `employee_id` varchar(10) NOT NULL,
  `device_phone` varchar(20) DEFAULT NULL,
  `caller_name` varchar(100) DEFAULT NULL,
  `caller_phone` varchar(20) DEFAULT NULL,
  `duration` int(11) DEFAULT 0,
  `type` varchar(20) DEFAULT NULL,
  `recording_url` text DEFAULT NULL,
  `note` text DEFAULT NULL,
  `upload_status` enum('pending','completed','failed') DEFAULT 'pending',
  `created_at` datetime DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `number_id` int(11) DEFAULT NULL,
  `call_time` datetime DEFAULT NULL,
  `is_archived` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Triggers `calls`
--
DELIMITER $$
CREATE TRIGGER `after_call_insert` AFTER INSERT ON `calls` FOR EACH ROW BEGIN INSERT INTO contacts (org_id, employee_id, phone, name, incomings, incoming_connected, outgoings, outgoing_connected, last_call_type, last_call_duration, last_call_time) VALUES (NEW.org_id, NEW.employee_id, NEW.caller_phone, NEW.caller_name, IF(NEW.type = "Inbound", 1, 0), IF(NEW.type = "Inbound" AND NEW.duration > 0, 1, 0), IF(NEW.type = "Outbound", 1, 0), IF(NEW.type = "Outbound" AND NEW.duration > 0, 1, 0), NEW.type, NEW.duration, NEW.call_time) ON DUPLICATE KEY UPDATE employee_id = IF(NEW.employee_id IS NOT NULL, NEW.employee_id, employee_id), name = IF(NEW.caller_name IS NOT NULL AND NEW.caller_name != "", NEW.caller_name, name), incomings = incomings + IF(NEW.type = "Inbound", 1, 0), incoming_connected = incoming_connected + IF(NEW.type = "Inbound" AND NEW.duration > 0, 1, 0), outgoings = outgoings + IF(NEW.type = "Outbound", 1, 0), outgoing_connected = outgoing_connected + IF(NEW.type = "Outbound" AND NEW.duration > 0, 1, 0), last_call_type = NEW.type, last_call_duration = NEW.duration, last_call_time = NEW.call_time, updated_at = NOW(); END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `contacts`
--

CREATE TABLE `contacts` (
  `id` int(11) NOT NULL,
  `org_id` varchar(6) NOT NULL,
  `employee_id` int(11) DEFAULT NULL,
  `phone` varchar(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `label` varchar(50) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `incomings` int(11) DEFAULT 0,
  `incoming_connected` int(11) DEFAULT 0,
  `outgoings` int(11) DEFAULT 0,
  `outgoing_connected` int(11) DEFAULT 0,
  `last_call_type` varchar(20) DEFAULT NULL,
  `last_call_duration` int(11) DEFAULT 0,
  `last_call_time` datetime DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `employees`
--

CREATE TABLE `employees` (
  `id` int(11) NOT NULL,
  `org_id` varchar(6) NOT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `status` enum('active','inactive') DEFAULT 'active',
  `join_date` date NOT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `call_track` tinyint(1) DEFAULT 1,
  `call_record_crm` tinyint(1) DEFAULT 1,
  `expiry_date` datetime DEFAULT NULL,
  `last_sync` datetime DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `allow_personal_exclusion` tinyint(1) DEFAULT 0,
  `allow_changing_tracking_start_date` tinyint(1) DEFAULT 0,
  `allow_updating_tracking_sims` tinyint(1) DEFAULT 0,
  `default_tracking_starting_date` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- --------------------------------------------------------

--
-- Table structure for table `excluded_contacts`
--

CREATE TABLE `excluded_contacts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `org_id` varchar(20) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_excluded_org` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `sessions`
--

CREATE TABLE `sessions` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `token` varchar(64) NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `settings`
--

CREATE TABLE `settings` (
  `id` int(11) NOT NULL,
  `org_id` varchar(6) NOT NULL,
  `setting_key` varchar(100) NOT NULL,
  `setting_value` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `org_id` varchar(6) NOT NULL,
  `name` varchar(255) NOT NULL,
  `org_name` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('admin','manager','user') DEFAULT 'admin',
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `plan_info` text DEFAULT NULL,
  `status` varchar(50) DEFAULT 'active'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `calls`
--
ALTER TABLE `calls`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_id` (`unique_id`),
  ADD KEY `idx_phone` (`caller_phone`);

--
-- Indexes for table `contacts`
--
ALTER TABLE `contacts`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_org_phone` (`org_id`,`phone`),
  ADD KEY `idx_phone` (`phone`),
  ADD KEY `idx_label` (`label`),
  ADD KEY `idx_employee_id` (`employee_id`);

--
-- Indexes for table `employees`
--
ALTER TABLE `employees`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_org_id` (`org_id`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_device_id` (`device_id`);

--
-- Indexes for table `sessions`
--
ALTER TABLE `sessions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `token` (`token`),
  ADD KEY `idx_token` (`token`),
  ADD KEY `idx_user` (`user_id`);

--
-- Indexes for table `settings`
--
ALTER TABLE `settings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_org_setting` (`org_id`,`setting_key`),
  ADD KEY `idx_org_id` (`org_id`),
  ADD KEY `idx_setting_key` (`setting_key`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `idx_email` (`email`),
  ADD KEY `idx_org_id` (`org_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `calls`
--
ALTER TABLE `calls`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `contacts`
--
ALTER TABLE `contacts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `employees`
--
ALTER TABLE `employees`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `sessions`
--
ALTER TABLE `sessions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `settings`
--
ALTER TABLE `settings`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `contacts`
--
ALTER TABLE `contacts`
  ADD CONSTRAINT `fk_contact_employee` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `sessions`
--
ALTER TABLE `sessions`
  ADD CONSTRAINT `sessions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
