SELECT id, sample_name, description, medical_specialty, keywords, split,
       1 - (embedding <=> :embedding::vector) AS similarity
FROM medical_case
WHERE embedding IS NOT NULL
  AND (COALESCE(:specialty, '') = '' OR medical_specialty = :specialty)
  AND (1 - (embedding <=> :embedding::vector)) >= :minSimilarity
ORDER BY embedding <=> :embedding::vector
LIMIT :topK
