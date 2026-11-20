package com.example.bourget.designlab

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bourget.R
import com.example.bourget.action.objectParticle
import com.example.bourget.data.LaunchableApp

/** A rule as the redesigned main list would show it. Mock only: nothing here is stored. */
data class MockRule(val app: LaunchableApp, val enabled: Boolean, val launchTo: LaunchableApp?)

private fun actionText(rule: MockRule, on: Boolean): String {
    val action = rule.launchTo?.let { "→ ${it.label} 실행" } ?: "확인 창"
    return if (on) action else "꺼짐 · $action"
}

private fun confirmTitle(label: String) = "정말 $label${objectParticle(label)} 열고 싶으신가요?"

@Composable
private fun AppIcon(app: LaunchableApp, size: Int) {
    Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(size.dp))
}

@Composable
private fun inkSwitchColors() = LocalLabStyle.current.p.let { p ->
    SwitchDefaults.colors(
        checkedTrackColor = p.ink,
        checkedThumbColor = p.onInk,
        checkedBorderColor = p.ink,
        uncheckedTrackColor = p.tonal,
        uncheckedThumbColor = p.outline,
        uncheckedBorderColor = p.outline,
    )
}

/* ------------------------------------------------------------------ main list */

@Composable
fun MainListMock(rules: List<MockRule>) {
    when (LocalLabStyle.current.concept) {
        Concept.SYSTEM -> SystemMain(rules)
        Concept.CARDS -> CardsMain(rules)
        Concept.TYPE -> TypeMain(rules)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemMain(rules: List<MockRule>) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Bourget") },
            actions = {
                IconButton(onClick = {}) { Icon(Icons.Outlined.Info, contentDescription = "접근성 권한 안내") }
                IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = "설정") }
            },
            windowInsets = WindowInsets(0),
        )
        TabRow(selectedTabIndex = 0) {
            Tab(selected = true, onClick = {}, text = { Text("앱") })
            Tab(selected = false, onClick = {}, text = { Text("웹사이트") })
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("규칙 ${rules.size}개", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = {}) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("앱 추가", modifier = Modifier.padding(start = 6.dp))
            }
        }
        LazyColumn {
            itemsIndexed(rules, key = { _, r -> r.app.packageName }) { _, rule ->
                var on by remember(rule.app.packageName) { mutableStateOf(rule.enabled) }
                ListItem(
                    headlineContent = { Text(rule.app.label) },
                    supportingContent = { Text(actionText(rule, on)) },
                    leadingContent = { AppIcon(rule.app, 40) },
                    trailingContent = { Switch(checked = on, onCheckedChange = { on = it }) },
                    modifier = Modifier.clickable {},
                )
            }
        }
    }
}

@Composable
private fun CardsMain(rules: List<MockRule>) {
    val p = LocalLabStyle.current.p
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Bourget", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Outlined.Info, contentDescription = "접근성 권한 안내") }
            IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = "설정") }
        }
        // Pill segment in place of the underline tab row.
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                .clip(RoundedCornerShape(50)).background(p.tonal).padding(4.dp),
        ) {
            listOf("앱", "웹사이트").forEachIndexed { i, label ->
                val selected = tab == i
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(50))
                        .background(if (selected) p.card else Color.Transparent)
                        .clickable { tab = i }.heightIn(min = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) p.ink else p.inkVariant)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("규칙 ${rules.size}개", style = MaterialTheme.typography.labelMedium, color = p.inkVariant, modifier = Modifier.weight(1f))
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = p.card, contentColor = p.ink),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("앱 추가", modifier = Modifier.padding(start = 6.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            color = p.card,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth(),
        ) {
            LazyColumn {
                itemsIndexed(rules, key = { _, r -> r.app.packageName }) { i, rule ->
                    if (i > 0) HorizontalDivider(color = p.line, modifier = Modifier.padding(start = 70.dp, end = 16.dp))
                    var on by remember(rule.app.packageName) { mutableStateOf(rule.enabled) }
                    Row(
                        Modifier.fillMaxWidth().clickable {}.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.dimIf(on)) { AppIcon(rule.app, 40) }
                        Column(Modifier.weight(1f)) {
                            Text(rule.app.label, style = MaterialTheme.typography.titleMedium, color = if (on) p.ink else p.inkVariant)
                            Text(actionText(rule, on), style = MaterialTheme.typography.bodySmall, color = p.inkVariant)
                        }
                        Switch(checked = on, onCheckedChange = { on = it }, colors = inkSwitchColors())
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeMain(rules: List<MockRule>) {
    val p = LocalLabStyle.current.p
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Bourget", style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Outlined.Info, contentDescription = "접근성 권한 안내") }
            IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = "설정") }
        }
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("앱", style = MaterialTheme.typography.headlineSmall, color = p.ink)
            Text("웹사이트", style = MaterialTheme.typography.headlineSmall, color = p.inkVariant.copy(alpha = 0.6f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("규칙 ${rules.size}개", style = MaterialTheme.typography.bodyMedium, color = p.inkVariant, modifier = Modifier.weight(1f))
            Text(
                "앱 추가",
                style = MaterialTheme.typography.labelLarge,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {}.padding(vertical = 10.dp),
            )
        }
        HorizontalDivider(color = p.ink, thickness = 1.dp)
        LazyColumn {
            itemsIndexed(rules, key = { _, r -> r.app.packageName }) { i, rule ->
                if (i > 0) HorizontalDivider(color = p.line)
                var on by remember(rule.app.packageName) { mutableStateOf(rule.enabled) }
                Row(
                    Modifier.fillMaxWidth().clickable {}.padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.dimIf(on)) { AppIcon(rule.app, 28) }
                    Column(Modifier.weight(1f)) {
                        Text(rule.app.label, style = MaterialTheme.typography.titleLarge, color = if (on) p.ink else p.inkVariant)
                        Text(actionText(rule, on), style = MaterialTheme.typography.bodyMedium, color = p.inkVariant)
                    }
                    Switch(checked = on, onCheckedChange = { on = it }, colors = inkSwitchColors())
                }
            }
        }
    }
}

