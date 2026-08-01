package com.htmltoapk.studio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * A labelled text-field row.
 *
 * The `placeholder` and `supportingText` lambdas are intentionally annotated
 * with `@Composable` so that the compiler treats them as composable lambdas
 * even when they appear inside an `if-else` expression (where Kotlin's
 * type inference would otherwise fall back to `() -> Unit` and reject
 * the `Text(...)` call inside).
 */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    // Build the optional composable lambdas up-front so their
    // @Composable nature is unambiguous to the compiler.
    val placeholderContent: (@Composable () -> Unit)? =
        if (placeholder.isNotEmpty()) {
            @Composable { Text(placeholder) }
        } else null

    val supportingContent: (@Composable () -> Unit)? =
        if (errorMessage != null) {
            @Composable { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
        } else null

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            trailing?.invoke()
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholderContent,
            isError = isError,
            supportingText = supportingContent,
            singleLine = singleLine,
            enabled = enabled,
            keyboardType = keyboardType,
            visualTransformation = visualTransformation,
            shape = MaterialTheme.shapes.small
        )
    }
}
