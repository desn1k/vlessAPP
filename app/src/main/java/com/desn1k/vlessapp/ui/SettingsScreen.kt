package com.desn1k.vlessapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.desn1k.vlessapp.BuildConfig
import com.desn1k.vlessapp.update.GitHubRelease

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val updateState by viewModel.updateState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Настройки") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(12.dp)) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Vless Checker")
                    Text("Версия: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    Button(onClick = { viewModel.checkForUpdate() }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Проверить обновления")
                    }
                }
            }
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
