package org.example.speechhelper.AI.contreller;

import lombok.RequiredArgsConstructor;
import org.example.speechhelper.AI.service.AiInterviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/v1/interview")
@RequiredArgsConstructor
public class AiInterviewController {
    private final AiInterviewService aiInterviewService;

    @PostMapping("/feedback")
    public ResponseEntity<String> getFeedback(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String answer = request.get("answer");

        String feedback = aiInterviewService.getInterviewFeedback(question, answer);
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/feedback")
    public String interviewPage() {
        return "interview";
    }
}
