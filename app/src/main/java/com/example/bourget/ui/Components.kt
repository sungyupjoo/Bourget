package com.example.bourget.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import com.example.bourget.R
import com.example.bourget.data.LaunchableApp
import com.example.bourget.data.Rule
import com.example.bourget.ui.theme.LocalInactiveTab
import com.example.bourget.data.RuleAction
import com.example.bourget.data.RuleKind

/*
 * Building blocks of the "type" design: no cards or boxes, hierarchy from type size, an ink rule
 * under section heads and light rules between rows. Screen margins are 24dp.
 */

val ScreenMargin = 24.dp

/** Installed apps by package name; null while loading. */
typealias AppDirectory = Map<String, LaunchableApp>?

fun AppDirectory.label(packageName: String): String = this?.get(packageName)?.label ?: packageName

/** "Bourget"-sized title of a screen. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.displaySmall, modifier = modifier.padding(horizontal = ScreenMargin))
}

/** Back arrow on the left and optional actions on the right, above a [ScreenTitle]. */
@Composable
fun BackBar(onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Spacer(Modifier.weight(1f))
        actions()
    }
}

/** Section title with an ink rule under it; [trailing] sits at the right end of the title line. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Column(modifier.padding(horizontal = ScreenMargin)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            trailing()
        }
        InkRule()
    }
}

/** The 1dp ink line under a section head; also the edge between fixed and scrolling areas. */
@Composable
fun InkRule(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface)
}

/** The light 1dp line between rows. */
@Composable
fun RowRule(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier.padding(horizontal = ScreenMargin), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

/** The one filled button on a screen: ink, full width. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Every other button: underlined text. */
@Composable
fun UnderlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color, textDecoration = TextDecoration.Underline)
    }
}

@Composable
fun AppIcon(app: LaunchableApp?, size: Dp, modifier: Modifier = Modifier) {
    if (app == null) {
        Spacer(modifier.size(size))
    } else {
        Image(bitmap = app.icon, contentDescription = null, modifier = modifier.size(size))
    }
}

/** Icon of a rule's target: the app's own icon, or a link icon for a website. */
@Composable
fun RuleIcon(kind: RuleKind, target: String, apps: AppDirectory, size: Dp, modifier: Modifier = Modifier) {
    if (kind == RuleKind.APP) {
        AppIcon(apps?.get(target), size, modifier)
    } else {
        Icon(
            painterResource(R.drawable.ic_link),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(size),
        )
    }
}

/** "Popup" or "→ Open {app}", prefixed with "Off · " when the rule is off. */
@Composable
fun ruleSummary(rule: Rule, apps: AppDirectory): String {
    val launch = rule.launchPackage
    val action = if (rule.action == RuleAction.LAUNCH && launch != null) {
        stringResource(R.string.rule_summary_launch, apps.label(launch))
    } else {
        stringResource(R.string.rule_summary_confirm)
    }
    return if (rule.enabled) action else stringResource(R.string.rule_summary_off, action)
}

/**
 * One rule in a list, the same for apps and websites. The switch is its own touch target;
 * the rest of the row opens the rule ([onClick]). [trailing] adds controls after the switch.
 */
@Composable
fun RuleRow(
    kind: RuleKind,
    rule: Rule,
    name: String,
    apps: AppDirectory,
    onEnabledChange: (Boolean) -> Unit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val switchLabel = stringResource(R.string.rule_switch, name)
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = ScreenMargin, end = 16.dp, top = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.alpha(if (rule.enabled) 1f else 0.4f)) { RuleIcon(kind, rule.target, apps, 28.dp) }
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleLarge,
                color = if (rule.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SecondaryText(ruleSummary(rule, apps))
        }
        Switch(
            checked = rule.enabled,
            onCheckedChange = onEnabledChange,
            modifier = Modifier.semantics { contentDescription = switchLabel },
        )
        trailing()
    }
}

/** Gray supporting text: descriptions, counts, values under a title. */
@Composable
fun SecondaryText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun SecondaryText(text: AnnotatedString, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Search box for the app lists, with a clear button once something is typed. */
@Composable
fun AppSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    CompactTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(R.string.apps_search),
        trailingIcon = {
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = stringResource(R.string.apps_search_clear),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onQueryChange("") }
                        .padding(6.dp)
                        .size(20.dp),
                )
            }
        },
        modifier = modifier,
    )
}

/** Centered message for an empty list or search, with an optional action below it. */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        SecondaryText(body)
        action()
    }
}

/** A tab drawn as large type: the selected one in ink, the other in the inactive tab color. */
@Composable
fun TextTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.headlineSmall,
        color = if (selected) MaterialTheme.colorScheme.onSurface else LocalInactiveTab.current,
        modifier = Modifier
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 6.dp),
    )
}
