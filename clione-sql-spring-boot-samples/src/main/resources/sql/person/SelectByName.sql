SELECT
  id, name, status, created_at
FROM
  person
WHERE
  name = /* $name */'dummy'
ORDER BY
  id
