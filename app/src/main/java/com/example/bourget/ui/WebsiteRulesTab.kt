package com.example.bourget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.bourget.R
import com.example.bourget.data.Rule
import com.example.bourget.data.RuleKind
import com.example.bourget.data.TargetAppsStore
import com.example.bourget.detection.normalizeDomain
import kotlinx.coroutines.launch

private enum class InputError(val message: Int) {
    EMPTY(R.string.website_error_empty),
    INVALID(R.string.website_error_invalid),
    DUPLICATE(R.string.website_error_duplicate),
}

/** Website rules: add a domain at the top; the input, notice and header stay fixed. */
@Composable
fun WebsiteRulesTab(
    store: TargetAppsStore,
    rules: List<Rule>?,
    apps: AppDirectory,
    showSnackbar: ShowSnackbar,
    onOpenRule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<InputError?>(null) }
    var showDetail by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val added = stringResource(R.string.website_added)
    val add = {
        val domain = normalizeDomain(input)
        error = when {
            input.isBlank() -> InputError.EMPTY
            domain == null -> InputError.INVALID
            rules.orEmpty().any { it.target == domain } -> InputError.DUPLICATE
            else -> null
        }
        if (error == null && domain != null) {
            scope.launch { store.addRule(RuleKind.WEBSITE, domain) }
            input = ""
            showSnackbar(added, null)
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = ScreenMargin).padding(top = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompactTextField(
                value = input,
                onValueChange = {
                    input = it
                    // The error goes away as soon as the user starts fixing the input.
                    error = null
                },
                placeholder = stringResource(R.string.website_input_hint),
                isError = error != null,
                supportingText = error?.let { stringResource(it.message) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = add, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.website_add), style = MaterialTheme.typography.labelLarge)
            }
        }
        Column(Modifier.padding(horizontal = ScreenMargin).padding(top = 4.dp)) {
            Text(
                stringResource(R.string.website_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showDetail) {
                Text(
                    stringResource(R.string.website_notice_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = ScreenMargin, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.rules_count, rules.orEmpty().size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            UnderlineButton(
                stringResource(if (showDetail) R.string.website_notice_less else R.string.website_notice_more),
                onClick = { showDetail = !showDetail },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        InkRule(Modifier.padding(horizontal = ScreenMargin))
        when {
            rules == null -> Unit
            rules.isEmpty() -> EmptyState(
                stringResource(R.string.rules_empty_title),
                stringResource(R.string.rules_empty_websites),
            )
            else -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(rules, key = { _, rule -> rule.target }) { i, rule ->
                    if (i > 0) RowRule()
                    RuleRow(
                        kind = RuleKind.WEBSITE,
                        rule = rule,
                        name = rule.target,
                        apps = apps,
                        onEnabledChange = { on ->
                            scope.launch { store.updateRule(RuleKind.WEBSITE, rule.target) { it.copy(enabled = on) } }
                        },
                        onClick = { onOpenRule(rule.target) },
                    )
                }
            }
        }
    }
}
