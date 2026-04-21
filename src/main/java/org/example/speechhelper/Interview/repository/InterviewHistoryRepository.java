package org.example.speechhelper.Interview.repository;

import org.example.speechhelper.Interview.entity.InterviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewHistoryRepository extends JpaRepository<InterviewHistory, Long> {
    List<InterviewHistory> findAllByUsername(String username);
}