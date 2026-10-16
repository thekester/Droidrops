package com.readrops.app.item.view

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageHoverTextTest {

    @Test
    fun titleTakesPrecedenceOverAltText() {
        val body = Jsoup.parseBodyFragment("<img src='comic.png' title='Title text' alt='Alt text'>").body()

        body.revealImageHoverText()

        assertEquals("Title text", body.selectFirst(".hover-text")?.text())
    }

    @Test
    fun altTextIsShownWhenTitleIsMissing() {
        val body = Jsoup.parseBodyFragment("<img src='comic.png' alt='Alt only'>").body()

        body.revealImageHoverText()

        assertEquals("Alt only", body.selectFirst(".hover-text")?.text())
    }

    @Test
    fun imageHoverTextIsEscapedAndPlacedOutsideItsLink() {
        val body = Jsoup.parseBodyFragment("<a href='comic'><img src='comic.png' title='<script>x</script>'></a>").body()

        body.revealImageHoverText()

        val caption = body.selectFirst(".hover-text")!!
        assertEquals("<script>x</script>", caption.text())
        assertTrue(caption.select("script").isEmpty())
        assertTrue(caption.previousElementSibling()?.tagName() == "a")
    }
}
