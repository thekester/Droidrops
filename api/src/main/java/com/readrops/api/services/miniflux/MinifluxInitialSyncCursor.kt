package com.readrops.api.services.miniflux

/** Stable keyset cursor for continuing a large first Miniflux import across sync runs. */
data class MinifluxInitialSyncCursor(
    val status: String,
    val beforeEntryId: Long?,
    val startedAt: Long
) {
    fun encode(): String = "$status|${beforeEntryId ?: ""}|$startedAt"

    companion object {
        fun decode(value: String?): MinifluxInitialSyncCursor? {
            if (value.isNullOrBlank()) return null
            val parts = value.split('|')
            if (parts.size != 3 || parts[0] !in setOf("unread", "read")) return null
            val beforeId = parts[1].takeIf(String::isNotBlank)?.toLongOrNull()
            val startedAt = parts[2].toLongOrNull() ?: return null
            if (parts[1].isNotBlank() && beforeId == null) return null
            return MinifluxInitialSyncCursor(parts[0], beforeId, startedAt)
        }
    }
}
