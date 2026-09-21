UPDATE rewards_catalog
SET active = 0
WHERE reward_type = 'PAID' OR category = 'PARTNER';

UPDATE rewards_catalog
SET sizes_json = '["XS","S","M","L","XL"]', partner_name = 'Runnit'
WHERE category = 'APPAREL';

INSERT INTO rewards_catalog
    (title, description, category, reward_type, points_cost, inventory, sizes_json, partner_name, active)
VALUES
    ('Runnit everyday hoodie', 'A heavyweight Runnit hoodie for warmups, cooldowns, and rest days.', 'APPAREL', 'POINTS', 5000, 50, '["XS","S","M","L","XL"]', 'Runnit', 1),
    ('50-mile club badge', 'A digital badge celebrating your first 50 miles with Runnit.', 'DIGITAL', 'POINTS', 500, NULL, NULL, 'Runnit', 1),
    ('100-mile club badge', 'A digital badge celebrating 100 miles logged with Runnit.', 'DIGITAL', 'POINTS', 1000, NULL, NULL, 'Runnit', 1);
