package com.desn1k.vlessapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.desn1k.vlessapp.BuildConfig
import com.desn1k.vlessapp.prefs.ThemeMode
import com.desn1k.vlessapp.update.GitHubRelease

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val updateState by viewModel.updateState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val subscriptionError by viewModel.subscriptionError.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Настройки") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(12.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    when {
                        updateState.checking -> Text("Проверка обновлений…")
                        updateState.updateAvailable && updateState.release != null ->
                            UpdateAvailableBlock(updateState.release!!, updateState.downloadPercent) {
                                viewModel.downloadAndInstallUpdate()
                            }
                        updateState.error != null -> Text("Не удалось проверить обновления: ${updateState.error}")
                        else -> Text("У вас последняя версия приложения.")
                    }
                    Button(
                        onClick = { viewModel.checkForUpdate() },
                        enabled = !updateState.checking,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Проверить обновления")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Тема приложения", style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        val options = listOf(
                            ThemeMode.SYSTEM to "Системная",
                            ThemeMode.LIGHT to "Светлая",
                            ThemeMode.DARK to "Тёмная"
                        )
                        options.forEach { (mode, label) ->
                            val selected = themeMode == mode
                            if (selected) {
                                FilledTonalButton(
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text(label) }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text(label) }
                            }
                        }
                    }
                }
            }

            if (subscriptions.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Подписки", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Добавляются на главном экране",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        subscriptions.forEach { url ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(url, modifier = Modifier.padding(end = 8.dp))
                                Row {
                                    TextButton(onClick = { viewModel.importSubscription(url) }) { Text("Обновить") }
                                    IconButton(onClick = { viewModel.removeSubscription(url) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Удалить подписку")
                                    }
                                }
                            }
                        }
                        subscriptionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }

            Text(
                "Vless Checker · ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun UpdateAvailableBlock(release: GitHubRelease, downloadPercent: Int?, onDownload: () -> Unit) {
    Column {
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
