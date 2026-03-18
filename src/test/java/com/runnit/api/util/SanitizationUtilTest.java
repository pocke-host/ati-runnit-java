package com.runnit.api.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SanitizationUtilTest {

    @Test
    void sanitize_stripsHtmlTags() {
        assertEquals("hello world", SanitizationUtil.sanitize("<b>hello</b> world"));
    }

    @Test
    void sanitize_stripsScriptTags() {
        assertEquals("", SanitizationUtil.sanitize("<script>alert('xss')</script>"));
    }

    @Test
    void sanitize_stripsOnClickAttributes() {
        assertEquals("click me", SanitizationUtil.sanitize("<a onclick=\"evil()\">click me</a>"));
    }

    @Test
    void sanitize_trimsWhitespace() {
        assertEquals("hello", SanitizationUtil.sanitize("  hello  "));
    }

    @Test
    void sanitize_returnsNullForNull() {
        assertNull(SanitizationUtil.sanitize(null));
    }

    @Test
    void sanitize_preservesPlainText() {
        String text = "Great run today! 5k in 22:30";
        assertEquals(text, SanitizationUtil.sanitize(text));
    }

    @Test
    void sanitizeAndLimit_truncatesOverLimit() {
        String input = "a".repeat(200);
        String result = SanitizationUtil.sanitizeAndLimit(input, 100);
        assertEquals(100, result.length());
    }

    @Test
    void sanitizeAndLimit_doesNotTruncateUnderLimit() {
        String input = "short text";
        assertEquals(input, SanitizationUtil.sanitizeAndLimit(input, 100));
    }

    @Test
    void sanitizeAndLimit_returnsNullForNull() {
        assertNull(SanitizationUtil.sanitizeAndLimit(null, 100));
    }
}
