package com.megawiki.repository;

import com.megawiki.domain.QuestionThread;
import java.util.List;
import java.util.Optional;

public interface QuestionThreadRepository {

    List<QuestionThread> findAll();

    List<QuestionThread> findRecent(int limit);

    Optional<QuestionThread> findById(String id);

    QuestionThread save(QuestionThread thread);

    long count();
}
