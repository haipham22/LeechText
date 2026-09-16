package dev.haipham22.leechtext.plugin.api

/**
 * Kết quả validate plugin (port từ api/ValidationResult.java) — status, errors, warnings.
 */
data class ValidationResult(
    val valid: Boolean = true,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    fun hasErrors(): Boolean = errors.isNotEmpty()

    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    companion object {
        @JvmStatic fun success(): ValidationResult = ValidationResult(valid = true)

        @JvmStatic
        fun success(warnings: List<String>): ValidationResult = ValidationResult(valid = true, warnings = warnings)

        @JvmStatic
        fun failure(error: String): ValidationResult = ValidationResult(valid = false, errors = listOf(error))

        @JvmStatic
        fun failure(errors: List<String>): ValidationResult = ValidationResult(valid = false, errors = errors)
    }
}
