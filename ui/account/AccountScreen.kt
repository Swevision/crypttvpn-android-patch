package com.crypttvpn.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.crypttvpn.data.remote.AccountDto
import com.crypttvpn.data.remote.AccountAuditDto
import com.crypttvpn.data.remote.AccountPaymentDto
import com.crypttvpn.data.remote.AccountSubscriptionDto
import com.crypttvpn.data.remote.AppStatusDto
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Bg = Color(0xFF0A0A0A)
private val Surface = Color(0xFF141414)
private val SurfaceAlpha = Color(0x40141414)
private val Divider = Color(0xFF222222)
private val DividerSoft = Color(0x66222222)
private val Primary = Color(0xFFE63946)
private val Success = Color(0xFF06D6A0)
private val Warn = Color(0xFFFACC15)
private val TextSecondary = Color(0xFF8B8B8B)

@Composable
fun AccountScreen(
    vm: AccountViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
    ) {
        when (val s = state) {
            AccountUiState.Loading -> Loading()
            AccountUiState.NotActivated -> NotActivated()
            is AccountUiState.Error -> ErrorView(s.message, onRetry = vm::refresh)
            is AccountUiState.Success -> Content(s.data, s.banner)
        }
    }
}

/* ---------------- main content ---------------- */

@Composable
private fun Content(d: AccountDto, banner: AppStatusDto?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Column {
            val name = d.user?.displayName
                ?: d.user?.email?.substringBefore('@')
                ?: "Кабинет"
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            )
            d.user?.email?.let {
                Text(it, color = TextSecondary, fontSize = 13.sp)
            }
        }

        // Banner
        banner?.let { BannerCard(it) }

        // Paid until
        Block(label = "ОПЛАЧЕНО ДО") {
            if (d.subscription.isActive) {
                val date = formatDate(d.subscription.expiresAt)
                Text(date, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Осталось ${d.subscription.daysLeft} дн.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else if (d.key.status == "unused") {
                Text("Ключ готов", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Активируйте его в приложении — после этого здесь появится дата окончания.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text("Подписки нет", color = TextSecondary, fontSize = 14.sp)
            }
        }

        // Active device
        d.subscription.device?.let { device ->
            if (device.name.isNotBlank() && device.name != "Unknown") {
                Block(label = "АКТИВНОЕ УСТРОЙСТВО") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Surface)
                                .border(1.dp, Divider, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.PhoneAndroid, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            device.os?.let { Text(it, color = TextSecondary, fontSize = 12.sp) }
                        }
                        Badge("ПОДКЛЮЧЕНО", Success)
                    }
                }
            }
        }

        // Activation key
        Block(label = "КЛЮЧ АКТИВАЦИИ") {
            KeyRow(d.key.value, d.key.status)
        }

        // Payments
        if (d.payments.isNotEmpty()) {
            Block(label = "ИСТОРИЯ ПЛАТЕЖЕЙ") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    d.payments.take(10).forEach { PaymentRow(it) }
                }
            }
        }

        // Audit
        if (d.auditLog.isNotEmpty()) {
            Block(label = "АКТИВНОСТЬ") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    d.auditLog.take(20).forEach { AuditRow(it) }
                }
            }
        }
    }
}

/* ---------------- pieces ---------------- */

@Composable
private fun Block(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = "// $label",
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceAlpha)
                .border(1.dp, Divider, RoundedCornerShape(16.dp))
                .padding(20.dp),
            content = content,
        )
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
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        b.title?.let {
            Text(it, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        b.message?.let {
            Text(it, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
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
            modifier = Modifier.weight(1f),
        )
        Badge(
            text = when (status) {
                "unused" -> "ГОТОВ"
                "activated" -> "АКТИВИРОВАН"
                "revoked" -> "ОТОЗВАН"
                else -> status.uppercase(Locale.ROOT)
            },
            color = when (status) {
                "unused" -> Success; "activated" -> TextSecondary
                "revoked" -> Primary; else -> TextSecondary
            },
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    clipboard.setText(AnnotatedString(key))
                    copied = true
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (copied) Icons.Filled.Done else Icons.Filled.ContentCopy,
                contentDescription = "Копировать",
                tint = if (copied) Success else TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PaymentRow(p: AccountPaymentDto) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${p.amountRub} ₽", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(formatDate(p.createdAt), color = TextSecondary, fontSize = 11.sp)
        }
        when (p.status) {
            "paid" -> Badge("ОПЛАЧЕНО", Success)
            "pending" -> Badge("ОЖИДАНИЕ", Warn)
            "failed", "expired" -> Badge(p.status.uppercase(Locale.ROOT), Primary)
            else -> Badge(p.status.uppercase(Locale.ROOT), TextSecondary)
        }
    }
}

@Composable
private fun AuditRow(a: AccountAuditDto) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.6f)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(a.label, color = Color.White, fontSize = 13.sp)
            a.subjectId?.let {
                Text(
                    "${a.subjectType ?: ""}: $it",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
        }
        Text(timeAgo(a.createdAt), color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

/* ---------------- states ---------------- */

@Composable
private fun Loading() {
    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Primary)
    }
}

@Composable
private fun NotActivated() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Подписка не активирована", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Введите ключ активации, чтобы попасть в кабинет.",
            color = TextSecondary, fontSize = 14.sp,
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ошибка", color = Primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(message, color = TextSecondary, fontSize = 14.sp)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White)
                .clickable { onRetry() }
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text("Повторить", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

/* ---------------- helpers ---------------- */

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

