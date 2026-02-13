insert into app_user (id, email, password_hash, role, created_at, created_by, updated_at, updated_by)
values ('22222222-2222-2222-2222-222222222222',
        'user@test.com',
        '$2a$10$rEBGnVLuVY6vGp5hLAFPyeUOOEw8bgQDp28/nUBIDihUg7FZGmjLq',  -- bcrypt("user")
        'USER',
        now(), 'system', now(), 'system');