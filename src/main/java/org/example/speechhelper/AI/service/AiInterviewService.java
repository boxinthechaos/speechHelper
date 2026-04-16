package org.example.speechhelper.AI.service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiInterviewService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getInterviewFeedback(String question, String answer) {
        // 💡 여기 URL에도 apiKey가 들어가도록 수정했습니다!
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        String prompt = String.format(
                "너는 10년 차 전문 면접관이야. 다음 면접 질문과 사용자의 답변을 보고, 장점과 개선점을 포함하여 팩트 폭격 피드백을 해줘.\n\n[면접 질문]: %s\n[사용자 답변]: %s",
                question, answer
        );

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", List.of(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(parts));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String aiAnswer = rootNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            return aiAnswer.trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "AI 피드백을 받아오는 중 문제가 발생했습니다. 면접관이 자리를 비웠네요!";
        }
    }
}