package com.example.bourget.data

/**
 * The confirm popup's title, written by the user once for every rule. "{이름}" becomes the app
 * name or the domain. Particles are not adjusted; users write "을(를)" themselves.
 * Pure Kotlin so it can be unit tested.
 */

const val NAME_PLACEHOLDER = "{이름}"
const val DEFAULT_TEMPLATE = "정말 {이름}을(를) 열고 싶으신가요?"
const val TEMPLATE_MAX_LENGTH = 60

val TEMPLATE_EXAMPLES = listOf(
    DEFAULT_TEMPLATE,
    "{이름}을(를) 여시겠습니까?",
    "{이름}을(를) 계속 사용하시겠습니까?",
    "{이름} 사용 확인",
)

enum class TemplateError { EMPTY, TOO_LONG, BAD_BRACES }

/** Replaces every "{이름}" in [template] with [name]. */
fun applyTemplate(template: String, name: String): String = template.replace(NAME_PLACEHOLDER, name)

/** Length as the user sees it (an emoji counts as one). */
fun templateLength(template: String): Int = template.codePointCount(0, template.length)

/** Why [template] cannot be saved, or null. A template without "{이름}" is allowed. */
fun templateError(template: String): TemplateError? {
    val withoutName = template.replace(NAME_PLACEHOLDER, "")
    return when {
        template.isBlank() -> TemplateError.EMPTY
        templateLength(template) > TEMPLATE_MAX_LENGTH -> TemplateError.TOO_LONG
        '{' in withoutName || '}' in withoutName -> TemplateError.BAD_BRACES
        else -> null
    }
}
