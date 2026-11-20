package com.example.bourget.data

/**
 * The confirm popup's title, written by the user once for every rule. "{이름}" becomes the app
 * name or the domain. Particles are not adjusted, so the defaults set the name off with a comma
 * ("YouTube, 또 볼 거니?"), which reads naturally after any name.
 * Pure Kotlin so it can be unit tested.
 */

const val NAME_PLACEHOLDER = "{이름}"
const val DEFAULT_TEMPLATE = "{이름}, 또 볼 거니?"
const val TEMPLATE_MAX_LENGTH = 60

val TEMPLATE_EXAMPLES = listOf(
    DEFAULT_TEMPLATE,
    "{이름}, 정말 열고 싶으신가요?",
    "{이름}, 지금 꼭 필요한가요?",
    "{이름}, 계속 사용하시겠습니까?",
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
