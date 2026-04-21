package org.example.speechhelper.Interview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
public class InterviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    private LocalDateTime createdAt;

    private String username;

    public InterviewHistory(String question, String answer, String feedback, String username) {
        this.question = question;
        this.answer = answer;
        this.feedback = feedback;
        this.username = username;
    }
}
