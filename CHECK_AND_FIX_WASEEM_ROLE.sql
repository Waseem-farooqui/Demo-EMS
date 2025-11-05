-- Check current roles for user 'waseem'
SELECT u.id, u.username, u.email, ur.role
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.username = 'waseem';

-- If the role is missing or incorrect, run these commands:

-- First, remove any existing roles for waseem (if needed)
-- DELETE FROM user_roles WHERE user_id = (SELECT id FROM users WHERE username = 'waseem');

-- Then add SUPER_ADMIN role
-- INSERT INTO user_roles (user_id, role)
-- VALUES ((SELECT id FROM users WHERE username = 'waseem'), 'SUPER_ADMIN');

-- Verify the fix
-- SELECT u.id, u.username, u.email, ur.role
-- FROM users u
-- LEFT JOIN user_roles ur ON u.id = ur.user_id
-- WHERE u.username = 'waseem';

