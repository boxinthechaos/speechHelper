package org.example.speechhelper.Interview.repository;

import org.example.speechhelper.Interview.entity.PortfolioEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioEvaluationRepository extends JpaRepository<PortfolioEvaluation, Long> {
    List<PortfolioEvaluation> findAllByUsername(String username);
}
