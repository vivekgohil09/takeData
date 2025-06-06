package offline_ai_sql.service;

import offline_ai_sql.utils.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);
    private String cachedSchema = null;

    private final LlmClient llmClient;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public QueryService(LlmClient llmClient, JdbcTemplate jdbcTemplate) {
        this.llmClient = llmClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Object handlePrompt(String prompt) {
        logger.info("Processing prompt: {}", prompt);
        String schema;
        try {
            if (cachedSchema == null) {
                cachedSchema = getAllTableSchemas();
                logger.debug("Database schema cached: {}", cachedSchema);
            }
            schema = cachedSchema;
        } catch (Exception e) {
            logger.error("Failed to build table schema", e);
            throw new RuntimeException("Failed to build table schema: " + e.getMessage(), e);
        }

        String fullPrompt = schema + "\nPrompt: " + prompt;
        String sql = null; // Initialize to null

        try {
            sql = llmClient.getSqlFromPrompt(fullPrompt);
            logger.info("Generated SQL: {}", sql);

            if (sql == null || sql.trim().isEmpty()) {
                throw new IllegalArgumentException("LLM did not return a valid SQL query.");
            }

            String sqlLower = sql.trim().toLowerCase();
            if (sqlLower.startsWith("select")) {
                return jdbcTemplate.queryForList(sql);
            } else if (sqlLower.startsWith("insert") || sqlLower.startsWith("update") || sqlLower.startsWith("delete")) {
                int affected = jdbcTemplate.update(sql);
                return Map.of("rowsAffected", affected);
            } else {
                throw new IllegalArgumentException("Only SELECT, INSERT, UPDATE, DELETE are allowed.");
            }
        } catch (Exception e) {
            logger.error("Query execution failed{}", sql != null ? " for SQL: " + sql : "", e);
            throw new RuntimeException("Query execution failed: " + e.getMessage() + (sql != null ? " (SQL: " + sql + ")" : ""), e);
        }
    }

    private String getAllTableSchemas() throws Exception {
        StringBuilder schemaBuilder = new StringBuilder();
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            var meta = conn.getMetaData();
            try (ResultSet rsTables = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rsTables.next()) {
                    String tableName = rsTables.getString("TABLE_NAME");
                    try (ResultSet rsColumns = meta.getColumns(null, null, tableName, null)) {
                        schemaBuilder.append("Table: ").append(tableName).append("(");
                        boolean first = true;
                        while (rsColumns.next()) {
                            if (!first) schemaBuilder.append(", ");
                            String colName = rsColumns.getString("COLUMN_NAME");
                            String colType = rsColumns.getString("TYPE_NAME");
                            schemaBuilder.append(colName).append(" ").append(colType);
                            first = false;
                        }
                        schemaBuilder.append(")\n");
                    }
                }
            }
        }
        String schema = schemaBuilder.toString();
        if (schema.isEmpty()) {
            logger.warn("No tables found in the database schema");
            throw new RuntimeException("No tables found in the database");
        }
        return schema;
    }
}