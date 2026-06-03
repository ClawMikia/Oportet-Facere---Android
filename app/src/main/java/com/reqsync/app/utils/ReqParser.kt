package com.reqsync.app.utils

import com.reqsync.app.data.database.entities.RequirementCategory
import com.reqsync.app.data.database.entities.RequirementItem

/**
 * ReqParser — Smart text parser for employment requirement lists.
 *
 * Detects:
 *  - Section headers (PEME, NBI Clearance, etc.)
 *  - Bullet items (•, *, -, numbers)
 *  - Standalone requirements
 *  - Nested sub-items
 *  - Optional items (in parentheses with "if applicable")
 *
 * Returns a list of [ParsedSection] ready to be saved to the database.
 */
object ReqParser {

    data class ParsedSession(
        val sessionId: Long = System.currentTimeMillis(),
        val sections: List<ParsedSection>
    )

    data class ParsedSection(
        val title: String,
        val items: List<ParsedItem>,
        val sortOrder: Int = 0
    )

    data class ParsedItem(
        val title: String,
        val isOptional: Boolean = false,
        val sortOrder: Int = 0
    )

    // Patterns
    private val bulletPattern = Regex("""^[\s]*[•\*\-–—>\d+\.\)]+\s+(.+)$""")
    private val numberedPattern = Regex("""^[\s]*\d+[\.\)]\s+(.+)$""")
    private val optionalPattern = Regex("""(?i)\(if applicable\)|\(optional\)|\(if married\)""")

    // Lines that likely indicate a section header:
    // - All caps or Title Case
    // - Ends with ':'
    // - Known keyword patterns
    // - Followed by bullet points
    private val sectionKeywords = setOf(
        "medical", "peme", "nbi", "bir", "psa", "tor", "requirements",
        "documents", "clearance", "records", "certificates", "ids",
        "examination", "government", "employment", "pre-employment",
        "onboarding", "hr", "academic", "financial", "legal"
    )

    fun parse(rawText: String): ParsedSession {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return ParsedSession(sections = emptyList())

        val sections = mutableListOf<ParsedSection>()
        var currentTitle = ""
        var currentItems = mutableListOf<ParsedItem>()
        var sortOrder = 0
        var itemOrder = 0

        fun flushSection() {
            if (currentTitle.isNotBlank() || currentItems.isNotEmpty()) {
                val title = currentTitle.ifBlank { inferSectionTitle(currentItems) }
                sections.add(ParsedSection(title, currentItems.toList(), sortOrder++))
                currentItems = mutableListOf()
                currentTitle = ""
                itemOrder = 0
            }
        }

        for (line in lines) {
            when {
                // Explicit header: ends with colon or all-caps short phrase
                isHeaderLine(line, lines) -> {
                    flushSection()
                    currentTitle = cleanHeader(line)
                }
                // Bullet / numbered item
                isBulletLine(line) -> {
                    val itemText = extractBulletText(line)
                    val optional = optionalPattern.containsMatchIn(itemText)
                    val cleanText = itemText.replace(optionalPattern, "").trim()
                    currentItems.add(ParsedItem(cleanText, optional, itemOrder++))
                }
                // Standalone short line (likely a requirement without bullets)
                isStandaloneLine(line) -> {
                    val optional = optionalPattern.containsMatchIn(line)
                    val cleanText = line.replace(optionalPattern, "").trim()
                    // If no current section, start a new implied "Other Requirements" section
                    if (currentTitle.isBlank() && currentItems.isEmpty()) {
                        currentTitle = "Other Requirements"
                    }
                    currentItems.add(ParsedItem(cleanText, optional, itemOrder++))
                }
            }
        }

        flushSection()

        return ParsedSession(sections = postProcessSections(sections))
    }

