package com.purovida.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.purovida.app.LocalApp
import com.purovida.app.ui.PuroColors
import com.purovida.core.ChatMessage
import com.purovida.core.generateAdvisorResponse
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val quickPrompts = listOf(
    "Recommend something for tonight",
    "Best cigars for bourbon",
    "What should I age?",
    "Best value cigars",
    "Mild and creamy options",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(navController: NavHostController) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    // Newest first, matching the original's reversed list.
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = "welcome",
                role = "assistant",
                content = "Welcome to your AI Cigar Advisor. I know your taste profile and can recommend cigars, suggest pairings, or advise on aging. What can I help you with?",
                timestamp = Instant.now().toString(),
            ),
        )
    }

    fun send(text: String) {
        if (text.trim().isEmpty() || isTyping) return
        val entries = app.journalStore.entries.value
        val profile = app.userStore.profile.value

        messages.add(
            0,
            ChatMessage(
                id = "${System.currentTimeMillis()}",
                role = "user",
                content = text.trim(),
                timestamp = Instant.now().toString(),
            ),
        )
        input = ""
        isTyping = true

        scope.launch {
            // Simulated typing delay (800–1400 ms, matching the original).
            delay(800L + Random.nextLong(600))
            val response = generateAdvisorResponse(text, entries, profile)
            messages.add(
                0,
                ChatMessage(
                    id = "${System.currentTimeMillis() + 1}",
                    role = "assistant",
                    content = response,
                    timestamp = Instant.now().toString(),
                ),
            )
            isTyping = false
        }
    }

    Scaffold(
        containerColor = PuroColors.background,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PuroColors.background,
                    titleContentColor = PuroColors.text,
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PuroColors.text,
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Memory,
                            contentDescription = null,
                            tint = PuroColors.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "Cigar Advisor",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                },
                actions = {
                    Box(
                        Modifier
                            .padding(end = 20.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PuroColors.success),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            LazyColumn(
                Modifier.weight(1f),
                reverseLayout = true,
                contentPadding = PaddingValues(16.dp),
            ) {
                if (isTyping) {
                    item {
                        MessageBubble(isUser = false) {
                            Text(
                                "Thinking...",
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                color = PuroColors.mutedForeground,
                            )
                        }
                    }
                }
                items(messages.size) { i ->
                    val msg = messages[i]
                    val isUser = msg.role == "user"
                    MessageBubble(isUser = isUser) {
                        Text(
                            boldMarkdown(msg.content),
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = if (isUser) PuroColors.primaryForeground else PuroColors.text,
                        )
                    }
                }
            }
            if (messages.size <= 2) {
                LazyRow(
                    Modifier.height(44.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(quickPrompts.size) { i ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PuroColors.card)
                                .border(1.dp, PuroColors.border, RoundedCornerShape(20.dp))
                                .clickable { send(quickPrompts[i]) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(quickPrompts[i], fontSize = 13.sp, color = PuroColors.text)
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(PuroColors.tabBar)
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            "Ask about cigars...",
                            fontSize = 14.sp,
                            color = PuroColors.mutedForeground,
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PuroColors.card,
                        unfocusedContainerColor = PuroColors.card,
                        focusedBorderColor = PuroColors.primary,
                        unfocusedBorderColor = PuroColors.border,
                        focusedTextColor = PuroColors.text,
                        unfocusedTextColor = PuroColors.text,
                        cursorColor = PuroColors.primary,
                    ),
                    modifier = Modifier.weight(1f),
                )
                val hasText = input.trim().isNotEmpty()
                Box(
                    Modifier
                        .padding(start = 10.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (hasText) PuroColors.primary else PuroColors.muted)
                        .clickable(enabled = hasText && !isTyping) { send(input) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (hasText) {
                            PuroColors.primaryForeground
                        } else {
                            PuroColors.mutedForeground
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(isUser: Boolean, content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUser) {
            Box(
                Modifier
                    .padding(end = 10.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(PuroColors.primary.copy(alpha = 0.13f))
                    .border(1.dp, PuroColors.primary.copy(alpha = 0.27f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Memory,
                    contentDescription = null,
                    tint = PuroColors.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isUser) 16.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp,
                    ),
                )
                .background(if (isUser) PuroColors.primary else PuroColors.card)
                .let { m ->
                    if (isUser) {
                        m
                    } else {
                        m.border(
                            1.dp,
                            PuroColors.border,
                            RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp,
                            ),
                        )
                    }
                }
                .padding(12.dp),
        ) {
            content()
        }
    }
}

/** Renders the advisor's lightweight markdown (**bold** spans only). */
private fun boldMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val pattern = Regex("\\*\\*(.+?)\\*\\*")
    var last = 0
    for (m in pattern.findAll(text)) {
        if (m.range.first > last) {
            append(text.substring(last, m.range.first))
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(m.groupValues[1])
        }
        last = m.range.last + 1
    }
    if (last < text.length) {
        append(text.substring(last))
    }
}
