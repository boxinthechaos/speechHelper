package org.example.speechhelper.Interview.AI.service;

import lombok.RequiredArgsConstructor;
import org.example.speechhelper.Interview.entity.InterviewHistory;
import org.example.speechhelper.Interview.entity.InterviewQuestion; // 💡 빠졌던 Import 추가!
import org.example.speechhelper.Interview.repository.InterviewHistoryRepository;
import org.example.speechhelper.Interview.repository.InterviewQuestionRepository; // 💡 빠졌던 Import 추가!
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

    // 💡 두 개의 DB 리포지토리를 모두 주입받도록 수정했습니다.
    private final InterviewHistoryRepository historyRepository;
    private final InterviewQuestionRepository questionRepository;

    public InterviewHistory createAndSaveFeedback(Long questionId, String userAnswer, String username) {
        // 1. DB 질문 창고에서 질문과 모범 답안을 가져옴
        InterviewQuestion questionData = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=" + apiKey;

        String prompt = String.format( "너는 기술 면접관이야. 아래 정보를 바탕으로 사용자의 답변을 평가해줘.\n\n" + "[면접 질문]: %s\n" + "[모범 답안(기준)]: %s\n" + "[사용자 답변]: %s\n\n" + "평가 기준:\n" + "1. 사용자 답변이 모범 답안의 핵심 키워드를 포함하고 있는지 확인해.\n" + "2. 만약 모범 답안과 표현이 다르더라도, 기술적으로 논리적이고 타당하다면 긍정적으로 평가해.\n" + "3. 팩폭 스타일로 피드백하되, 부족한 개념이 있다면 모범 답안을 참고해서 보완할 점을 명확히 짚어줘.", questionData.getQuestion(), questionData.getSampleAnswer(), userAnswer );

        String feedback = callGeminiApi(url, prompt);

        // 💡 엔티티를 생성할 때 username도 같이 넣어서 DB에 저장합니다!
        InterviewHistory history = new InterviewHistory(questionData.getQuestion(), userAnswer, feedback, username);
        return historyRepository.save(history);
    }

    private String callGeminiApi(String url, String prompt) {
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

    public List<InterviewHistory> getMyHistory(String username) {
        return historyRepository.findAllByUsername(username);
    }

    public List<InterviewQuestion> getAllQuestions() {
        return questionRepository.findAll();
    }

    public void deleteHistory(Long historyId, String username){
        InterviewHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기록입니다."));

        if (!history.getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 기록만 삭제할 수 있습니다.");
        }

        historyRepository.delete(history);
    }
}