package com.crypttvpn.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.crypttvpn.data.remote.AccountDto
import com.crypttvpn.data.remote.AccountAuditDto
import com.crypttvpn.data.remote.AccountPaymentDto
import com.crypttvpn.data.remote.AppStatusDto
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ───────── design tokens (mirror of web globals.css) ───────── */
private val Bg = Color(0xFF060606)
private val SurfaceA = Color(0x0Affffff)   // white/4
private val SurfaceB = Color(0x05ffffff)   // white/2
private val DividerC = Color(0x1Affffff)   // white/10
private val Primary = Color(0xFFE63946)
private val PrimaryHover = Color(0xFFD62838)
private val Success = Color(0xFF06D6A0)
private val Warn = Color(0xFFFACC15)
private val TextSecondary = Color(0xB3ffffff) // white/70
private val TextDim = Color(0x66ffffff)       // white/40
private val Glow = Color(0x40E63946)         // primary 25%

/**
 * Native LK screen — 1:1 visual port of the web /dashboard route.
 * No webview, no redirects. Reads from /v1/app/account using the device JWT.
 */
@Composable
fun AccountScreen(
    vm: AccountViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        // Atmospheric red glow blobs
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-120).dp)
                .clip(CircleShape)
                .background(Glow),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val s = state) {
                AccountUiState.Loading -> Loading()
                AccountUiState.NotActivated -> NotActivated()
                is AccountUiState.Error -> ErrorView(s.message, vm::refresh)
                is AccountUiState.Success -> Content(s.data, s.banner)
            }
        }
    }
}

/* ───────── main content ───────── */

@Composable
private fun ColumnScope.Content(d: AccountDto, banner: AppStatusDto?) {
    // Header
    Column {
        MonoLabel("— Личный кабинет")
        Spacer(Modifier.height(12.dp))
        val name = d.user?.displayName
            ?: d.user?.email?.substringBefore('@')
            ?: "друг"
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Привет, ", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(name, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(".", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
        d.user?.email?.let {
            Text(it, color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 6.dp))
        }
    }
    Spacer(Modifier.height(20.dp))

    // Banner
    banner?.let { BannerCard(it) }

    // Hero subscription card
    HeroSubscriptionCard(d)

    // Quick actions row
    QuickActions(d)

    // Activation key
    Block(label = "КЛЮЧ АКТИВАЦИИ", icon = Icons.Filled.VpnKey) {
        KeyRow(d.key.value, d.key.status)
        Spacer(Modifier.height(12.dp))
        Text(
            text = when (d.key.status) {
                "unused" -> "— используйте этот ключ в приложении один раз"
                "activated" -> "— уже активирован на вашем устройстве"
                "revoked" -> "— ключ отозван, обратитесь в поддержку"
                else -> ""
            },
            color = TextDim,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }

    // Active device
    d.subscription.device?.takeIf { it.name.isNotBlank() && it.name != "Unknown" }?.let { dev ->
        Block(label = "АКТИВНОЕ УСТРОЙСТВО", icon = Icons.Filled.PhoneAndroid) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceA)
                        .border(1.dp, DividerC, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PhoneAndroid, null, tint = TextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(dev.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        dev.os ?: "OS не определена",
                        color = TextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                MonoTag("ПОДКЛЮЧЕНО", Success, pulse = true)
            }
        }
    }

    // Payments
    if (d.payments.isNotEmpty()) {
        var open by remember { mutableStateOf(false) }
        Collapsible("ИСТОРИЯ ПЛАТЕЖЕЙ", Icons.Filled.CalendarMonth, d.payments.size, open, { open = !open }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                d.payments.take(10).forEach { PaymentRow(it) }
            }
        }
    }

    // Audit log
    if (d.auditLog.isNotEmpty()) {
        var open by remember { mutableStateOf(false) }
        Collapsible("АКТИВНОСТЬ", Icons.Filled.AccessTime, d.auditLog.size, open, { open = !open }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                d.auditLog.take(20).forEach { AuditRow(it) }
            }
        }
    }

    // Help footer
    HelpFooter()
}

