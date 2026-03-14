UPDATE memberships m
SET display_name = trim(concat(coalesce(a.given_name, ''), ' ', coalesce(a.family_name, '')))
FROM accounts a
WHERE a.id = m.account_id
  AND coalesce(m.display_name, '') = '';
