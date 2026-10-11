package com.readrops.app.item.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleScrollPositionsTest {
    @Test
    fun scrollPositionSurvivesStoreRecreationAndIsClampedToZero() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val articleId = Int.MAX_VALUE
        val preferences = "article_scroll_positions"

        try {
            ArticleScrollPositions(context).save(articleId, 1240)
            assertEquals(1240, ArticleScrollPositions(context).get(articleId))

            ArticleScrollPositions(context).save(articleId, -20)
            assertEquals(0, ArticleScrollPositions(context).get(articleId))
        } finally {
            context.deleteSharedPreferences(preferences)
        }
    }
}
