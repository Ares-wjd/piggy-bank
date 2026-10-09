package io.github.areswjd.piggybank.ui.update

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.update.UpdateState
import kotlin.math.roundToInt

/** 업데이트 상태에 맞는 팝업을 띄운다. 앱 최상단에 한 번 놓는다. */
@Composable
fun UpdateDialogs(viewModel: UpdateViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        UpdateState.Idle -> Unit
        is UpdateState.Available -> AvailableDialog(
            s,
            onUpdate = viewModel::download,
            onSkip = viewModel::skip,
            onLater = viewModel::dismiss,
        )
        is UpdateState.Downloading -> DownloadingDialog(s.progress)
        is UpdateState.ReadyToInstall -> InstallStep(s, viewModel)
        is UpdateState.Failed -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.update_failed_title)) },
            text = { Text(stringResource(R.string.update_failed_message)) },
            confirmButton = { TextButton(onClick = viewModel::download) { Text(stringResource(R.string.update_retry)) } },
            dismissButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.update_close)) } },
        )
    }
}

@Composable
private fun AvailableDialog(
    state: UpdateState.Available,
    onUpdate: () -> Unit,
    onSkip: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.update_available_title, state.release.versionCode)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.release.notes.isNotEmpty()) {
                    Text(
                        state.release.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                    )
                }
                Text(
                    stringResource(R.string.update_available_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onUpdate) { Text(stringResource(R.string.update_now)) } },
        dismissButton = {
            Row {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.update_skip)) }
                TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) }
            }
        },
    )
}

@Composable
private fun DownloadingDialog(progress: Float?) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.update_downloading)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall)
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {},
    )
}

/**
 * 받은 APK를 설치한다. 이 앱에 "다른 앱 설치" 권한이 없으면 먼저 설정 화면으로 안내하고,
 * 돌아왔을 때 허용됐으면 설치 화면을 띄운다.
 */
@Composable
private fun InstallStep(state: UpdateState.ReadyToInstall, viewModel: UpdateViewModel) {
    val context = LocalContext.current
    var permissionCheck by remember { mutableIntStateOf(0) }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        permissionCheck++
    }
    val allowed = remember(permissionCheck) { viewModel.manager.canInstallPackages() }

    LaunchedEffect(allowed, state) {
        if (allowed) {
            context.startActivity(viewModel.manager.installIntent(state.apk))
            viewModel.onInstallerLaunched()
        }
    }

    if (!allowed) {
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.update_permission_title)) },
            text = { Text(stringResource(R.string.update_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    settingsLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
                    )
                }) { Text(stringResource(R.string.update_open_settings)) }
            },
            dismissButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
