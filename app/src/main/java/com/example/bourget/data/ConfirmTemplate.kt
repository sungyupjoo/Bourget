package com.example.bourget.data

/**
 * The confirm popup's title, written by the user once for every rule. The name placeholder
 * ("{이름}", or "{name}" in English) becomes the app name or the domain. Both placeholders work in
 * either language, so a saved text keeps working after the language changes. Korean particles
 * are not adjusted, so the Korean defaults set the name off with a comma
 * ("YouTube, 정말 열고 싶으신가요?"), which reads naturally after any name.
 * The default text and examples are string resources (template_default, template_examples), so
 * they follow the app language until the user saves a text of their own.
 * Pure Kotlin so it can be unit tested.
 */

val NAME_PLACEHOLDERS = listOf("{이름}", "{name}")
const val TEMPLATE_MAX_LENGTH = 60

enum class TemplateError { EMPTY, TOO_LONG, BAD_BRACES }

/** Replaces every name placeholder in [template] with [name]. */
fun applyTemplate(template: String, name: String): String =
    NAME_PLACEHOLDERS.fold(template) { text, placeholder -> text.replace(placeholder, name) }

/** Whether [template] contains a name placeholder. */
fun hasNamePlaceholder(template: String): Boolean = NAME_PLACEHOLDERS.any { it in template }

/** Length as the user sees it (an emoji counts as one). */
fun templateLength(template: String): Int = template.codePointCount(0, template.length)

/** Why [template] cannot be saved, or null. A template without a name placeholder is allowed. */
fun templateError(template: String): TemplateError? {
    val withoutName = NAME_PLACEHOLDERS.fold(template) { text, placeholder -> text.replace(placeholder, "") }
    return when {
        template.isBlank() -> TemplateError.EMPTY
        templateLength(template) > TEMPLATE_MAX_LENGTH -> TemplateError.TOO_LONG
        '{' in withoutName || '}' in withoutName -> TemplateError.BAD_BRACES
        else -> null
    }
}
