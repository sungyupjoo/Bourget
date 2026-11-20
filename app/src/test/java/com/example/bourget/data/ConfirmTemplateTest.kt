package com.example.bourget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfirmTemplateTest {

    @Test
    fun replacesEveryPlaceholder() {
        assertEquals("YouTube, 정말 열고 싶으신가요?", applyTemplate(DEFAULT_TEMPLATE, "YouTube"))
        assertEquals("namu.wiki? namu.wiki!", applyTemplate("{이름}? {이름}!", "namu.wiki"))
    }

    @Test
    fun templateWithoutPlaceholderOrEmptyIsUnchanged() {
        assertEquals("잠시 멈추기", applyTemplate("잠시 멈추기", "YouTube"))
        assertEquals("", applyTemplate("", "YouTube"))
    }

    @Test
    fun validTemplates() {
        TEMPLATE_EXAMPLES.forEach { assertNull(it, templateError(it)) }
        assertNull(templateError("이름 없이도 괜찮습니다"))
        assertNull(templateError("가".repeat(TEMPLATE_MAX_LENGTH)))
    }

    @Test
    fun emptyOrBlankIsAnError() {
        assertEquals(TemplateError.EMPTY, templateError(""))
        assertEquals(TemplateError.EMPTY, templateError("  \n"))
    }

    @Test
    fun longerThanSixtyIsAnError() {
        assertEquals(TemplateError.TOO_LONG, templateError("가".repeat(TEMPLATE_MAX_LENGTH + 1)))
        // Counted as the user sees it: one emoji is one character.
        assertNull(templateError("😀".repeat(TEMPLATE_MAX_LENGTH)))
    }

    @Test
    fun bracesOtherThanThePlaceholderAreAnError() {
        assertEquals(TemplateError.BAD_BRACES, templateError("{앱}, 정말 열고 싶으신가요?"))
        assertEquals(TemplateError.BAD_BRACES, templateError("{이름} }"))
        assertEquals(TemplateError.BAD_BRACES, templateError("{이름"))
    }
}
