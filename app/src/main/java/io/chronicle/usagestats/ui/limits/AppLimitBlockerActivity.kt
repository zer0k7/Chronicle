package io.chronicle.usagestats.ui.limits

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.AndroidEntryPoint
import io.chronicle.usagestats.R
import io.chronicle.usagestats.domain.model.LimitBypassMode
import io.chronicle.usagestats.domain.repository.AppLimitRepository
import io.chronicle.usagestats.service.AppLimitMonitorService
import io.chronicle.usagestats.ui.theme.ChronicleTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BlockerScreenState {
    MAIN,
    TYPING_CHALLENGE,
    MATH_CHALLENGE
}

@AndroidEntryPoint
class AppLimitBlockerActivity : ComponentActivity() {

    @Inject
    lateinit var appLimitRepository: AppLimitRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val blockedPackage = intent.getStringExtra(AppLimitMonitorService.EXTRA_BLOCKED_PACKAGE) ?: ""
        val blockedLabel = intent.getStringExtra(AppLimitMonitorService.EXTRA_BLOCKED_LABEL) ?: blockedPackage
        val limitMinutes = intent.getIntExtra(AppLimitMonitorService.EXTRA_LIMIT_MINUTES, 0)
        val usedMinutes = intent.getIntExtra(AppLimitMonitorService.EXTRA_USED_MINUTES, 0)
        val bypassModeStr = intent.getStringExtra(AppLimitMonitorService.EXTRA_BYPASS_MODE) ?: LimitBypassMode.NO_BYPASS.name

        val bypassMode = try {
            LimitBypassMode.valueOf(bypassModeStr)
        } catch (_: IllegalArgumentException) {
            LimitBypassMode.NO_BYPASS
        }

        setContent {
            ChronicleTheme {
                val scope = rememberCoroutineScope()
                var screenState by remember { mutableStateOf(BlockerScreenState.MAIN) }

                BackHandler {
                    navigateToHome()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = screenState,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "blocker_screen_transition"
                    ) { state ->
                        when (state) {
                            BlockerScreenState.MAIN -> {
                                BlockerMainContent(
                                    packageName = blockedPackage,
                                    appLabel = blockedLabel,
                                    limitMinutes = limitMinutes,
                                    usedMinutes = usedMinutes,
                                    bypassMode = bypassMode,
                                    onGoHome = { navigateToHome() },
                                    onRequestEmergencyTime = {
                                        when (bypassMode) {
                                            LimitBypassMode.TYPING_CHALLENGE -> screenState = BlockerScreenState.TYPING_CHALLENGE
                                            LimitBypassMode.MATH_CHALLENGE -> screenState = BlockerScreenState.MATH_CHALLENGE
                                            LimitBypassMode.NO_BYPASS -> {}
                                        }
                                    }
                                )
                            }
                            BlockerScreenState.TYPING_CHALLENGE -> {
                                TypingChallengeScreen(
                                    onChallengeSuccess = {
                                        scope.launch {
                                            val unlockUntil = System.currentTimeMillis() + (5 * 60 * 1000L)
                                            appLimitRepository.setTemporaryUnlock(blockedPackage, unlockUntil)
                                            finish()
                                        }
                                    },
                                    onGiveUp = { navigateToHome() },
                                    modifier = Modifier
                                        .windowInsetsPadding(WindowInsets.statusBars)
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                )
                            }
                            BlockerScreenState.MATH_CHALLENGE -> {
                                MathChallengeScreen(
                                    onChallengeSuccess = {
                                        scope.launch {
                                            val unlockUntil = System.currentTimeMillis() + (5 * 60 * 1000L)
                                            appLimitRepository.setTemporaryUnlock(blockedPackage, unlockUntil)
                                            finish()
                                        }
                                    },
                                    onGiveUp = { navigateToHome() },
                                    modifier = Modifier
                                        .windowInsetsPadding(WindowInsets.statusBars)
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navigateToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
private fun BlockerMainContent(
    packageName: String,
    appLabel: String,
    limitMinutes: Int,
    usedMinutes: Int,
    bypassMode: LimitBypassMode,
    onGoHome: () -> Unit,
    onRequestEmergencyTime: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(128, 128).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun formatMins(totalMins: Int): String {
        val hours = totalMins / 60
        val mins = totalMins % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Icon with lock badge
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.size(80.dp)
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = appLabel,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HourglassBottom,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.blocker_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.blocker_subtitle, appLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stats breakdown card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Time Used",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatMins(usedMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Daily Limit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatMins(limitMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (bypassMode == LimitBypassMode.NO_BYPASS) {
                    stringResource(R.string.blocker_locked_message)
                } else {
                    stringResource(R.string.blocker_reset_notice)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.blocker_button_home),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (bypassMode != LimitBypassMode.NO_BYPASS) {
                OutlinedButton(
                    onClick = onRequestEmergencyTime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.blocker_button_request_time),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
