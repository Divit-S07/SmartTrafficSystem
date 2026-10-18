-- ============================================================================
-- VEGAM CITY SMART TRAFFIC MANAGEMENT & EMERGENCY ROUTING
-- MySQL Database Schema
-- Compatible with MySQL 5.7+ / 8.0+
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `smart_traffic_db` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `smart_traffic_db`;

-- 1. Junctions (Nodes of the road network graph)
CREATE TABLE IF NOT EXISTS `junctions` (
    `id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `name` VARCHAR(100) NULL,
    `x_coord` INT DEFAULT 0,
    `y_coord` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Road Segments (Directed weighted edges)
CREATE TABLE IF NOT EXISTS `roads` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `from_junction` VARCHAR(50) NOT NULL,
    `to_junction` VARCHAR(50) NOT NULL,
    `base_travel_time` DECIMAL(8, 2) NOT NULL,
    `current_travel_time` DECIMAL(8, 2) NOT NULL,
    `is_closed` TINYINT(1) DEFAULT 0,
    `is_two_way` TINYINT(1) DEFAULT 0,
    `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_road_segment` (`from_junction`, `to_junction`),
    FOREIGN KEY (`from_junction`) REFERENCES `junctions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`to_junction`) REFERENCES `junctions`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Emergency Vehicles
CREATE TABLE IF NOT EXISTS `vehicles` (
    `id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `vehicle_type` ENUM('AMBULANCE', 'FIRE_ENGINE', 'RESCUE') NOT NULL,
    `current_junction` VARCHAR(50) NOT NULL,
    `destination_junction` VARCHAR(50) NULL,
    `status` ENUM('IDLE', 'EN_ROUTE', 'ARRIVED', 'UNREACHABLE') DEFAULT 'IDLE',
    `current_route` TEXT NULL,
    `route_travel_time` DECIMAL(8, 2) NULL,
    `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`current_junction`) REFERENCES `junctions`(`id`)
) ENGINE=InnoDB;

-- 4. Emergency Routing Requests (Priority Queue logs & state)
CREATE TABLE IF NOT EXISTS `routing_requests` (
    `id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `vehicle_id` VARCHAR(50) NOT NULL,
    `destination_junction` VARCHAR(50) NOT NULL,
    `severity` ENUM('CRITICAL', 'HIGH', 'NORMAL') NOT NULL,
    `severity_rank` INT NOT NULL, -- 3 for CRITICAL, 2 for HIGH, 1 for NORMAL
    `arrival_sequence` INT NOT NULL,
    `status` ENUM('PENDING', 'PROCESSED', 'ESCALATED', 'FAILED') DEFAULT 'PENDING',
    `computed_route` TEXT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`destination_junction`) REFERENCES `junctions`(`id`)
) ENGINE=InnoDB;

-- 5. Traffic Events (FIFO Queue: Closures, Reopenings, Congestion)
CREATE TABLE IF NOT EXISTS `traffic_events` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `sequence_num` INT NOT NULL,
    `event_type` ENUM('CLOSE', 'REOPEN', 'CONGESTION') NOT NULL,
    `from_junction` VARCHAR(50) NOT NULL,
    `to_junction` VARCHAR(50) NOT NULL,
    `slowdown_factor` DECIMAL(5, 2) DEFAULT 1.00,
    `description` VARCHAR(255) NOT NULL,
    `processed` TINYINT(1) DEFAULT 0,
    `processed_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 6. System Audit Logs / Cloud Sync Snapshots
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `action` VARCHAR(100) NOT NULL,
    `details` TEXT NOT NULL,
    `logged_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Seed Sample Data (Corresponds to Vegam City demo network)
INSERT IGNORE INTO `junctions` (`id`, `name`, `x_coord`, `y_coord`) VALUES
('J1', 'Central Hub', 100, 200),
('J2', 'North Ring East', 220, 100),
('J3', 'East Express Junction', 380, 100),
('J4', 'Suburban Terminal', 540, 100),
('J5', 'Metro Plaza', 220, 300),
('J6', 'Tech Park North', 380, 300),
('J7', 'East Boulevard', 540, 300),
('J8', 'South Spine A', 100, 420),
('J9', 'South Spine B', 220, 420),
('J10', 'Airport Interchange', 380, 420),
('J11', 'Medical City', 540, 420),
('J12', 'Disaster Relief Base', 660, 300);

-- Backbone Roads
INSERT IGNORE INTO `roads` (`from_junction`, `to_junction`, `base_travel_time`, `current_travel_time`, `is_closed`, `is_two_way`) VALUES
('J1', 'J2', 5.0, 5.0, 0, 1),
('J2', 'J1', 5.0, 5.0, 0, 1),
('J2', 'J3', 4.0, 4.0, 0, 1),
('J3', 'J2', 4.0, 4.0, 0, 1),
('J3', 'J4', 3.0, 3.0, 0, 1),
('J4', 'J3', 3.0, 3.0, 0, 1),
('J1', 'J5', 6.0, 6.0, 0, 1),
('J5', 'J1', 6.0, 6.0, 0, 1),
('J5', 'J6', 3.0, 3.0, 0, 1),
('J6', 'J5', 3.0, 3.0, 0, 1),
('J6', 'J7', 4.0, 4.0, 0, 1),
('J7', 'J6', 4.0, 4.0, 0, 1),
('J5', 'J9', 4.0, 4.0, 0, 1),
('J9', 'J5', 4.0, 4.0, 0, 1),
('J6', 'J10', 5.0, 5.0, 0, 1),
('J10', 'J6', 5.0, 5.0, 0, 1),
('J2', 'J6', 7.0, 7.0, 0, 0),
('J3', 'J7', 2.0, 2.0, 0, 0),
('J5', 'J11', 8.0, 8.0, 0, 0),
('J6', 'J12', 6.0, 6.0, 0, 0);

-- Initial Emergency Vehicles
INSERT IGNORE INTO `vehicles` (`id`, `vehicle_type`, `current_junction`, `status`) VALUES
('AMB1', 'AMBULANCE', 'J1', 'IDLE'),
('AMB2', 'AMBULANCE', 'J11', 'IDLE'),
('FIRE1', 'FIRE_ENGINE', 'J4', 'IDLE'),
('RESCUE1', 'RESCUE', 'J8', 'IDLE');

