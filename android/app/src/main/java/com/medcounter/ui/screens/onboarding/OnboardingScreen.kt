package com.medcounter.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medcounter.data.PreferencesManager
import kotlinx.coroutines.launch

private data class OnboardingPage(val emoji: String, val title: String, val body: String)

private val pages = listOf(
    OnboardingPage(
        emoji = "💊",
        title = "Know exactly when\nyou'll run out",
        body = "Enter your medications and dosing schedule. PillStock calculates the precise date you'll need a refill — automatically.",
    ),
    OnboardingPage(
        emoji = "🔔",
        title = "Get reminded before\nyou run low",
        body = "Set your lead time and PillStock alerts you when it's time to call the pharmacy — before you run out.",
    ),
)

@Composable
fun OnboardingScreen(preferences: PreferencesManager, onFinished: () -> Unit) {
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        preferences.hasCompletedOnboarding = true
        onFinished()
    }
    fun finish() {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            preferences.hasCompletedOnboarding = true
            onFinished()
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { finish() }) { Text("Skip") }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val item = pages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(item.emoji, style = MaterialTheme.typography.displayMedium)
                Text(item.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(item.body, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { index ->
                Text(if (index == pager.currentPage) "●" else "○", modifier = Modifier.padding(4.dp))
            }
        }
        Button(
            onClick = {
                if (pager.currentPage < pages.lastIndex) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                } else {
                    finish()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (pager.currentPage < pages.lastIndex) "Next" else "Get Started")
        }
    }
}
