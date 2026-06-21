package com.desn1k.vlessapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.desn1k.vlessapp.vpn.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(viewModel: MainViewModel) {
    val report by viewModel.testReport.collectAsState()
    val connection by viewModel.connectionState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Проверка подключения") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(12.dp)) {
            Text("VPN статус: ${connection.status}")

            Button(
                onClick = { viewModel.runFullTest() },
                enabled = connection.status == ConnectionState.Status.CONNECTED && !report.running,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(if (report.running) "Проверка…" else "Проверить сайты и пинги")
            }
            if (connection.status != ConnectionState.Status.CONNECTED) {
                Text("Подключитесь к серверу, чтобы протестировать реальный трафик через туннель.")
            }

            report.probeMs?.let { ms ->
                Text(if (ms >= 0) "Прямая проверка сервера: $ms мс" else "Прямая проверка не удалась")
            }
            report.error?.let { Text(it) }

            if (report.sites.isNotEmpty()) {
                Text("— Сайты —")
                report.sites.forEach {
                    Text("${if (it.ok) "OK" else "FAIL"}  ${it.url}  ${it.httpCode ?: ""}  ${it.latencyMs} мс  ${it.error ?: ""}")
                }
            }
            if (report.pings.isNotEmpty()) {
                Text("— Пинги (TCP connect) —")
                report.pings.forEach {
                    Text("${if (it.ok) "OK" else "FAIL"}  ${it.host}:${it.port}  ${it.latencyMs} мс  ${it.error ?: ""}")
                }
            }
        }
    }
}
