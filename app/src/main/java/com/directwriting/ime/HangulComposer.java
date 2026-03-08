package com.directwriting.ime;

/**
 * Korean composition engine contract for 2-set Hangul input.
 */
public interface HangulComposer {

    Result inputJamo(char jamo);

    Result backspace();

    String flushComposing();

    String getComposingText();

    final class Result {
        private final String committedText;
        private final String composingText;

        public Result(String committedText, String composingText) {
            this.committedText = committedText == null ? "" : committedText;
            this.composingText = composingText == null ? "" : composingText;
        }

        public String getCommittedText() {
            return committedText;
        }

        public String getComposingText() {
            return composingText;
        }
    }
}