/* ───────── pieces ───────── */

@Composable
private fun HeroSubscriptionCard(d: AccountDto) {
    val s = d.subscription
    val active = s.isActive

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    if (active) listOf(Color(0x26E63946), SurfaceA, SurfaceB)
                    else listOf(SurfaceA, SurfaceB),
                ),
            )
            .border(1.dp, if (active) Color(0x40E63946) else DividerC, RoundedCornerShape(24.dp))
            .padding(24.dp),
    ) {
        if (active) {
            // Status pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoTag("● АКТИВНА", Success, pulse = true)
                Spacer(Modifier.width(8.dp))
                MonoLabel("· ${planName(s.plan)}")
            }
            Spacer(Modifier.height(12.dp))
            // Big day count
            Text(
                s.daysLeft.toString(),
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 80.sp,
            )
            Text(
                "${dayWord(s.daysLeft)} осталось",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarMonth, null, tint = TextDim, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("до ", color = TextSecondary.copy(alpha = 0.7f), fontSize = 13.sp)
                Text(formatDate(s.expiresAt), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }

            // Progress bar
            Spacer(Modifier.height(20.dp))
            val total = planDays(s.plan).toFloat()
            val pct = (s.daysLeft / total).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(pct)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryHover))),
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("сегодня", color = TextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("конец", color = TextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        } else if (d.key.status == "unused") {
            MonoTag("● ОЖИДАЕТ АКТИВАЦИИ", Warn)
            Spacer(Modifier.height(12.dp))
            Text("Ключ готов", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Активируйте его в приложении — ключ привяжется к этому устройству, и здесь появится дата окончания.",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        } else {
            Text("Подписки нет", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Купите подписку — ключ придёт моментально.",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun QuickActions(d: AccountDto) {
    val clipboard = LocalClipboardManager.current
    val uri = LocalUriHandler.current
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Copy key
        ActionTile(
            modifier = Modifier.weight(1f),
            icon = if (copied) Icons.Filled.Done else Icons.Filled.ContentCopy,
            iconTint = if (copied) Success else Primary,
            title = if (copied) "Скопировано!" else "Скопировать ключ",
            subtitle = d.key.value.take(14) + "…",
            onClick = {
                clipboard.setText(AnnotatedString(d.key.value))
                copied = true
            },
        )
        // Support
        ActionTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Forum,
            iconTint = Primary,
            title = "Поддержка",
            subtitle = "TELEGRAM",
            onClick = { uri.openUri("https://t.me/crypttvpn_support") },
        )
    }
}

@Composable
private fun ActionTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceB)
            .border(1.dp, DividerC, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.1f))
                .border(1.dp, iconTint.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp)
            Text(subtitle, color = TextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ColumnScope.Block(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)) {
            Icon(icon, null, tint = TextDim, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text("— $label", color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceA)
                .border(1.dp, DividerC, RoundedCornerShape(24.dp))
                .padding(20.dp),
            content = content,
        )
    }
}

