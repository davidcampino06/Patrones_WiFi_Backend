package com.wifisense.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SsidTextTest {

    @Test
    void namesWithAccentsSymbolsQuotesAndEmojiAreKeptAsTheyAre() {
        assertThat(SsidText.clean("Café ☕ \"Vecino\" <3 & Ñandú")).isEqualTo("Café ☕ \"Vecino\" <3 & Ñandú");
        assertThat(SsidText.clean("  espacios  ")).isEqualTo("  espacios  ");
    }

    @Test
    void controlCharactersAreRemoved() {
        assertThat(SsidText.clean("Red\u0000\u0007\nMala\t")).isEqualTo("RedMala");
    }

    @Test
    void anEmptyOrBlankNameIsAHiddenNetwork() {
        assertThat(SsidText.clean(null)).isNull();
        assertThat(SsidText.clean("")).isNull();
        assertThat(SsidText.clean("   ")).isNull();
        assertThat(SsidText.clean("\u0000\u0000")).isNull();
    }

    @Test
    void aLongNameIsCutAt64CharactersWithoutSplittingAnEmoji() {
        String name = "📶".repeat(70);
        String cut = SsidText.clean(name);

        assertThat(cut.codePointCount(0, cut.length())).isEqualTo(64);
        assertThat(cut).isEqualTo("📶".repeat(64));
    }

    @Test
    void systemLabelsAreTrimmedAndLimitedToTheirColumn() {
        assertThat(SsidText.cleanLabel("  WPA2-Personal  ", 40)).isEqualTo("WPA2-Personal");
        assertThat(SsidText.cleanLabel("x".repeat(30), 20)).hasSize(20);
        assertThat(SsidText.cleanLabel("   ", 20)).isNull();
    }
}
