package com.example.medicalmcp.medicalcase.repository.impl;

import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.domain.SpecialtyCount;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class MedicalCaseRepositoryImpl implements MedicalCaseRepository {

    private static final String INSERT_SQL =
            """
            INSERT INTO medical_case (
                id, sample_name, description, transcription, medical_specialty, keywords, split, created_at
            ) VALUES (
                :id, :sampleName, :description, :transcription, :medicalSpecialty, :keywords, :split, :createdAt
            )
            """;

    private static final String SELECT_BY_ID =
            """
            SELECT id, sample_name, description, transcription, medical_specialty, keywords, split, created_at
            FROM medical_case
            WHERE id = :id
            """;

    private static final String COUNT_ALL = "SELECT COUNT(*) FROM medical_case";

    private final NamedParameterJdbcTemplate jdbc;

    public MedicalCaseRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<MedicalCase> findById(UUID id) {
        List<MedicalCase> rows =
                jdbc.query(SELECT_BY_ID, Map.of("id", id), (rs, rowNum) -> mapMedicalCase(rs));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    @Override
    public List<CaseSummary> fullTextSearch(String query, String specialty, String split, int limit) {
        throw new UnsupportedOperationException("fullTextSearch is implemented in milestone M3");
    }

    @Override
    public List<SpecialtyCount> listSpecialties() {
        throw new UnsupportedOperationException("listSpecialties is implemented in milestone M3");
    }

    @Override
    public long countAll() {
        Long count = jdbc.getJdbcTemplate().queryForObject(COUNT_ALL, Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public void insertBatch(List<MedicalCase> cases) {
        if (cases.isEmpty()) {
            return;
        }
        SqlParameterSource[] batch =
                cases.stream().map(this::toInsertParams).toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(INSERT_SQL, batch);
    }

    @Override
    public void updateEmbeddingsBatch(Map<UUID, float[]> embeddings) {
        throw new UnsupportedOperationException("updateEmbeddingsBatch is implemented in milestone M4");
    }

    private SqlParameterSource toInsertParams(MedicalCase medicalCase) {
        return new MapSqlParameterSource()
                .addValue("id", medicalCase.id())
                .addValue("sampleName", medicalCase.sampleName())
                .addValue("description", medicalCase.description())
                .addValue("transcription", medicalCase.transcription())
                .addValue("medicalSpecialty", medicalCase.medicalSpecialty())
                .addValue("keywords", medicalCase.keywords())
                .addValue("split", medicalCase.split())
                .addValue("createdAt", Timestamp.from(medicalCase.createdAt()));
    }

    private static MedicalCase mapMedicalCase(ResultSet rs) throws SQLException {
        return new MedicalCase(
                rs.getObject("id", UUID.class),
                rs.getString("sample_name"),
                rs.getString("description"),
                rs.getString("transcription"),
                rs.getString("medical_specialty"),
                rs.getString("keywords"),
                rs.getString("split"),
                toInstant(rs.getTimestamp("created_at")));
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
