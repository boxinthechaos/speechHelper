package org.example.speechhelper.Interview.AI.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import org.example.speechhelper.Interview.entity.InterviewHistory;
import org.example.speechhelper.Interview.entity.InterviewQuestion; // 💡 빠졌던 Import 추가!
import org.example.speechhelper.Interview.entity.PortfolioEvaluation;
import org.example.speechhelper.Interview.repository.InterviewHistoryRepository;
import org.example.speechhelper.Interview.repository.InterviewQuestionRepository; // 💡 빠졌던 Import 추가!
import org.example.speechhelper.Interview.repository.PortfolioEvaluationRepository;
import org.jsoup.Jsoup;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
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

import java.time.Duration;
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
    private final PortfolioEvaluationRepository portfolioEvaluationRepository;

    // 💡 두 개의 DB 리포지토리를 모두 주입받도록 수정했습니다.
    private final InterviewHistoryRepository historyRepository;
    private final InterviewQuestionRepository questionRepository;

    public InterviewHistory createAndSaveFeedback(Long questionId, String userAnswer, String username) {
        // 1. DB 질문 창고에서 질문과 모범 답안을 가져옴
        InterviewQuestion questionData = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=" + apiKey;

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

    public String evaluatePortfolio(String portfolioUrl, String username) {
        // 💡 1. Jsoup 대신 새로 만든 Selenium 크롤러(crawlPortfolio)를 호출합니다!
        String portfolioContent = crawlPortfolio(portfolioUrl);

        // 💡 2. Gemini 토큰 제한 대비 너무 긴 텍스트는 잘라냅니다.
        if (portfolioContent.length() > 4000) {
            portfolioContent = portfolioContent.substring(0, 4000) + "...(이하 생략)";
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=" + apiKey;

        String prompt = String.format(
                "너는 10년 경력의 시니어 개발자 면접관이야. 아래 포트폴리오를 보고 종합 평가를 해줘.\n\n" +
                        "[포트폴리오 내용]:\n%s\n\n" +
                        "아래 항목별로 평가해줘:\n" +
                        "1. 📌 기술 스택 분석 - 어떤 기술을 사용했는지, 수준은 어느 정도인지\n" +
                        "2. 💼 프로젝트 완성도 - 프로젝트의 규모와 완성도\n" +
                        "3. ✅ 잘한 점 - 포트폴리오에서 인상적인 부분\n" +
                        "4. ⚠️ 부족한 점 - 보완하면 좋을 부분\n" +
                        "5. 💡 면접 예상 질문 3개 - 이 포트폴리오를 보고 면접관이 물어볼 것 같은 질문\n\n" +
                        "팩폭 스타일로 솔직하게 평가해줘.",
                portfolioContent
        );

        // 💡 3. API는 딱 한 번만 호출해서 feedback 변수에 담습니다.
        String feedback = callGeminiApi(url, prompt);

        // 4. DB에 저장
        PortfolioEvaluation evaluation = new PortfolioEvaluation(username, portfolioUrl, feedback);
        portfolioEvaluationRepository.save(evaluation);

        // 💡 5. 저장한 피드백을 그대로 프론트엔드로 반환합니다 (중복 호출 X)
        return feedback;
    }

    private String fetchPortfolioContent(String portfolioUrl) {
        try {
            String text = Jsoup.connect(portfolioUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get()
                    .text();

            // Gemini 토큰 제한 대비 자르기
            if (text.length() > 4000) {
                text = text.substring(0, 4000) + "...(이하 생략)";
            }
            return text;

        } catch (Exception e) {
            return "포트폴리오를 불러올 수 없습니다. URL을 확인해주세요.";
        }
    }

    public List<PortfolioEvaluation> getPortfolioHistory(String username) {
        return portfolioEvaluationRepository.findAllByUsername(username);
    }

    public void deletePortfolioHistory(Long id, String username) {
        PortfolioEvaluation eval = portfolioEvaluationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기록입니다."));
        if (!eval.getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 기록만 삭제할 수 있습니다.");
        }
        portfolioEvaluationRepository.delete(eval);
    }

    private String crawlPortfolio(String url) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            Thread.sleep(3000);

            String content = driver.findElement(By.tagName("body")).getText();

            if (content.trim().isEmpty() || content.length() < 50) {
                throw new IllegalArgumentException("노션 페이지 내용을 읽을 수 없습니다. '웹에서 공유'가 켜져 있는지 확인해주세요.");
            }

            return content;


        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("크롤링 중 대기 시간이 초과되었습니다.");
        } catch (Exception e) {
            System.err.println("크롤링 에러 발생: " + e.getMessage());
            throw new RuntimeException("URL에서 포트폴리오를 가져오는 데 실패했습니다.");
        } finally {
            driver.quit();
        }
    }
}