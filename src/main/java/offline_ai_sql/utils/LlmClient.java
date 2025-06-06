package offline_ai_sql.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(LlmClient.class);

    public String getSqlFromPrompt(String prompt) throws IOException {
        String safePrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");

        String jsonRequest = """
            {
              "model": "llama3",
              "messages": [
                {
                  "role": "user",
                  "content": "%s"
                }
              ],
              "stream": false
            }
        """.formatted(safePrompt);

        logger.debug("Sending request to LLaMA: {}", jsonRequest);

        URL url = new URL("http://localhost:11434/api/chat");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonRequest.getBytes());
            os.flush();
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    error.append(line);
                }
                logger.error("LLaMA server error: HTTP {} - {}", status, error);
                throw new IOException("LLaMA server error: HTTP " + status + " - " + error);
            }
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        logger.debug("LLaMA response: {}", response);
        return extractResponse(response.toString());
    }

    private String extractResponse(String json) throws IOException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode contentNode = root.path("message").path("content");
            if (contentNode.isMissingNode()) {
                logger.error("Invalid LLaMA response format: 'message.content' not found in {}", json);
                throw new IOException("Invalid response format: 'message.content' not found");
            }
            String content = contentNode.asText();
            String sqlQuery = extractSqlQuery(content);
            if (sqlQuery == null) {
                logger.error("No valid SQL query found in response: {}", content);
                throw new IOException("No valid SQL query found in response");
            }
            logger.debug("Extracted SQL query: {}", sqlQuery);
            return sqlQuery;
        } catch (Exception e) {
            logger.error("Failed to parse LLaMA response: {}", json, e);
            throw new IOException("Failed to parse LLaMA response: " + e.getMessage(), e);
        }
    }

    private String extractSqlQuery(String content) {
        // Extract SQL query from ```sql ... ``` block
        String sqlBlockPattern = "(?s)```sql\\n(.*?)\\n```";
        Pattern pattern = Pattern.compile(sqlBlockPattern);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String query = matcher.group(1).trim();
            // Validate that the query starts with an allowed SQL keyword
            if (isValidSqlQuery(query)) {
                return query;
            }
        }

        // Fallback: Check if content starts with a valid SQL keyword
        content = content.trim();
        if (isValidSqlQuery(content)) {
            return content;
        }

        return null; // No valid SQL query found
    }

    private boolean isValidSqlQuery(String query) {
        String[] validSqlStarts = {"SELECT", "INSERT", "UPDATE", "DELETE"};
        query = query.trim().toUpperCase();
        for (String keyword : validSqlStarts) {
            if (query.startsWith(keyword)) {
                return true;
            }
        }
        return false;
    }
}