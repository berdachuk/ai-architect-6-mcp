package com.example.medicalmcp.medicalcase.repository.impl;

import com.example.medicalmcp.core.repository.sql.InjectSql;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.domain.SemanticMatch;
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

    @InjectSql("/sql/medicalcase/insert.sql")
    private String insertSql;

    @InjectSql("/sql/medicalcase/selectById.sql")
    private String selectByIdSql;

    @InjectSql("/sql/medicalcase/countAll.sql")
    private String countAllSql;

    @InjectSql("/sql/medicalcase/fullTextSearch.sql")
    private String fullTextSearchSql;

    @InjectSql("/sql/medicalcase/listSpecialties.sql")
    private String listSpecialtiesSql;

    @InjectSql("/sql/medicalcase/countBySplit.sql")
    private String countBySplitSql;

    @InjectSql("/sql/medicalcase/findWithoutEmbeddings.sql")
    private String findWithoutEmbeddingsSql;

    @InjectSql("/sql/medicalcase/updateEmbedding.sql")
    private String updateEmbeddingSql;

    @InjectSql("/sql/medicalcase/semanticSearch.sql")
    private String semanticSearchSql;

    private final NamedParameterJdbcTemplate jdbc;

    public MedicalCaseRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<MedicalCase> findById(UUID id) {
        List<MedicalCase> rows =
                jdbc.query(selectByIdSql, Map.of("id", id), (rs, rowNum) -> mapMedicalCase(rs));
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

        return jdbc.query(
                fullTextSearchSql,
                new MapSqlParameterSource()
                        .addValue("query", query.trim())
                        .addValue("specialty", StringUtils.hasText(specialty) ? specialty : "")
                        .addValue("split", StringUtils.hasText(split) ? split : "")
                        .addValue("limit", limit),
                (rs, rowNum) -> mapCaseSummary(rs));
    }

    @Override
    public List<SpecialtyCount> listSpecialties() {
        return jdbc.query(listSpecialtiesSql, Map.of(), (rs, rowNum) -> new SpecialtyCount(
                rs.getString("medical_specialty"), rs.getLong("case_count")));
    }

    @Override
    public Map<String, Long> countBySplit() {
        Map<String, Long> counts = new HashMap<>();
        jdbc.query(countBySplitSql, Map.of(), rs -> {
            counts.put(rs.getString("split"), rs.getLong("case_count"));
        });
        return Map.copyOf(counts);
    }

    @Override
    public long countAll() {
        Long count = jdbc.queryForObject(countAllSql, Map.of(), Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public void insertBatch(List<MedicalCase> cases) {
        if (cases.isEmpty()) {
            return;
        }
        SqlParameterSource[] batch =
                cases.stream().map(this::toInsertParams).toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(insertSql, batch);
    }

    @Override
    public List<MedicalCase> findWithoutEmbeddings() {
        return jdbc.query(findWithoutEmbeddingsSql, Map.of(), (rs, rowNum) -> mapMedicalCase(rs));
    }

    @Override
    public List<SemanticMatch> semanticSearch(
            float[] queryEmbedding, String specialty, int topK, double minSimilarity) {
        if (queryEmbedding == null || queryEmbedding.length == 0 || topK <= 0) {
            return List.of();
        }
        return jdbc.query(
                semanticSearchSql,
                new MapSqlParameterSource()
                        .addValue("embedding", formatVector(queryEmbedding))
                        .addValue("specialty", StringUtils.hasText(specialty) ? specialty : "")
                        .addValue("minSimilarity", minSimilarity)
                        .addValue("topK", topK),
                (rs, rowNum) -> new SemanticMatch(mapCaseSummary(rs), rs.getDouble("similarity")));
    }

    @Override
    public void updateEmbeddingsBatch(Map<UUID, float[]> embeddings) {
        if (embeddings.isEmpty()) {
            return;
        }
        SqlParameterSource[] batch = embeddings.entrySet().stream()
                .map(entry -> new MapSqlParameterSource()
                        .addValue("id", entry.getKey())
                        .addValue("embedding", formatVector(entry.getValue())))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(updateEmbeddingSql, batch);
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

    private static String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
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
