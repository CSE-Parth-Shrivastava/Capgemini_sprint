-- FinFlow – create all service databases
-- Runs automatically on first MySQL container start

CREATE DATABASE IF NOT EXISTS finflow_auth      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS finflow_application CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS finflow_document   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS finflow_notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS finflow_admin      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant all privileges to the finflow user
GRANT ALL PRIVILEGES ON finflow_auth.*         TO 'finflow'@'%';
GRANT ALL PRIVILEGES ON finflow_application.*  TO 'finflow'@'%';
GRANT ALL PRIVILEGES ON finflow_document.*     TO 'finflow'@'%';
GRANT ALL PRIVILEGES ON finflow_notification.* TO 'finflow'@'%';
GRANT ALL PRIVILEGES ON finflow_admin.*        TO 'finflow'@'%';

FLUSH PRIVILEGES;
