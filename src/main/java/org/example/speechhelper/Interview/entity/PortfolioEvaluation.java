package org.example.speechhelper.Interview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class PortfolioEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String portfolioUrl;

    @Column(columnDefinition = "TEXT")
    private String feedback;  // AI 평가 결과

    private LocalDateTime createdAt;

    public PortfolioEvaluation(String username, String portfolioUrl, String feedback) {
        this.username = username;
        this.portfolioUrl = portfolioUrl;
        this.feedback = feedback;
        this.createdAt = LocalDateTime.now();
    }
}
