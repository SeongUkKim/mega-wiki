package com.megawiki.service;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SlugGenerator {

    public String generate(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^0-9a-z]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "knowledge-page" : normalized;
    }
}