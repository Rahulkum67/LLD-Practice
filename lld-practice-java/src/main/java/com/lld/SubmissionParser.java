package com.lld;

/** One implementation per submission format. Add a format = add a parser. */
public interface SubmissionParser {
    String format();
    DesignModel parse(String content);
}
