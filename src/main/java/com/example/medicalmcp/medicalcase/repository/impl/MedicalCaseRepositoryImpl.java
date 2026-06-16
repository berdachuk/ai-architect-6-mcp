package com.example.medicalmcp.medicalcase.repository.impl;

import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.domain.SpecialtyCount;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class MedicalCaseRepositoryImpl implements MedicalCaseRepository {

    private static final Set<String> VALID_SPLITS = Set.of("train", "validation", "test");

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

    private static final String FTS_SEARCH_BASE =
            """
            SELECT id, sample_name, description, medical_specialty, keywords, split
            FROM medical_case
            WHERE fts @@ plainto_tsquery('english', :query)
            """;

    private static final String LIST_SPECIALTIES =
            """
            SELECT medical_specialty, COUNT(*) AS case_count
            FROM medical_case
            WHERE medical_specialty IS NOT NULL
            GROUP BY medical_specialty
            ORDER BY medical_specialty
            """;

    private static final String COUNT_BY_SPLIT =
            """
            SELECT split, COUNT(*) AS case_count
            FROM medical_case
            WHERE split IS NOT NULL
            GROUP BY split
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
        if (!StringUtils.hasText(query) || limit <= 0) {
            return List.of();
        }
        if (StringUtils.hasText(split) && !VALID_SPLITS.contains(split)) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder(FTS_SEARCH_BASE);
        MapSqlParameterSource params = new MapSqlParameterSource("query", query.trim());
        if (StringUtils.hasText(specialty)) {
            sql.append(" AND medical_specialty = :specialty");
            params.addValue("specialty", specialty);
        }
        if (StringUtils.hasText(split)) {
            sql.append(" AND split = :split");
            params.addValue("split", split);
        }
        sql.append(" ORDER BY ts_rank(fts, plainto_tsquery('english', :query)) DESC LIMIT :limit");
        params.addValue("limit", limit);

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapCaseSummary(rs));
    }

    @Override
    public List<SpecialtyCount> listSpecialties() {
        return jdbc.query(LIST_SPECIALTIES, Map.of(), (rs, rowNum) -> new SpecialtyCount(
                rs.getString("medical_specialty"), rs.getLong("case_count")));
    }

    @Override
    public Map<String, Long> countBySplit() {
        Map<String, Long> counts = new HashMap<>();
        jdbc.query(COUNT_BY_SPLIT, Map.of(), rs -> {
            counts.put(rs.getString("split"), rs.getLong("case_count"));
        });
        return Map.copyOf(counts);
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

    private static CaseSummary mapCaseSummary(ResultSet rs) throws SQLException {
        return new CaseSummary(
                rs.getObject("id", UUID.class),
                rs.getString("sample_name"),
                rs.getString("description"),
                rs.getString("medical_specialty"),
                rs.getString("keywords"),
                rs.getString("split"));
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
