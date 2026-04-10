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
        return "등록되어 있지 않은 질문입니다. 다른 키워드로 다시 질문해 주세요.";
    }

    static String failure() {
        return "응답 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
    }

    static String snowflakeReply(SnowflakeCortexResponse response) {
        return "*" + response.title() + "*\n\n" + response.answer();
    }

    static String geminiReply(KnowledgePage page, QuestionThread thread) {
        return "*" + page.getTitle() + "*\n"
                + thread.getAiAnswer() + "\n\n"
                + "Saved in Mega-Wiki with page id `" + page.getId() + "`.";
    }
}