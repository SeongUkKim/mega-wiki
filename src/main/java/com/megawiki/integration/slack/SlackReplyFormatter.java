package com.megawiki.integration.slack;

import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.QuestionThread;
import com.megawiki.integration.snowflake.SnowflakeCortexResponse;

final class SlackReplyFormatter {

    private SlackReplyFormatter() {
    }

    static String questionRequired() {
        return "질문을 입력해 주세요. 멘션 뒤에 궁금한 내용을 적어 주세요.";
    }

    static String knowledgeNotFound() {
        return "등록된 지식을 찾지 못했습니다. 다른 키워드로 다시 질문해 주세요.";
    }

    static String failure() {
        return "답변 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
    }

    static String snowflakeReply(SnowflakeCortexResponse response) {
        return "*" + response.title() + "*\n\n" + response.answer();
    }

    static String geminiReply(KnowledgePage page, QuestionThread thread, boolean reusedExistingPage) {
        String actionLine = reusedExistingPage
                ? "Found in Mega-Wiki with page id `" + page.getId() + "`."
                : "Saved in Mega-Wiki with page id `" + page.getId() + "`.";
        return "*" + page.getTitle() + "*\n"
                + thread.getAiAnswer() + "\n\n"
                + actionLine;
    }
}