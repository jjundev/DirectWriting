package com.directwriting.ime;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HangulComposerImplTest {

    private HangulComposer composer;

    @Before
    public void setUp() {
        composer = new HangulComposerImpl();
    }

    @Test
    public void composeBasicSyllable_giyeokA_equalsGa() {
        composer.inputJamo('ㄱ');
        HangulComposer.Result result = composer.inputJamo('ㅏ');

        assertEquals("", result.getCommittedText());
        assertEquals("가", result.getComposingText());
        assertEquals("가", composer.flushComposing());
    }

    @Test
    public void composeCompoundVowel_giyeokOa_equalsGwa() {
        composer.inputJamo('ㄱ');
        composer.inputJamo('ㅗ');
        HangulComposer.Result result = composer.inputJamo('ㅏ');

        assertEquals("", result.getCommittedText());
        assertEquals("과", result.getComposingText());
    }

    @Test
    public void composeFinalConsonant_giyeokAgiyeok_equalsGak() {
        composer.inputJamo('ㄱ');
        composer.inputJamo('ㅏ');
        HangulComposer.Result result = composer.inputJamo('ㄱ');

        assertEquals("", result.getCommittedText());
        assertEquals("각", result.getComposingText());
    }

    @Test
    public void splitFinalOnVowel_inputGakA_commitsGaAndComposesGa() {
        composer.inputJamo('ㄱ');
        composer.inputJamo('ㅏ');
        composer.inputJamo('ㄱ');

        HangulComposer.Result result = composer.inputJamo('ㅏ');

        assertEquals("가", result.getCommittedText());
        assertEquals("가", result.getComposingText());
    }

    @Test
    public void backspaceDecomposesStepByStep() {
        composer.inputJamo('ㄱ');
        composer.inputJamo('ㅗ');
        composer.inputJamo('ㅏ');
        composer.inputJamo('ㄱ');

        HangulComposer.Result first = composer.backspace();
        HangulComposer.Result second = composer.backspace();
        HangulComposer.Result third = composer.backspace();
        HangulComposer.Result fourth = composer.backspace();

        assertEquals("과", first.getComposingText());
        assertEquals("고", second.getComposingText());
        assertEquals("ㄱ", third.getComposingText());
        assertEquals("", fourth.getComposingText());
    }

    @Test
    public void splitCompoundFinalOnVowel_inputGaksA_commitsGakAndComposesSa() {
        composer.inputJamo('ㄱ');
        composer.inputJamo('ㅏ');
        composer.inputJamo('ㄱ');
        composer.inputJamo('ㅅ');

        HangulComposer.Result result = composer.inputJamo('ㅏ');

        assertEquals("각", result.getCommittedText());
        assertEquals("사", result.getComposingText());
    }
}
