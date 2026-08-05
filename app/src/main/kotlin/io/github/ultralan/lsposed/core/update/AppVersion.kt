package io.github.ultralan.lsposed.core.update

object AppVersion {
    fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = parts(candidate)
        val currentParts = parts(current)
        val size = maxOf(candidateParts.size, currentParts.size)
        for (index in 0 until size) {
            val candidatePart = candidateParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (candidatePart != currentPart) return candidatePart > currentPart
        }
        return false
    }

    private fun parts(version: String): List<Int> =
        version.trim()
            .removePrefix("v")
            .split('.')
            .map { segment -> segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
}
