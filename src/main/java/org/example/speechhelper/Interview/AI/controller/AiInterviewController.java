package org.example.speechhelper.Interview.AI.controller;

import lombok.RequiredArgsConstructor;
import org.example.speechhelper.Interview.entity.InterviewHistory;
import org.example.speechhelper.Interview.entity.InterviewQuestion; // 💡 새 엔티티 import
import org.example.speechhelper.Interview.AI.service.AiInterviewService;
import org.example.speechhelper.Interview.entity.PortfolioEvaluation;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/v1/interview")
@RequiredArgsConstructor
public class AiInterviewController {

    private final AiInterviewService aiInterviewService;

    // 1. 💡 답변 제출 및 피드백 받기 (DB 저장)
    @PostMapping("/answer")
    @ResponseBody
    public ResponseEntity<InterviewHistory> submitAnswer(
            @RequestBody Map<String, String> request,
            Principal principal) { // 💡 현재 로그인한 사용자의 인증 정보를 가져옵니다!

        Long questionId = Long.parseLong(request.get("questionId"));
        String answer = request.get("answer");

        // 💡 로그인한 유저의 ID를 가져옵니다. (로그인이 안 되어있을 경우를 대비해 방어 로직 추가)
        String username = (principal != null) ? principal.getName() : "anonymous";

        // Service로 username도 같이 넘겨줍니다.
        InterviewHistory history = aiInterviewService.createAndSaveFeedback(questionId, answer, username);

        return ResponseEntity.ok(history);
    }

    // 2. 💡 (신규) DB에 저장된 '면접 질문 창고' 목록 불러오기 (프론트 드롭다운용)
    @GetMapping("/questions")
    @ResponseBody
    public ResponseEntity<List<InterviewQuestion>> getQuestions() {
        return ResponseEntity.ok(aiInterviewService.getAllQuestions());
    }

    // 3. 과거 내 면접 기록(피드백 결과) 불러오기
    @GetMapping("/history")
    @ResponseBody
    public ResponseEntity<List<InterviewHistory>> getHistory(Principal principal) {
        String email = principal.getName();
        List<InterviewHistory> historyList = aiInterviewService.getMyHistory(email);
        return ResponseEntity.ok(historyList);
    }

    // 4. 기존에 화면(HTML) 띄워주는 부분은 그대로 유지
    @GetMapping("/feedback")
    public String interviewPage() {
        return "interview";
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<String> deleteHistory(@PathVariable Long id, Principal principal) {
        String username = principal.getName();

        aiInterviewService.deleteHistory(id, username);

        return ResponseEntity.ok("삭제 완료");
    }

    @PostMapping("/portfolio/evaluate")
    public ResponseEntity<String> evaluatePortfolio(
            @RequestParam String portfolioUrl,
            Principal principal) {

        String username = (principal != null) ? principal.getName() : "anonymous";
        String result = aiInterviewService.evaluatePortfolio(portfolioUrl, username);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/portfolio/history")
    @ResponseBody
    public ResponseEntity<List<PortfolioEvaluation>> getPortfolioHistory(Principal principal) {
        String username = principal.getName();
        List<PortfolioEvaluation> list = aiInterviewService.getPortfolioHistory(username);
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/portfolio/history/{id}")
    @ResponseBody
    public ResponseEntity<String> deletePortfolioHistory(@PathVariable Long id, Principal principal) {
        aiInterviewService.deletePortfolioHistory(id, principal.getName());
        return ResponseEntity.ok("삭제 완료");
    }
}