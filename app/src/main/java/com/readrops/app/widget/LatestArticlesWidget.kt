package com.readrops.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver.PendingResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.readrops.app.MainActivity
import com.readrops.app.R
import com.readrops.app.sync.SyncWorker
import com.readrops.db.Database
import com.readrops.db.pojo.WidgetArticle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class LatestArticlesWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context, manager, appWidgetIds, goAsync())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            refreshWidgets(context)
        }
    }

    private fun refresh(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        pendingResult: PendingResult? = null
    ) {
        if (ids.isEmpty()) {
            pendingResult?.finish()
            return
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val articles = runCatching {
                    GlobalContext.get().get<Database>().itemDao().selectLatestUnreadForWidget()
                }.getOrDefault(emptyList())
                ids.forEach { id -> manager.updateAppWidget(id, views(context, articles)) }
            } finally {
                pendingResult?.finish()
                scope.cancel()
            }
        }
    }

    private fun views(context: Context, articles: List<WidgetArticle>): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_latest_articles).apply {
            setOnClickPendingIntent(
                R.id.widget_refresh,
                PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_REFRESH),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val rowIds = listOf(R.id.widget_article_1, R.id.widget_article_2, R.id.widget_article_3)
            rowIds.forEachIndexed { index, viewId ->
                val article = articles.getOrNull(index)
                if (article == null) {
                    setTextViewText(viewId, if (index == 0) context.getString(R.string.widget_no_unread) else "")
                    setViewVisibility(viewId, if (index == 0) android.view.View.VISIBLE else android.view.View.GONE)
                    setOnClickPendingIntent(viewId, null)
                } else {
                    val title = article.title?.takeIf(String::isNotBlank)
                        ?: context.getString(R.string.widget_untitled)
                    setTextViewText(viewId, "$title\n${article.feedName.orEmpty()}")
                    setViewVisibility(viewId, android.view.View.VISIBLE)
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        // Start from the widget in a clean task so an already-open settings/dialog
                        // screen cannot swallow the request to open this specific article.
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra(SyncWorker.ACCOUNT_ID_KEY, article.accountId)
                        putExtra(SyncWorker.ITEM_ID_KEY, article.id)
                    }
                    setOnClickPendingIntent(
                        viewId,
                        PendingIntent.getActivity(
                            context,
                            article.id,
                            openIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }
        }

    companion object {
        const val ACTION_REFRESH = "com.droidrops.app.widget.REFRESH"

        fun refreshWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LatestArticlesWidget::class.java))
            LatestArticlesWidget().refresh(context, manager, ids)
        }
    }
}

class WidgetRefreshReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LatestArticlesWidget.refreshWidgets(context)
    }
}
