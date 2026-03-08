package com.directwriting.ime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 2-set Hangul composer with syllable composition and stepwise backspace decomposition.
 */
public class HangulComposerImpl implements HangulComposer {

    private static final char IMPLICIT_INITIAL = 'ㅇ';
    private static final char NO_JONGSEONG = '\0';

    private static final List<Character> CHOSEONG_TABLE = Arrays.asList(
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    );

    private static final List<Character> JUNGSEONG_TABLE = Arrays.asList(
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
            'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
    );

    private static final List<Character> JONGSEONG_TABLE = Arrays.asList(
            NO_JONGSEONG, 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
            'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    );

    private static final Map<Character, Integer> CHOSEONG_INDEX = buildIndexMap(CHOSEONG_TABLE);
    private static final Map<Character, Integer> JUNGSEONG_INDEX = buildIndexMap(JUNGSEONG_TABLE);
    private static final Map<Character, Integer> JONGSEONG_INDEX = buildIndexMap(JONGSEONG_TABLE);

    private static final Set<Character> CONSONANTS = new HashSet<>(Arrays.asList(
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ',
            'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    ));

    private static final Set<Character> VOWELS = new HashSet<>(Arrays.asList(
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅛ', 'ㅜ',
            'ㅠ', 'ㅡ', 'ㅣ'
    ));

    private static final Map<String, Character> COMBINE_JUNGSEONG = new HashMap<>();
    private static final Map<Character, PairChars> SPLIT_JUNGSEONG = new HashMap<>();

    private static final Map<String, Character> COMBINE_JONGSEONG = new HashMap<>();
    private static final Map<Character, PairChars> SPLIT_JONGSEONG = new HashMap<>();

    static {
        registerJung('ㅗ', 'ㅏ', 'ㅘ');
        registerJung('ㅗ', 'ㅐ', 'ㅙ');
        registerJung('ㅗ', 'ㅣ', 'ㅚ');
        registerJung('ㅜ', 'ㅓ', 'ㅝ');
        registerJung('ㅜ', 'ㅔ', 'ㅞ');
        registerJung('ㅜ', 'ㅣ', 'ㅟ');
        registerJung('ㅡ', 'ㅣ', 'ㅢ');

        registerJong('ㄱ', 'ㅅ', 'ㄳ');
        registerJong('ㄴ', 'ㅈ', 'ㄵ');
        registerJong('ㄴ', 'ㅎ', 'ㄶ');
        registerJong('ㄹ', 'ㄱ', 'ㄺ');
        registerJong('ㄹ', 'ㅁ', 'ㄻ');
        registerJong('ㄹ', 'ㅂ', 'ㄼ');
        registerJong('ㄹ', 'ㅅ', 'ㄽ');
        registerJong('ㄹ', 'ㅌ', 'ㄾ');
        registerJong('ㄹ', 'ㅍ', 'ㄿ');
        registerJong('ㄹ', 'ㅎ', 'ㅀ');
        registerJong('ㅂ', 'ㅅ', 'ㅄ');
    }

    private Character choseong;
    private Character jungseong;
    private Character jongseong;

    @Override
    public Result inputJamo(char jamo) {
        if (isConsonant(jamo)) {
            return handleConsonant(jamo);
        }
        if (isVowel(jamo)) {
            return handleVowel(jamo);
        }

        String committed = flushComposing() + jamo;
        return new Result(committed, "");
    }

    @Override
    public Result backspace() {
        if (isEmpty()) {
            return new Result("", "");
        }

        if (jongseong != null) {
            PairChars split = SPLIT_JONGSEONG.get(jongseong);
            jongseong = split != null ? split.first : null;
            return result("", getComposingText());
        }

        if (jungseong != null) {
            PairChars split = SPLIT_JUNGSEONG.get(jungseong);
            jungseong = split != null ? split.first : null;
            return result("", getComposingText());
        }

        choseong = null;
        return result("", getComposingText());
    }

    @Override
    public String flushComposing() {
        String composing = getComposingText();
        clear();
        return composing;
    }

    @Override
    public String getComposingText() {
        if (isEmpty()) {
            return "";
        }

        if (jungseong == null) {
            return choseong == null ? "" : String.valueOf(choseong);
        }

        Character initial = choseong == null ? IMPLICIT_INITIAL : choseong;
        Integer l = CHOSEONG_INDEX.get(initial);
        Integer v = JUNGSEONG_INDEX.get(jungseong);
        if (l == null || v == null) {
            return fallbackText();
        }

        int t = 0;
        if (jongseong != null) {
            Integer jongIndex = JONGSEONG_INDEX.get(jongseong);
            if (jongIndex == null) {
                return fallbackText();
            }
            t = jongIndex;
        }

        char syllable = (char) (0xAC00 + ((l * 21 + v) * 28) + t);
        return String.valueOf(syllable);
    }

    private Result handleConsonant(char consonant) {
        if (choseong == null && jungseong == null) {
            choseong = consonant;
            return result("", getComposingText());
        }

        if (jungseong == null) {
            String committed = String.valueOf(choseong);
            choseong = consonant;
            return result(committed, getComposingText());
        }

        if (jongseong == null) {
            if (canBeJongseong(consonant)) {
                jongseong = consonant;
                return result("", getComposingText());
            }

            String committed = getComposingText();
            choseong = consonant;
            jungseong = null;
            jongseong = null;
            return result(committed, getComposingText());
        }

        Character combined = COMBINE_JONGSEONG.get(key(jongseong, consonant));
        if (combined != null) {
            jongseong = combined;
            return result("", getComposingText());
        }

        String committed = getComposingText();
        choseong = consonant;
        jungseong = null;
        jongseong = null;
        return result(committed, getComposingText());
    }

    private Result handleVowel(char vowel) {
        if (choseong == null && jungseong == null) {
            choseong = IMPLICIT_INITIAL;
            jungseong = vowel;
            return result("", getComposingText());
        }

        if (jungseong == null) {
            jungseong = vowel;
            return result("", getComposingText());
        }

        if (jongseong == null) {
            Character combined = COMBINE_JUNGSEONG.get(key(jungseong, vowel));
            if (combined != null) {
                jungseong = combined;
                return result("", getComposingText());
            }

            String committed = getComposingText();
            choseong = IMPLICIT_INITIAL;
            jungseong = vowel;
            jongseong = null;
            return result(committed, getComposingText());
        }

        PairChars split = SPLIT_JONGSEONG.get(jongseong);
        String committed;
        if (split != null) {
            jongseong = split.first;
            committed = getComposingText();
            choseong = split.second;
        } else {
            char movedInitial = jongseong;
            jongseong = null;
            committed = getComposingText();
            choseong = movedInitial;
        }

        jungseong = vowel;
        jongseong = null;
        return result(committed, getComposingText());
    }

    private boolean isConsonant(char jamo) {
        return CONSONANTS.contains(jamo);
    }

    private boolean isVowel(char jamo) {
        return VOWELS.contains(jamo);
    }

    private boolean canBeJongseong(char consonant) {
        Integer index = JONGSEONG_INDEX.get(consonant);
        return index != null && index > 0;
    }

    private boolean isEmpty() {
        return choseong == null && jungseong == null && jongseong == null;
    }

    private void clear() {
        choseong = null;
        jungseong = null;
        jongseong = null;
    }

    private String fallbackText() {
        StringBuilder builder = new StringBuilder();
        if (choseong != null) {
            builder.append(choseong);
        }
        if (jungseong != null) {
            builder.append(jungseong);
        }
        if (jongseong != null) {
            builder.append(jongseong);
        }
        return builder.toString();
    }

    private static Result result(String committed, String composing) {
        return new Result(committed, composing);
    }

    private static Map<Character, Integer> buildIndexMap(List<Character> table) {
        Map<Character, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < table.size(); i++) {
            Character c = table.get(i);
            if (c != NO_JONGSEONG) {
                indexMap.put(c, i);
            }
        }
        return indexMap;
    }

    private static void registerJung(char first, char second, char combined) {
        COMBINE_JUNGSEONG.put(key(first, second), combined);
        SPLIT_JUNGSEONG.put(combined, new PairChars(first, second));
    }

    private static void registerJong(char first, char second, char combined) {
        COMBINE_JONGSEONG.put(key(first, second), combined);
        SPLIT_JONGSEONG.put(combined, new PairChars(first, second));
    }

    private static String key(char first, char second) {
        return new String(new char[] { first, second });
    }

    private static final class PairChars {
        private final char first;
        private final char second;

        private PairChars(char first, char second) {
            this.first = first;
            this.second = second;
        }
    }
}
