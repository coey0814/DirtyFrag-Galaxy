package dirtyfrag.galaxy.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dirtyfrag.galaxy.core.RootViewModel
import dirtyfrag.galaxy.ui.components.AuroraFlow
import dirtyfrag.galaxy.ui.theme.AuroraBg2
import dirtyfrag.galaxy.ui.theme.Cyan
import dirtyfrag.galaxy.ui.theme.Dur
import dirtyfrag.galaxy.ui.theme.Purple
import dirtyfrag.galaxy.ui.theme.SpecterEase
import dirtyfrag.galaxy.ui.theme.Surface1
import dirtyfrag.galaxy.ui.theme.Teal
import dirtyfrag.galaxy.ui.theme.TextSecondary

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("홈", Icons.Filled.Home),
    LOG("로그", Icons.Filled.ReceiptLong),
    SETTINGS("설정", Icons.Filled.Settings)
}

@Composable
fun AppScaffold(vm: RootViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }

    // Re-detect whenever the app comes back to the foreground — e.g. after the user
    // installed modules or granted root in the KernelSU manager. Without this the UI
    // kept showing stale "미설치" until the process was recreated.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    // First-run guidance for the overlay permission (needed by boot auto-root).
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val overlayGranted = dirtyfrag.galaxy.ui.components.rememberOverlayGranted()
    val uiPrefs = ctx.getSharedPreferences("ui", android.content.Context.MODE_PRIVATE)
    var showOverlayDialog by rememberSaveable {
        mutableStateOf(!overlayGranted && !uiPrefs.getBoolean("overlay_prompt_done", false))
    }
    if (showOverlayDialog) {
        dirtyfrag.galaxy.ui.components.OverlayPermissionDialog(
            onAllow = {
                uiPrefs.edit().putBoolean("overlay_prompt_done", true).apply()
                showOverlayDialog = false
                dirtyfrag.galaxy.ui.components.openOverlaySettings(ctx)
            },
            onDismiss = {
                uiPrefs.edit().putBoolean("overlay_prompt_done", true).apply()
                showOverlayDialog = false
            }
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(dirtyfrag.galaxy.ui.theme.AuroraBg, AuroraBg2)))
    ) {
        // Full-screen aurora background (Home / Log / Settings all share it).
        AuroraFlow(Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Surface1.copy(alpha = 0.92f)) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Teal,
                                selectedTextColor = Teal,
                                indicatorColor = Purple.copy(alpha = 0.22f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        val dx = if (forward) 1 else -1
                        (fadeIn(tween(Dur.BASE, easing = SpecterEase)) +
                            slideInHorizontally(tween(Dur.BASE, easing = SpecterEase)) { dx * it / 8 })
                            .togetherWith(
                                fadeOut(tween(Dur.FAST)) +
                                    slideOutHorizontally(tween(Dur.BASE, easing = SpecterEase)) { -dx * it / 8 }
                            )
                    },
                    label = "tab"
                ) { t ->
                    when (t) {
                        Tab.HOME -> HomeScreen(vm, onOpenLog = { tab = Tab.LOG }, onRootStart = { tab = Tab.LOG })
                        Tab.LOG -> LogScreen(vm)
                        Tab.SETTINGS -> SettingsScreen(vm)
                    }
                }
            }
        }
    }
}
