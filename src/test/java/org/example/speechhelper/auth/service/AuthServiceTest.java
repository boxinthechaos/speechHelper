package org.example.speechhelper.auth.service;

import org.example.speechhelper.Interview.AI.service.AiInterviewService;
import org.example.speechhelper.Interview.entity.InterviewHistory;
import org.example.speechhelper.Interview.entity.InterviewQuestion;
import org.example.speechhelper.Interview.entity.PortfolioEvaluation;
import org.example.speechhelper.Interview.repository.InterviewHistoryRepository;
import org.example.speechhelper.Interview.repository.InterviewQuestionRepository;
import org.example.speechhelper.Interview.repository.PortfolioEvaluationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * AiInterviewService 단위 테스트
 *
 * ⚠️  Gemini API 실호출 및 Selenium 크롤링은 외부 의존성이므로
 *    실제 HTTP/브라우저 호출이 일어나는 메서드(createAndSaveFeedback, evaluatePortfolio)는
 *    여기서는 외부 콜을 Mock으로 대체하거나, 예외 경로만 검증합니다.
 *    통합 테스트가 필요한 경우 별도 @SpringBootTest 클래스를 작성하세요.
 */
@ExtendWith(MockitoExtension.class)
class AiInterviewServiceTest {

    @Mock InterviewHistoryRepository    historyRepository;
    @Mock InterviewQuestionRepository   questionRepository;
    @Mock PortfolioEvaluationRepository portfolioEvaluationRepository;

    @InjectMocks
    AiInterviewService aiInterviewService;

    // ──────────────────────────────────────────────
    // createAndSaveFeedback – 질문 미존재 경로
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("createAndSaveFeedback()")
    class CreateAndSaveFeedback {

        @Test
        @DisplayName("존재하지 않는 questionId면 예외를 던진다")
        void throwsWhenQuestionNotFound() {
            given(questionRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    aiInterviewService.createAndSaveFeedback(999L, "my answer", "user1")
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("질문을 찾을 수 없습니다.");
        }
    }

    // ──────────────────────────────────────────────
    // getMyHistory
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("getMyHistory()")
    class GetMyHistory {

        @Test
        @DisplayName("해당 유저의 히스토리 목록을 반환한다")
        void returnsHistoryForUser() {
            InterviewHistory h1 = new InterviewHistory("Q1", "A1", "F1", "user1");
            InterviewHistory h2 = new InterviewHistory("Q2", "A2", "F2", "user1");
            given(historyRepository.findAllByUsername("user1")).willReturn(List.of(h1, h2));

            List<InterviewHistory> result = aiInterviewService.getMyHistory("user1");

            assertThat(result).hasSize(2).containsExactly(h1, h2);
        }

        @Test
        @DisplayName("히스토리가 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoHistory() {
            given(historyRepository.findAllByUsername("nobody")).willReturn(List.of());

            assertThat(aiInterviewService.getMyHistory("nobody")).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // getAllQuestions
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("getAllQuestions()")
    class GetAllQuestions {

        @Test
        @DisplayName("저장된 모든 질문 목록을 반환한다")
        void returnsAllQuestions() {
            InterviewQuestion q1 = new InterviewQuestion();
            InterviewQuestion q2 = new InterviewQuestion();
            given(questionRepository.findAll()).willReturn(List.of(q1, q2));

            assertThat(aiInterviewService.getAllQuestions()).hasSize(2);
        }
    }

    // ──────────────────────────────────────────────
    // deleteHistory
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("deleteHistory()")
    class DeleteHistory {

        @Test
        @DisplayName("존재하지 않는 히스토리 ID면 예외를 던진다")
        void throwsWhenHistoryNotFound() {
            given(historyRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> aiInterviewService.deleteHistory(99L, "user1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 기록입니다.");
        }

        @Test
        @DisplayName("다른 유저의 기록을 삭제하려 하면 예외를 던진다")
        void throwsWhenNotOwner() {
            InterviewHistory history = new InterviewHistory("Q", "A", "F", "owner");
            given(historyRepository.findById(1L)).willReturn(Optional.of(history));

            assertThatThrownBy(() -> aiInterviewService.deleteHistory(1L, "other"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("본인의 기록만 삭제할 수 있습니다.");
        }

        @Test
        @DisplayName("본인의 기록은 정상적으로 삭제된다")
        void deletesOwnHistory() {
            InterviewHistory history = new InterviewHistory("Q", "A", "F", "user1");
            given(historyRepository.findById(1L)).willReturn(Optional.of(history));

            aiInterviewService.deleteHistory(1L, "user1");

            verify(historyRepository).delete(history);
        }
    }

    // ──────────────────────────────────────────────
    // getPortfolioHistory
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("getPortfolioHistory()")
    class GetPortfolioHistory {

        @Test
        @DisplayName("해당 유저의 포트폴리오 평가 목록을 반환한다")
        void returnsPortfolioHistoryForUser() {
            PortfolioEvaluation e1 = new PortfolioEvaluation("user1", "https://notion.so/1", "great");
            PortfolioEvaluation e2 = new PortfolioEvaluation("user1", "https://notion.so/2", "good");
            given(portfolioEvaluationRepository.findAllByUsername("user1")).willReturn(List.of(e1, e2));

            List<PortfolioEvaluation> result = aiInterviewService.getPortfolioHistory("user1");

            assertThat(result).hasSize(2).containsExactly(e1, e2);
        }
    }

    // ──────────────────────────────────────────────
    // deletePortfolioHistory
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("deletePortfolioHistory()")
    class DeletePortfolioHistory {

        @Test
        @DisplayName("존재하지 않는 포트폴리오 평가 ID면 예외를 던진다")
        void throwsWhenNotFound() {
            given(portfolioEvaluationRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> aiInterviewService.deletePortfolioHistory(99L, "user1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 기록입니다.");
        }

        @Test
        @DisplayName("다른 유저의 포트폴리오 평가를 삭제하려 하면 예외를 던진다")
        void throwsWhenNotOwner() {
            PortfolioEvaluation eval = new PortfolioEvaluation("owner", "https://url", "fb");
            given(portfolioEvaluationRepository.findById(1L)).willReturn(Optional.of(eval));

            assertThatThrownBy(() -> aiInterviewService.deletePortfolioHistory(1L, "hacker"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("본인의 기록만 삭제할 수 있습니다.");
        }

        @Test
        @DisplayName("본인의 포트폴리오 평가는 정상적으로 삭제된다")
        void deletesOwnEvaluation() {
            PortfolioEvaluation eval = new PortfolioEvaluation("user1", "https://url", "fb");
            given(portfolioEvaluationRepository.findById(1L)).willReturn(Optional.of(eval));

            aiInterviewService.deletePortfolioHistory(1L, "user1");

            verify(portfolioEvaluationRepository).delete(eval);
        }
    }
}