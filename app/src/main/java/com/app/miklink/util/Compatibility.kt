package com.app.miklink.util

object Compatibility {
    private val tdrCompatibleBoards = setOf(
        "RB450G", "RB850Gx2", "RB3011", "RB4011", 
        "CCR1009", "CCR1016", "CCR1036", "CCR1072", "CRS3xx"
    )

    fun isTdrSupported(boardName: String?): Boolean {
        if (boardName == null) return false
        return tdrCompatibleBoards.any { boardName.startsWith(it, ignoreCase = true) }
    }
}