    private fun postProcessSections(sections: List<ParsedSection>): List<ParsedSection> {
        val multiItemSections = mutableListOf<ParsedSection>()
        val singleItems = mutableListOf<ParsedItem>()

        sections.forEach { section ->
            // A section with 0 items is treated as 1 item (the title itself)
            // A section with 1 item is also treated as a single item
            if (section.items.isEmpty()) {
                singleItems.add(ParsedItem(section.title))
            } else if (section.items.size == 1) {
                // Combine section title and item title if they are different and descriptive
                val title = if (section.title != "Other Requirements" && section.title != "Requirements") {
                    "${section.title}: ${section.items[0].title}"
                } else {
                    section.items[0].title
                }
                singleItems.add(ParsedItem(title, section.items[0].isOptional))
            } else {
                multiItemSections.add(section)
            }
        }

        if (singleItems.isNotEmpty()) {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            val timestamp = sdf.format(java.util.Date())
            val othersSection = ParsedSection(
                title = "Others - $timestamp",
                items = singleItems.mapIndexed { index, item -> item.copy(sortOrder = index) },
                sortOrder = multiItemSections.size
            )
            multiItemSections.add(othersSection)
        }

        return multiItemSections
    }

    private fun isHeaderLine(line: String, allLines: List<String>): Boolean {
        if (line.endsWith(":")) return true
        val lower = line.lowercase()
        if (sectionKeywords.any { lower.contains(it) } && line.length < 80 && !isBulletLine(line)) return true
        if (line == line.uppercase() && line.length in 3..60 && !isBulletLine(line)) return true
        return false
    }

    private fun isBulletLine(line: String): Boolean =
        bulletPattern.matches(line) || numberedPattern.matches(line)

    private fun isStandaloneLine(line: String): Boolean =
        line.length < 80 && !isBulletLine(line) && !isHeaderLine(line, emptyList())

    private fun extractBulletText(line: String): String {
        bulletPattern.find(line)?.let { return it.groupValues[1].trim() }
        numberedPattern.find(line)?.let { return it.groupValues[1].trim() }
        return line.trim()
    }

    private fun cleanHeader(line: String): String =
        line.trimEnd(':').trim()

    private fun inferSectionTitle(items: List<ParsedItem>): String {
        if (items.isEmpty()) return "Requirements"
        // Try to infer from item contents
        val allText = items.joinToString(" ") { it.title }.lowercase()
        return when {
            allText.contains("medical") || allText.contains("health") -> "Medical Requirements"
            allText.contains("clearance") -> "Government Clearances"
            allText.contains("tax") || allText.contains("bir") -> "Financial Documents"
            allText.contains("school") || allText.contains("transcript") -> "Academic Records"
            else -> "Other Requirements"
        }
    }

    /**
     * Convert ParsedSession into Room entities ready for DB insertion.
     */
    fun toEntities(session: ParsedSession): Pair<List<RequirementCategory>, Map<Int, List<RequirementItem>>> {
        val categories = session.sections.mapIndexed { index, section ->
            RequirementCategory(
                title = section.title,
                sortOrder = section.sortOrder,
                sessionId = session.sessionId,
                colorTag = getCategoryColor(index)
            )
        }
        val itemsByIndex = session.sections.mapIndexed { index, section ->
            index to section.items.map { parsedItem ->
                RequirementItem(
                    categoryId = 0L, // will be set after category insert
                    title = parsedItem.title,
                    isOptional = parsedItem.isOptional,
                    sortOrder = parsedItem.sortOrder,
                    xpReward = if (parsedItem.isOptional) 25 else 50
                )
            }
        }.toMap()
        return Pair(categories, itemsByIndex)
    }

    private fun getCategoryColor(index: Int): String {
        val colors = listOf(
            "#00F5FF", // neon cyan
            "#BF00FF", // purple glow
            "#0080FF", // electric blue
            "#00FF9F", // matrix green
            "#FF6B35", // amber warning
            "#FF00AA", // hot pink
            "#FFD700"  // gold
        )
        return colors[index % colors.size]
    }
}
