package com.desn1k.vlessapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.desn1k.vlessapp.data.Profile
import com.desn1k.vlessapp.update.GitHubRelease
import com.desn1k.vlessapp.vpn.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    viewModel: MainViewModel,
    onAddProfile: () -> Unit,
    onEditProfile: (Long) -> Unit,
    onOpenTests: () -> Unit,
    onOpenOperators: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val connection by viewModel.connectionState.collectAsState()
    val selectedId by viewModel.selectedProfileId.collectAsState()
    val importError by viewModel.importError.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Profile?>(null) }

    LaunchedEffect(Unit) { viewModel.checkForUpdate() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Vless Checker") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showImportDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(12.dp)) {
            if (updateState.updateAvailable && updateState.release != null) {
                UpdateBanner(
                    release = updateState.release!!,
                    downloadPercent = updateState.downloadPercent,
                    onDownload = { viewModel.downloadAndInstallUpdate() }
                )
            }

            StatusCard(connection, onOpenTests, onOpenOperators)

            if (profiles.isEmpty()) {
                Text("Нет профилей. Нажмите + чтобы вставить vless:// ссылку или создать вручную.")
            }

            LazyColumn {
                items(profiles, key = { it.id }) { profile ->
                    ProfileRow(
                        profile = profile,
                        isSelected = profile.id == selectedId,
                        isConnected = profile.id == selectedId && connection.status == ConnectionState.Status.CONNECTED,
                        isConnecting = profile.id == selectedId && connection.status == ConnectionState.Status.CONNECTING,
                        onConnect = { viewModel.connect(profile) },
                        onDisconnect = { viewModel.disconnect() },
                        onTest = { viewModel.quickProbe(profile) },
                        onEdit = { onEditProfile(profile.id) },
                        onDelete = { pendingDelete = profile }
                    )
                }
            }
        }
    }

    if (showImportDialog) {
        ImportLinkDialog(
            error = importError,
            onDismiss = { showImportDialog = false },
            onCreateManually = {
                showImportDialog = false
                onAddProfile()
            },
            onImport = { link ->
                viewModel.importLink(link)
                showImportDialog = false
            }
        )
    }

    pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить профиль?") },
            text = { Text(profile.remark) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProfile(profile)
                    pendingDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun StatusCard(connection: ConnectionState.State, onOpenTests: () -> Unit, onOpenOperators: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Статус: ${connection.status}")
            connection.detail?.let { Text(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = onOpenTests) { Text("Проверить соединение") }
                Button(onClick = onOpenOperators) { Text("По операторам") }
            }
        }
    }
}

@Composable
private fun UpdateBanner(release: GitHubRelease, downloadPercent: Int?, onDownload: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Доступно обновление ${release.tagName}")
            when {
                downloadPercent == null -> Button(onClick = onDownload, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Скачать и установить")
                }
                downloadPercent < 100 -> Text("Загрузка: $downloadPercent%")
                else -> Text("Загружено, запуск установки…")
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: Profile,
    isSelected: Boolean,
    isConnected: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(profile.remark)
            Text("${profile.address}:${profile.port}  ·  ${profile.network}/${profile.security}")
            if (profile.lastLatencyMs >= 0) {
                Text("Последняя проверка: ${profile.lastLatencyMs} мс")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (isConnected) {
                    Button(onClick = onDisconnect) { Text("Отключить") }
                } else {
                    Button(onClick = onConnect, enabled = !isConnecting) {
                        Text(if (isConnecting && isSelected) "Подключение…" else "Подключить")
                    }
                }
                TextButton(onClick = onTest) { Text("Тест") }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Изменить") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Удалить") }
            }
        }
    }
}

@Composable
private fun ImportLinkDialog(
    error: String?,
    onDismiss: () -> Unit,
    onCreateManually: () -> Unit,
    onImport: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить сервер") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("vless:// ссылка") },
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it) }
                TextButton(onClick = onCreateManually) { Text("Или создать вручную") }
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(text) }) { Text("Импортировать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
