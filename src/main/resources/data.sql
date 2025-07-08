-- 初始化管理员用户
INSERT IGNORE INTO users (username, password, email, real_name, role, status, created_at, updated_at)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'admin@taiyi.com', '系统管理员', 'ADMIN', 'ACTIVE', NOW(), NOW());

-- 注意：上面的密码是 'admin123' 经过BCrypt加密后的结果
