package com.veggiepal.blog.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaxWordsValidatorTest {

    static final MaxWordsValidator validator = new MaxWordsValidator(3);

    @Test
    void atTheLimit_isValid() {
        assertThat(validator.isValid("ngon quá đi", null)).isTrue();
    }

    @Test
    void overTheLimit_isInvalid() {
        assertThat(validator.isValid("ngon quá đi thôi", null)).isFalse();
    }

    // Words are whitespace-separated, so a run of spaces, a newline or a tab between two words
    // must not inflate the count. Counting separators instead of words would fail here.
    @Test
    void repeatedAndMixedWhitespace_countsWordsNotSeparators() {
        assertThat(validator.isValid("  ngon   quá\n\tđi  ", null)).isTrue();
    }

    // Blank and missing content are @NotBlank's concern, not this constraint's.
    @Test
    void nullAndBlank_areLeftToNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }
}
