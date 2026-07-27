INSERT IGNORE INTO users (username, email, password) VALUES
    ('user1', 'johndoe@example.com', 'hashed_password_123'),
    ('user2', 'janedoe@example.com', 'hashed_password_456'),
    ('user3', 'user123@example.com', 'hashed_password_789');

INSERT IGNORE INTO available_slots (start_time, end_time, is_reserved) VALUES
    ('2026-08-01 09:00:00', '2026-08-01 10:00:00', FALSE),
    ('2026-08-01 10:00:00', '2026-08-01 11:00:00', FALSE),
    ('2026-08-01 11:00:00', '2026-08-01 12:00:00', FALSE),
    ('2026-08-01 12:00:00', '2026-08-01 13:00:00', FALSE),
    ('2026-08-01 13:00:00', '2026-08-01 14:00:00', FALSE),
    ('2026-08-01 14:00:00', '2026-08-01 15:00:00', FALSE),
    ('2026-08-01 15:00:00', '2026-08-01 16:00:00', FALSE),
    ('2026-08-01 16:00:00', '2026-08-01 17:00:00', FALSE),
    ('2026-08-02 09:00:00', '2026-08-02 10:00:00', FALSE),
    ('2026-08-02 10:00:00', '2026-08-02 11:00:00', FALSE);