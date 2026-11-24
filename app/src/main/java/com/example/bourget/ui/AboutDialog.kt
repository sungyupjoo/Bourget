package com.example.bourget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bourget.R

/** Logo tap: what Bourget is for, under the Paul Bourget line it is named after. */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painterResource(R.drawable.ic_bourget),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(width = 32.dp, height = 40.dp),
            )
        },
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // A blockquote: an ink bar down the left, the line, then the attribution.
                Row(Modifier.height(IntrinsicSize.Min)) {
                    VerticalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
                    Column(
                        Modifier.padding(start = 14.dp, top = 2.dp, bottom = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.about_quote),
                            style = MaterialTheme.typography.titleMedium.copy(lineHeight = 26.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(R.string.about_quote_author),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.info_close)) }
        },
    )
}
