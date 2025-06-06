package offline_ai_sql.controller;

import offline_ai_sql.model.PromptRequest;
import offline_ai_sql.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final QueryService queryService;

    @Autowired
    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<?> ask(@RequestBody PromptRequest promptRequest) {
        try {
            if (promptRequest == null || promptRequest.getPrompt() == null || promptRequest.getPrompt().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Prompt cannot be null or empty");
            }
            Object result = queryService.handlePrompt(promptRequest.getPrompt());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid prompt or SQL: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }
}