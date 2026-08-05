package io.github.ultralan.lsposed.features.smspush

object SmsCodeExtractor {
    private val codePattern = Regex("""(?<![A-Za-z0-9])([A-Za-z0-9]{4,8})(?![A-Za-z0-9])""")
    private val verificationKeywordPattern = Regex(
        """验证码|校验码|动态码|认证码|安全码|确认码|短信码|一次性密码|OTP|verification\s*code""",
        RegexOption.IGNORE_CASE,
    )
    private const val MAX_KEYWORD_DISTANCE = 24

    fun extract(body: String): String? {
        val keywordRanges = verificationKeywordPattern.findAll(body).map { it.range }.toList()
        if (keywordRanges.isEmpty()) return null

        return codePattern.findAll(body)
            .firstOrNull { match ->
                val candidate = match.groupValues[1]
                val candidateRange = match.groups[1]?.range ?: match.range
                candidate.any { it.isDigit() } && keywordRanges.any { keywordRange ->
                    distance(candidateRange, keywordRange) <= MAX_KEYWORD_DISTANCE
                }
            }
            ?.groupValues
            ?.get(1)
    }

    private fun distance(first: IntRange, second: IntRange): Int =
        when {
            first.last < second.first -> second.first - first.last - 1
            second.last < first.first -> first.first - second.last - 1
            else -> 0
        }
}