@Composable
private fun Collapsible(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceB)
                .border(1.dp, DividerC, RoundedCornerShape(24.dp))
                .clickable { onToggle() }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = TextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(count.toString(), color = TextSecondary.copy(alpha = 0.7f), fontSize = 10.sp)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                null,
                tint = TextDim,
                modifier = Modifier.size(16.dp),
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceA)
                    .border(1.dp, DividerC, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun BannerCard(b: AppStatusDto) {
    val color = when (b.severity) {
        "error", "maintenance" -> Primary
        "warn" -> Warn
        else -> TextSecondary
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        b.title?.let { Text(it, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
        b.message?.let { Text(it, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun KeyRow(key: String, status: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = key,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f),
        )
        StatusTag(status)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceA)
                .border(1.dp, DividerC, CircleShape)
                .clickable {
                    clipboard.setText(AnnotatedString(key))
                    copied = true
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (copied) Icons.Filled.Done else Icons.Filled.ContentCopy,
                null,
                tint = if (copied) Success else TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun PaymentRow(p: AccountPaymentDto) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text("${p.amountRub} ₽", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(formatDate(p.createdAt), color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
        }
        when (p.status) {
            "paid" -> MonoTag("ОПЛАЧЕНО", Success)
            "pending" -> MonoTag("ОЖИДАНИЕ", Warn)
            "failed", "expired" -> MonoTag(p.status.uppercase(Locale.ROOT), Primary)
            else -> MonoTag(p.status.uppercase(Locale.ROOT), TextSecondary)
        }
    }
}

@Composable
private fun AuditRow(a: AccountAuditDto) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.6f)),
        )
        Column(Modifier.weight(1f)) {
            Text(a.label, color = Color.White, fontSize = 13.sp)
            a.subjectId?.let {
                Text(
                    "${a.subjectType ?: ""}: $it",
                    color = TextDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(timeAgo(a.createdAt), color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
    }
}

@Composable
private fun HelpFooter() {
    val uri = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceB)
            .border(1.dp, DividerC, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Primary.copy(alpha = 0.1f))
                .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Help, null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Нужна помощь?", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Telegram-поддержка отвечает в течение часа.", color = TextSecondary.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, DividerC, RoundedCornerShape(999.dp))
                .clickable { uri.openUri("https://t.me/crypttvpn_support") }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Открыть", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/* ───────── small atoms ───────── */

@Composable
private fun MonoLabel(text: String) {
    Text(text, color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
}

@Composable
private fun MonoTag(text: String, color: Color, pulse: Boolean = false) {
    val alpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = if (pulse) 0.6f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text,
            color = if (pulse) color.copy(alpha = alpha) else color,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun StatusTag(status: String) {
    val (color, label) = when (status) {
        "unused" -> Success to "ГОТОВ"
        "activated" -> TextSecondary.copy(alpha = 0.5f) to "АКТИВИРОВАН"
        "revoked" -> Primary to "ОТОЗВАН"
        else -> TextSecondary.copy(alpha = 0.5f) to status.uppercase(Locale.ROOT)
    }
    MonoTag(label, color)
}

/* ───────── states ───────── */

@Composable
private fun Loading() {
    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Primary)
    }
}

@Composable
private fun NotActivated() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 48.dp)) {
        MonoLabel("— Доступ")
        Text("Нужен ключ", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(
            "Введите ключ активации, чтобы попасть в кабинет.",
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 48.dp)) {
        MonoLabel("— Ошибка")
        Text("Не удалось загрузить", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(message, color = TextSecondary.copy(alpha = 0.7f), fontSize = 13.sp)
        Box(
            Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White)
                .clickable { onRetry() }
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text("Повторить", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

/* ───────── helpers ───────── */

private fun planName(plan: String): String = when (plan) {
    "monthly" -> "1 месяц"
    "yearly" -> "1 год"
    "babushka" -> "Lifetime"
    else -> plan
}

private fun planDays(plan: String): Int = when (plan) {
    "yearly" -> 365
    "babushka" -> 36500
    else -> 30
}

private fun dayWord(n: Int): String {
    val last = n % 10
    val lastTwo = n % 100
    return when {
        lastTwo in 11..14 -> "дней"
        last == 1 -> "день"
        last in 2..4 -> "дня"
        else -> "дней"
    }
}

private fun formatDate(iso: String): String = try {
    val instant = Instant.parse(iso).atZone(ZoneId.systemDefault())
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")).format(instant)
} catch (_: Exception) { iso }

private fun timeAgo(iso: String): String = try {
    val d = Duration.between(Instant.parse(iso), Instant.now())
    when {
        d.toMinutes() < 1 -> "только что"
        d.toMinutes() < 60 -> "${d.toMinutes()} мин назад"
        d.toHours() < 24 -> "${d.toHours()} ч назад"
        d.toDays() < 7 -> "${d.toDays()} дн назад"
        else -> formatDate(iso)
    }
} catch (_: Exception) { "" }
