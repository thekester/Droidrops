package com.readrops.app.item.fullcontent

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FullArticleExtractorTest {

    private val paragraph =
        "The council voted on Tuesday, after a long debate, to extend the cycling network " +
            "across the old harbour district, a project first proposed more than ten years ago."

    private fun page(article: String) = """
        <html><head><title>Example</title><script>var tracking = 1;</script></head>
        <body>
          <header class="site-header"><a href="/">Home</a> <a href="/news">News</a></header>
          <nav><ul><li><a href="/a">A section with a long navigation label</a></li></ul></nav>
          $article
          <aside class="sidebar"><p>$paragraph Sidebar copy should not be kept in the result.</p></aside>
          <div class="comments"><p>$paragraph First reader comment that must be dropped.</p></div>
          <footer><p>Copyright notice and other footer text for the whole website.</p></footer>
        </body></html>
    """.trimIndent()

    @Test
    fun keepsTheArticleBodyAndDropsPageChrome() {
        val html = page(
            """
            <div class="post-content">
              <p>$paragraph</p>
              <p>$paragraph Second paragraph.</p>
              <figure><img src="/images/harbour.jpg" alt="Harbour"></figure>
              <p>$paragraph Third paragraph with <a href="/related">a link</a>.</p>
            </div>
            """
        )

        val result = FullArticleExtractor.extract(html, "https://news.example.com/2026/harbour")

        assertNotNull(result)
        val body = Jsoup.parseBodyFragment(result!!).body()
        assertTrue(body.text().contains("Second paragraph"))
        assertTrue(body.text().contains("Third paragraph"))
        assertFalse(body.text().contains("Sidebar copy"))
        assertFalse(body.text().contains("reader comment"))
        assertFalse(body.text().contains("Copyright"))
        assertFalse(result.contains("<script"))
        assertEquals("https://news.example.com/images/harbour.jpg", body.selectFirst("img")!!.attr("src"))
        assertEquals("https://news.example.com/related", body.selectFirst("a")!!.attr("href"))
    }

    @Test
    fun resolvesLazyLoadedImages() {
        val html = page(
            """
            <article>
              <p>$paragraph</p>
              <img src="data:image/gif;base64,R0lGOD" data-src="https://cdn.example.com/photo.jpg">
              <p>$paragraph Another paragraph.</p>
            </article>
            """
        )

        val result = FullArticleExtractor.extract(html, "https://news.example.com/")!!

        assertEquals(
            "https://cdn.example.com/photo.jpg",
            Jsoup.parseBodyFragment(result).selectFirst("img")!!.attr("src")
        )
    }

    @Test
    fun returnsNullWhenThePageHasNoArticleText() {
        val html = page("<div class=\"post-content\"><p>Too short.</p></div>")

        assertNull(FullArticleExtractor.extract(html, "https://news.example.com/"))
    }

    @Test
    fun dropsLinkListsInsideTheArticle() {
        val html = page(
            """
            <article>
              <p>$paragraph</p>
              <p>$paragraph More text.</p>
              <ul>
                <li><a href="/one">Read this other story about the harbour</a></li>
                <li><a href="/two">And this one about the new tram line</a></li>
              </ul>
            </article>
            """
        )

        val result = FullArticleExtractor.extract(html, "https://news.example.com/")!!

        assertFalse(result.contains("new tram line"))
        assertTrue(result.contains("More text"))
    }
}
