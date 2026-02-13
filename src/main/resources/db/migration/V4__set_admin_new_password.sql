CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE app_user
SET password_hash = '$2a$10$1wqE2IwBG8TzoBhcsZcrTu1VvS42VriLqxYK/GYm.TuxFZu2UXgVy' -- там заруинил с паролем админа, не понял как он поменялся, сейчас пароль admin
WHERE email = 'admin@test.com';
