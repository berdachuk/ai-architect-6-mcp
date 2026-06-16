INSERT INTO medical_case (
    id, sample_name, description, transcription, medical_specialty, keywords, split, created_at
) VALUES (
    :id, :sampleName, :description, :transcription, :medicalSpecialty, :keywords, :split, :createdAt
)
