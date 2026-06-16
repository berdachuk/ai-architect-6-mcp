UPDATE medical_case
SET embedding = :embedding::vector
WHERE id = :id
