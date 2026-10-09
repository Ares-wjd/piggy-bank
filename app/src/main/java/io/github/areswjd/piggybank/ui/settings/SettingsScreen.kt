package io.github.areswjd.piggybank.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.areswjd.piggybank.BuildConfig
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.preferences.BackupStatus
import io.github.areswjd.piggybank.model.AppDesign
import io.github.areswjd.piggybank.model.AppIcon
import io.github.areswjd.piggybank.ui.AppViewModelProvider
import io.github.areswjd.piggybank.ui.common.ConfirmDialog
import io.github.areswjd.piggybank.ui.common.PigMascot
import io.github.areswjd.piggybank.ui.common.PiggyCard
import io.github.areswjd.piggybank.ui.design.AppIconImage
import io.github.areswjd.piggybank.ui.design.ThemeSwatch
import io.github.areswjd.piggybank.ui.design.labelRes
import io.github.areswjd.piggybank.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDesign: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    val design by viewModel.design.collectAsStateWithLifecycle()
    val icon by viewModel.icon.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsCard(title = stringResource(R.string.settings_account)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PigMascot(Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(email.orEmpty(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { confirmLogout = true }) { Text(stringResource(R.string.settings_logout)) }
                }
            }

            SettingsCard(title = stringResource(R.string.settings_screen)) {
                DesignRow(design, icon, onClick = onOpenDesign)
            }

            SettingsCard(title = stringResource(R.string.settings_backup)) {
                BackupStatusText(backupStatus)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_auto_backup),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = viewModel::backupNow,
                        enabled = !viewModel.busy,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_backup_now))
                    }
                    OutlinedButton(
                        onClick = { confirmRestore = true },
                        enabled = !viewModel.busy,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_restore))
                    }
                }
                if (viewModel.busy) {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                }
            }

            SettingsCard(title = stringResource(R.string.settings_app_info)) {
                Row {
                    Text(stringResource(R.string.settings_version), modifier = Modifier.weight(1f))
                    Text(appVersion(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                TextButton(
                    onClick = viewModel::checkForUpdate,
                    enabled = !viewModel.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_check_update), modifier = Modifier.fillMaxWidth())
                }
                TextButton(onClick = { showLicenses = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_licenses), modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = stringResource(R.string.settings_logout_title),
            message = stringResource(R.string.settings_logout_message),
            confirmLabel = stringResource(R.string.settings_logout),
            onConfirm = {
                confirmLogout = false
                viewModel.logout()
            },
            onDismiss = { confirmLogout = false },
        )
    }
    if (confirmRestore) {
        ConfirmDialog(
            title = stringResource(R.string.settings_restore_title),
            message = stringResource(R.string.settings_restore_message),
            confirmLabel = stringResource(R.string.restore_action),
            destructive = true,
            onConfirm = {
                confirmRestore = false
                viewModel.restore()
            },
            onDismiss = { confirmRestore = false },
        )
    }
    if (showLicenses) {
        ConfirmDialog(
            title = stringResource(R.string.settings_licenses),
            message = stringResource(R.string.settings_licenses_body),
            confirmLabel = stringResource(R.string.action_confirm),
            onConfirm = { showLicenses = false },
            onDismiss = { showLicenses = false },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    PiggyCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** "디자인 · 딸기우유 · 동전 돼지  [견본][아이콘] >" 한 줄. 누르면 디자인 화면이 열린다. */
@Composable
private fun DesignRow(design: AppDesign, icon: AppIcon, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_design), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.settings_design_summary, stringResource(design.labelRes()), stringResource(icon.labelRes())),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        ThemeSwatch(design, 24.dp)
        Spacer(Modifier.width(6.dp))
        AppIconImage(icon, 28.dp)
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BackupStatusText(status: BackupStatus?) {
    if (status == null) return
    val last = status.lastBackupAt
    Row {
        Text(stringResource(R.string.settings_last_backup), modifier = Modifier.weight(1f))
        Text(
            if (last != null) formatDateTime(last) else stringResource(R.string.settings_never_backed_up),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (status.lastBackupFailed) {
        Text(stringResource(R.string.settings_backup_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    } else if (status.hasPendingChanges) {
        Text(stringResource(R.string.settings_backup_pending), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
    }
}

/** "1.0 (빌드 57)" */
@Composable
private fun appVersion(): String =
    stringResource(R.string.settings_version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
