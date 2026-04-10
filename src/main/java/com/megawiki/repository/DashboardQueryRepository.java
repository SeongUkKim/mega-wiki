package com.megawiki.repository;

import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.QuestionThread;
import java.util.List;

public interface DashboardQueryRepository {

    DashboardQueryResult load(int recentLimit);

    record DashboardQueryResult(
            long pageCount,
            long questionCount,
            long contributionCount,
            List<KnowledgePage> recentPages,
            List<QuestionThread> recentQuestions
    ) {
    }
}