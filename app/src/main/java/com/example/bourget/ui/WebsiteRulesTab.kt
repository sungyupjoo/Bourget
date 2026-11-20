package com.example.bourget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.bourget.data.TargetAppsStore
import com.example.bourget.detection.normalizeDomain
import kotlinx.coroutines.launch

/** Website rules: add a domain, toggle or delete it. The input and notice stay fixed. */
@Composable
fun WebsiteRulesTab(store: TargetAppsStore, modifier: Modifier = Modifier) {
    val rules by store.websiteRules.collectAsState(initial = emptyList())
    var input by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val add = {
        val domain = normalizeDomain(input)
        if (domain == null) {
            invalid = true
        } else {
            scope.launch { store.addWebsite(domain) }
            input = ""
            invalid = false
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompactTextField(
                value = input,
                onValueChange = {
                    input = it
                    invalid = false
                },
                placeholder = stringResource(R.string.website_input_hint),
                isError = invalid,
                supportingText = if (invalid) stringResource(R.string.website_input_invalid) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = add) { Text(stringResource(R.string.website_add)) }
        }
        Text(
            stringResource(R.string.website_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        HorizontalDivider(Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurface)
        LazyColumn(Modifier.fillMaxSize()) {
            if (rules.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.website_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    )
                }
            }
            itemsIndexed(rules, key = { _, rule -> rule.domain }) { i, rule ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(rule.domain, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = { scope.launch { store.setWebsiteEnabled(rule.domain, it) } },
                    )
                    IconButton(onClick = { scope.launch { store.removeWebsite(rule.domain) } }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.website_delete, rule.domain),
                        )
                    }
                }
            }
        }
    }
}