/** Disabled rules keep their row but fade the icon. */
private fun Modifier.dimIf(on: Boolean) = if (on) this else this.alpha(0.4f)

/* ------------------------------------------------------------------ confirm popup */

/** Stand-in for the app underneath: Bourget cannot control it, so it is only gray blocks. */
@Composable
private fun FakeAppBehind(darkApp: Boolean) {
    val bg = if (darkApp) Color(0xFF0F0F0F) else Color(0xFFFFFFFF)
    val block = if (darkApp) Color(0xFF2A2A2A) else Color(0xFFE8EAED)
    Column(Modifier.fillMaxSize().background(bg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth(0.4f).height(24.dp).clip(RoundedCornerShape(6.dp)).background(block))
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)).background(block))
            Box(Modifier.fillMaxWidth(0.7f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(block))
        }
    }
}

@Composable
fun PopupMock(app: LaunchableApp, darkApp: Boolean) {
    var shown by remember(app.packageName) { mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        FakeAppBehind(darkApp)
        if (shown) {
            when (LocalLabStyle.current.concept) {
                Concept.SYSTEM -> SystemPopup(app) { shown = false }
                Concept.CARDS -> CardsPopup(app) { shown = false }
                Concept.TYPE -> TypePopup(app) { shown = false }
            }
        } else {
            TextButton(onClick = { shown = true }, modifier = Modifier.align(Alignment.Center)) {
                Text("확인 창 다시 띄우기", color = if (darkApp) Color.White else Color.Black)
            }
        }
    }
}

@Composable
private fun Scrim(alpha: Float, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = alpha)).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun SystemPopup(app: LaunchableApp, onClose: () -> Unit) = Scrim(0.32f) {
    Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 360.dp)) {
        Column(Modifier.padding(24.dp)) {
            AppIcon(app, 32)
            Text(confirmTitle(app.label), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp, bottom = 24.dp))
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClose) { Text("돌아가기") }
                TextButton(onClick = onClose) { Text("계속 사용하기") }
            }
        }
    }
}

@Composable
private fun CardsPopup(app: LaunchableApp, onClose: () -> Unit) = Scrim(0.32f) {
    val p = LocalLabStyle.current.p
    Surface(color = p.card, shape = RoundedCornerShape(28.dp), shadowElevation = 12.dp, modifier = Modifier.widthIn(max = 360.dp)) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)) {
            Box(Modifier.padding(start = 4.dp)) { AppIcon(app, 40) }
            Text(confirmTitle(app.label), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 22.dp))
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = p.ink, contentColor = p.onInk),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("돌아가기") }
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("계속 사용하기", color = p.ink)
            }
        }
    }
}

/** The pause as the hero: the app underneath is almost covered and the question is set large. */
@Composable
private fun TypePopup(app: LaunchableApp, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xF2141312)).padding(horizontal = 32.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        AppIcon(app, 36)
        Text(
            confirmTitle(app.label),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp, lineHeight = 40.sp),
            color = Color(0xFFF4F1EC),
            modifier = Modifier.padding(top = 20.dp, bottom = 40.dp),
        )
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4F1EC), contentColor = Color(0xFF141312)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { Text("돌아가기") }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("계속 사용하기", color = Color(0xFFBDB7AE), textDecoration = TextDecoration.Underline)
        }
    }
}

/* ------------------------------------------------------------------ consent */

@Composable
fun ConsentMock() {
    val style = LocalLabStyle.current
    val p = style.p
    val title = stringResource(R.string.main_disclosure_title)
    val body = stringResource(R.string.main_disclosure_body)
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = if (style.concept == Concept.TYPE) 28.dp else 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (style.concept) {
                Concept.SYSTEM -> {
                    Text("Bourget", style = MaterialTheme.typography.headlineMedium)
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
                Concept.CARDS -> {
                    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 4.dp))
                    Surface(color = p.card, shape = RoundedCornerShape(22.dp)) {
                        Text(body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(18.dp))
                    }
                }
                Concept.TYPE -> {
                    Text(title, style = MaterialTheme.typography.displaySmall)
                    HorizontalDivider(color = p.ink)
                    Text(body, style = MaterialTheme.typography.bodyLarge)
                }
            }
            TextButton(onClick = {}) { Text("개인정보 처리방침 보기") }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (style.concept == Concept.SYSTEM) {
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("동의하고 접근성 설정 열기") }
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("동의하지 않음") }
            } else {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = p.ink, contentColor = p.onInk),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text("동의하고 접근성 설정 열기") }
                TextButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("동의하지 않음", color = p.ink) }
            }
        }
    }
}
