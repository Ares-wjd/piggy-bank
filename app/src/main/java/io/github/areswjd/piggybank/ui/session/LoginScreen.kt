package io.github.areswjd.piggybank.ui.session

import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.ui.common.PigMascot
import io.github.areswjd.piggybank.util.formatDateTime

@Composable
fun LoginScreen(viewModel: SessionViewModel) {
    val state = viewModel.login

    val accountPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val email = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (result.resultCode == Activity.RESULT_OK && email != null) {
            viewModel.onAccountChosen(email)
        } else {
            viewModel.onAccountPickerCancelled()
        }
    }
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onConsentResult(result.resultCode, result.data)
    }

    LaunchedEffect(Unit) {
        viewModel.consentRequests.collect { pendingIntent ->
            consentLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PigMascot(160.dp)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.login_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))

            if (state.busy) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.login_in_progress), style = MaterialTheme.typography.bodyMedium)
            } else {
                Button(
                    onClick = {
                        accountPicker.launch(
                            AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(stringResource(R.string.login_button), style = MaterialTheme.typography.titleMedium)
                }
            }
            state.errorRes?.let {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.login_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }

    state.restoreOffer?.let { offer ->
        val whenText = offer.modifiedTime?.let { formatDateTime(it.toEpochMilli()) } ?: "-"
        // 바깥을 눌러 닫히지 않게 한다 — 둘 중 하나를 꼭 고르게.
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.restore_found_title)) },
            text = { Text(stringResource(R.string.restore_found_message, whenText)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onRestoreDecision(restore = true) }) {
                    Text(stringResource(R.string.restore_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onRestoreDecision(restore = false) }) {
                    Text(stringResource(R.string.restore_skip))
                }
            },
        )
    }
}
