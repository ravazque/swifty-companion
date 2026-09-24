package com.ravazque.swiftycompanion.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartSkillsTest {
    private fun cursus(slug: String, vararg skills: Skill) = Cursus(1, slug, slug, 1.0, null, skills.toList())

    @Test
    fun mainCursusShowsEveryAxisAlphabetically() {
        val chart = cursus(Cursus.MAIN_SLUG, Skill("Unix", 9.0), Skill("Aardvark studies", 2.0)).chartSkills

        assertEquals(Cursus.MAIN_SKILLS.size + 1, chart.size)
        assertEquals(chart.map { it.name }.sorted(), chart.map { it.name })
        assertEquals("Aardvark studies", chart.first().name)
        assertEquals(9.0, chart.first { it.name == "Unix" }.level, 0.0)
        assertEquals(0.0, chart.first { it.name == "Web" }.level, 0.0)
    }

    @Test
    fun otherCursusOnlyShowsItsOwnSkills() {
        val chart = cursus("c-piscine", Skill("Unix", 5.0), Skill("Rigor", 3.0)).chartSkills

        assertEquals(listOf("Rigor", "Unix"), chart.map { it.name })
        assertEquals(emptyList<Skill>(), cursus("c-piscine").chartSkills)
    }

    @Test
    fun ratioIsClampedToTheChartScale() {
        assertEquals(0.5, Skill("Unix", 10.5).ratio, 1e-9)
        assertEquals(1.0, Skill("Unix", 30.0).ratio, 0.0)
        assertEquals(0.0, Skill("Unix", -1.0).ratio, 0.0)
    }
}
