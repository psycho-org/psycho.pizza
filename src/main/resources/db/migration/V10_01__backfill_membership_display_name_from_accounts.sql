UPDATE memberships m
SET display_name = left(trim(concat(coalesce(a.given_name, ''), ' ', coalesce(a.family_name, ''))), 255)
FROM accounts a
WHERE a.id = m.account_id
  AND coalesce(m.display_name, '') = '';
