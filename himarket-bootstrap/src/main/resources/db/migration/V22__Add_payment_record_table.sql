CREATE TABLE IF NOT EXISTS `payment_record` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `payment_record_id` varchar(64) NOT NULL,
    `order_id` varchar(64) NOT NULL,
    `provider` varchar(64) NOT NULL,
    `provider_transaction_id` varchar(128) DEFAULT NULL,
    `amount` decimal(10,2) NOT NULL,
    `currency` varchar(16) NOT NULL,
    `status` varchar(32) NOT NULL,
    `callback_payload` text,
    `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_record_id` (`payment_record_id`),
    KEY `idx_payment_record_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
