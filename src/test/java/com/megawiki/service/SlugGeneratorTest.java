package com.megawiki.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugGeneratorTest {

    private final SlugGenerator slugGenerator = new SlugGenerator();

    @Test
    void keepsUnicodeLettersInSlug() {
        assertThat(slugGenerator.generate("\uBA85\uD568 \uC2E0\uCCAD\uD558\uAE30"))
                .isEqualTo("\uBA85\uD568-\uC2E0\uCCAD\uD558\uAE30");
    }
}