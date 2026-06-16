SELECT split, COUNT(*) AS case_count
FROM medical_case
WHERE split IS NOT NULL
GROUP BY split
