package com.ravazque.swiftycompanion.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.model.AppError

@Composable
fun AppError.message(): String = when (this) {
    AppError.EmptyLogin -> stringResource(R.string.error_empty_login)
    AppError.InvalidLogin -> stringResource(R.string.error_invalid_login)
    is AppError.NotFound -> stringResource(R.string.error_not_found, login)
    AppError.Network -> stringResource(R.string.error_network)
    AppError.RateLimited -> stringResource(R.string.error_rate_limited)
    AppError.MissingCredentials -> stringResource(R.string.error_missing_credentials)
    AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
    AppError.Forbidden -> stringResource(R.string.error_forbidden)
    is AppError.Server -> stringResource(R.string.error_server, code)
    AppError.UnexpectedResponse -> stringResource(R.string.error_unexpected)
}

// Errors about what the user typed are shown under the text field instead of in a panel.
val AppError.isInputError: Boolean
    get() = this is AppError.EmptyLogin || this is AppError.InvalidLogin || this is AppError.NotFound

val AppError.isRetryable: Boolean
    get() = this is AppError.Network || this is AppError.RateLimited || this is AppError.Forbidden ||
        this is AppError.Server || this is AppError.UnexpectedResponse

@Composable
fun ErrorPanel(error: AppError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(Modifier.padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 6.dp)) {
            Text(error.message(), style = MaterialTheme.typography.bodyMedium)
            if (error.isRetryable) {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
        }
    }
}
