package com.example.bourget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.data.Rule
import com.example.bourget.data.RuleAction
import com.example.bourget.data.RuleKind
import com.example.bourget.data.applyTemplate

/**
 * One rule: on/off, what happens when its app or site opens, and deletion. Changes are saved at
 * once. Choosing "다른 앱 실행" without an app to launch goes straight to the launch picker; the
 * rule stays on "확인 창 표시" until an app is picked. "규칙 삭제" sits at the right end of the icon row.
 */
@Composable
fun RuleDetailScreen(
    kind: RuleKind,
    rule: Rule,
    apps: AppDirectory,
    appRuleTargets: Set<String>,
    template: String,
    onBack: () -> Unit,
    onUpdate: ((Rule) -> Rule) -> Unit,
    onPickLaunchApp: () -> Unit,
    onEditTemplate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = if (kind == RuleKind.APP) apps.label(rule.target) else rule.target
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val launch = rule.launchPackage.takeIf { rule.action == RuleAction.LAUNCH }

    Column(modifier) {
        BackBar(onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(
                Modifier.padding(horizontal = ScreenMargin).padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RuleIcon(kind, rule.target, apps, 48.dp)
                    Spacer(Modifier.weight(1f))
                    UnderlineButton(
                        stringResource(R.string.rule_delete),
                        onClick = { confirmDelete = true },
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (kind == RuleKind.APP) rule.target else stringResource(R.string.rule_subdomains),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(rule.enabled, role = Role.Switch) { on -> onUpdate { it.copy(enabled = on) } }
                    .padding(horizontal = ScreenMargin, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.rule_enabled), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = rule.enabled, onCheckedChange = null)
            }
            SectionHeader(
                stringResource(if (kind == RuleKind.APP) R.string.rule_section_app else R.string.rule_section_website),
                Modifier.padding(top = 16.dp),
            )
            Column(Modifier.selectableGroup()) {
                ActionOption(
                    title = stringResource(R.string.action_confirm),
                    description = stringResource(R.string.action_confirm_desc),
                    selected = launch == null,
                    onClick = { onUpdate { it.copy(action = RuleAction.CONFIRM, launchPackage = null) } },
                ) {
                    Text(applyTemplate(template, name), style = MaterialTheme.typography.titleMedium)
                    // Ink with light text and slightly rounded corners: set apart from the plain
                    // underlined buttons, since it leads to another screen.
                    Button(
                        onClick = onEditTemplate,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(stringResource(R.string.template_change), style = MaterialTheme.typography.labelLarge)
                    }
                }
                RowRule()
                ActionOption(
                    title = stringResource(R.string.action_launch),
                    description = stringResource(
                        if (kind == RuleKind.APP) R.string.action_launch_desc_app else R.string.action_launch_desc_website,
                    ),
                    selected = launch != null,
                    onClick = { if (launch == null) onPickLaunchApp() },
                ) {
                    if (launch != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            AppIcon(apps?.get(launch), 28.dp)
                            Text(apps.label(launch), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            UnderlineButton(stringResource(R.string.action_change), onClick = onPickLaunchApp)
                        }
                        if (launch in appRuleTargets) {
                            Text(
                                stringResource(R.string.launch_has_rule, apps.label(launch)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.rule_delete_title)) },
            text = { Text(stringResource(R.string.rule_delete_body, name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** A radio row; the chosen option's [details] appear under it, indented, without a box. */
@Composable
private fun ActionOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    details: @Composable () -> Unit,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .selectable(selected, role = Role.RadioButton, onClick = onClick)
                .padding(start = 12.dp, end = ScreenMargin, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (selected) {
            Column(
                Modifier.padding(start = 60.dp, end = ScreenMargin, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { details() }
        }
    }
}
