SELECT id, sample_name, description, transcription, medical_specialty, keywords, split, created_at
FROM medical_case
WHERE embedding IS NULL
ORDER BY created_at
