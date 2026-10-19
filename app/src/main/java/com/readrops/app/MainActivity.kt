package com.readrops.app

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.transitions.SlideTransition
import com.readrops.app.account.selection.AccountSelectionScreen
import com.readrops.app.account.selection.AccountSelectionScreenModel
import com.readrops.app.home.HomeScreen
import com.readrops.app.item.ReaderSession
import com.readrops.app.repositories.BaseRepository
import com.readrops.app.sync.SyncWorker
import com.readrops.app.timelime.TimelineTab
import com.readrops.app.util.KeyboardShortcuts
import com.readrops.app.util.Preferences
import com.readrops.app.util.theme.ReadropsTheme
import com.readrops.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity(), KoinComponent {

    var ready = false

    @OptIn(ExperimentalVoyagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Disable waiting for timeline tab list to be populated before removing splash screen
        //splashScreen.setKeepOnScreenCondition { !ready }

        val screenModel = get<AccountSelectionScreenModel>()
        val accountExists = screenModel.accountExists()

        val preferences = get<Preferences>()

        val darkFlag = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val initialThemeMode = runBlocking {
            preferences.theme.flow.first()
        }

        setContent {
            KoinAndroidContext {
                val themeMode by preferences.theme.flow.collectAsState(initial = initialThemeMode)
                val useDarkTheme = useDarkTheme(themeMode, darkFlag)

                ReadropsTheme(
                    useDarkTheme = useDarkTheme,
                    useDynamicColor = themeMode == DYNAMIC_THEME,
                    useTrueBlack = themeMode == TRUE_BLACK_THEME
                ) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            lightScrim = Color.TRANSPARENT,
                            darkScrim = Color.TRANSPARENT,
                            detectDarkMode = { useDarkTheme }
                        ),
                        navigationBarStyle = SystemBarStyle.light(
                            scrim = BottomAppBarDefaults.containerColor.toArgb(),
                            darkScrim = BottomAppBarDefaults.containerColor.toArgb()
                        )
                    )

                    Navigator(
                        screen = if (accountExists) HomeScreen else AccountSelectionScreen(),
                        disposeBehavior = NavigatorDisposeBehavior(
                            // prevent screenModels being recreated when opening a screen from a tab
                            disposeNestedNavigators = false,
                            disposeSteps = true
                        )
                    ) { navigator ->
                        LaunchedEffect(Unit) {
                            val hasNavigationIntent = intent.hasExtra(SyncWorker.ACCOUNT_ID_KEY) ||
                                    intent.hasExtra(SyncWorker.ITEM_ID_KEY) ||
                                    intent.action == Intent.ACTION_SEND
                            if (hasNavigationIntent) {
                                ReaderSession.clear(this@MainActivity)
                                handleIntent(intent)
                            } else if (navigator.lastItem == HomeScreen) {
                                ReaderSession.take(this@MainActivity)?.let { itemId ->
                                    HomeScreen.openItem(itemId)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                // custom safe drawing to be able to draw behind the status bar
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(
                                        WindowInsetsSides.Start + WindowInsetsSides.End
                                    )
                                )
                        ) {
                            SlideTransition(
                                navigator = navigator,
                                modifier = Modifier.imePadding(),
                                disposeScreenAfterTransitionEnd = true
                            )
                        }
                    }
                }
            }
        }
    }

    // hardware keyboard shortcuts only see the keys no view or text field consumed
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (super.dispatchKeyEvent(event)) return true
        if (!KeyboardShortcuts.isShortcutPress(event)) return false

        // A typed key leaves touch mode, and Android then focuses the first focusable control,
        // the drawer's close button, which draws a focus ring. Focus cannot simply be dropped
        // outside touch mode (Android hands it back to that same control), so it is moved to
        // an invisible anchor, whether or not a screen could use the shortcut. The shortcut
        // may also have replaced the reader, as when the side pane switches article: wait for
        // the layout that shows the new one.
        focusAnchorUntil = SystemClock.uptimeMillis() + FOCUS_ANCHOR_WINDOW_MS
        if (!focusAnchorListenerAdded) {
            focusAnchorListenerAdded = true
            window.decorView.viewTreeObserver.addOnGlobalLayoutListener { focusAnchorIfPending() }
        }
        window.decorView.post { focusAnchorIfPending() }

        return KeyboardShortcuts.dispatch(event)
    }

    private var focusAnchorUntil = 0L
    private var focusAnchorListenerAdded = false

    private fun focusAnchorIfPending() {
        if (SystemClock.uptimeMillis() > focusAnchorUntil) return
        if (KeyboardShortcuts.focusAnchor()) focusAnchorUntil = 0L
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        lifecycleScope.launch {
            handleIntent(intent)
        }
    }

    private suspend fun handleIntent(intent: Intent) = withContext(Dispatchers.IO) {
        when {
            intent.hasExtra(SyncWorker.ACCOUNT_ID_KEY) -> {
                val accountId = intent.getIntExtra(SyncWorker.ACCOUNT_ID_KEY, -1)
                val database = get<Database>().also {
                    it.accountDao()
                        .updateCurrentAccount(accountId)
                }

                HomeScreen.openTab(TimelineTab)

                if (intent.hasExtra(SyncWorker.ITEM_ID_KEY)) {
                    val itemId = intent.getIntExtra(SyncWorker.ITEM_ID_KEY, -1)
                    val account = database.accountDao().select(accountId)
                    val item = database.itemDao().select(itemId)
                        .apply { isRead = true }

                    get<BaseRepository>(parameters = { parametersOf(account) })
                        .setItemReadState(item)
                    HomeScreen.openItem(itemId)
                }
            }

            intent.action != null && intent.action == Intent.ACTION_SEND -> {
                HomeScreen.openAddFeedDialog(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty())
            }
        }
    }

    private fun useDarkTheme(mode: String, darkFlag: Int): Boolean {
        return when (mode) {
            "light" -> false
            "dark" -> true
            TRUE_BLACK_THEME -> true
            else -> darkFlag == Configuration.UI_MODE_NIGHT_YES
        }
    }

    private companion object {
        const val DYNAMIC_THEME = "dynamic"
        const val TRUE_BLACK_THEME = "true_black"

        // long enough for the side pane to show the article a shortcut switched to
        const val FOCUS_ANCHOR_WINDOW_MS = 2_000L
    }
}
