package com.directwriting.ime;

import android.content.res.Configuration;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImeWindowModeResolverTest {

    @Test
    public void shouldDisableExtractUi_sw599_returnsFalse() {
        assertFalse(ImeWindowModeResolver.shouldDisableExtractUi(599));
    }

    @Test
    public void shouldUseSplitLayout_sw600Portrait_returnsFalse() {
        assertFalse(
                ImeWindowModeResolver.shouldUseSplitLayout(
                        600,
                        Configuration.ORIENTATION_PORTRAIT
                )
        );
    }

    @Test
    public void shouldUseSplitLayout_sw600Landscape_returnsTrue() {
        assertTrue(
                ImeWindowModeResolver.shouldUseSplitLayout(
                        600,
                        Configuration.ORIENTATION_LANDSCAPE
                )
        );
        assertTrue(ImeWindowModeResolver.shouldDisableExtractUi(600));
    }
}
