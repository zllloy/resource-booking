insert into app_user (id, email, password_hash, role,
                      created_at, created_by, updated_at, updated_by)
values ('11111111-1111-1111-1111-111111111111',
        'admin@test.com',
        '$2a$10$7QJv3f1fK6mK1j8nWjC9yO2oC0Vx0b2b7l2ZyqYh9p8QH7v5cFz9S', -- bcrypt("admin")
        'ADMIN',
        now(), 'seed', now(), 'seed');
