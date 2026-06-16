SELECT id, sample_name, description, medical_specialty, keywords, split
FROM medical_case
WHERE fts @@ plainto_tsquery('english', :query)
  AND (COALESCE(:specialty, '') = '' OR medical_specialty = :specialty)
  AND (COALESCE(:split, '') = '' OR split = :split)
ORDER BY ts_rank(fts, plainto_tsquery('english', :query)) DESC
LIMIT :limit
