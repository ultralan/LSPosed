package io.github.ultralan.lsposed.features.smspush

object SmsTitleExtractor {
    private const val MAX_SUMMARY_LENGTH = 30
    private val bracketedBrandPattern = Regex("""^\s*[【\[]([^】\]]{1,20})[】\]]""")
    private val greetingPattern = Regex("""^(?:尊敬的(?:用户|客户|会员)[，,、\s]*)+""")
    private val sentenceEndPattern = Regex("""[。！？!?；;\r\n]""")

    fun extract(sender: String, body: String): String {
        val normalizedBody = body.replace(Regex("""\s+"""), " ").trim()
        val brandMatch = bracketedBrandPattern.find(normalizedBody)
        val source = brandMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() } ?: sender
        val content = normalizedBody
            .removePrefix(brandMatch?.value.orEmpty())
            .replace(greetingPattern, "")
            .trimStart('，', ',', '、', ' ')
            .trim()
        val summary = sentenceEndPattern.find(content)?.let { content.substring(0, it.range.first) } ?: content

        return summary.takeIf { it.isNotEmpty() }
            ?.let { "$source：${truncate(it)}" }
            ?: "短信：$source"
    }

    private fun truncate(value: String): String =
        if (value.length <= MAX_SUMMARY_LENGTH) value else "${value.take(MAX_SUMMARY_LENGTH)}…"
}
