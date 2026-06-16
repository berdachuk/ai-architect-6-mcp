SELECT medical_specialty, COUNT(*) AS case_count
FROM medical_case
WHERE medical_specialty IS NOT NULL
GROUP BY medical_specialty
ORDER BY medical_specialty
