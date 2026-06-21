package com.desn1k.vlessapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.desn1k.vlessapp.sim.OperatorTestResult
import com.desn1k.vlessapp.vpn.ConnectionState

/** Single "Tests" tab: tunnel check, Wi-Fi check, and per-SIM operator checks, all in one place. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestsScreen(viewModel: MainViewModel) {
    val report by viewModel.testReport.collectAsState()
    val connection by viewModel.connectionState.collectAsState()
    val wifiState by viewModel.wifiTestState.collectAsState()
    val operatorState by viewModel.operatorTestState.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val hasProfiles = profiles.isNotEmpty()

    Scaffold(topBar = { TopAppBar(title = { Text("Тесты") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(12.dp).fillMaxSize()) {
            if (!hasProfiles) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text(
                            "Добавьте профиль на главном экране, чтобы запускать проверки.",
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            item { SectionTitle("Через VPN-туннель") }
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("VPN статус: ${connection.status}")
                        Button(
                            onClick = { viewModel.runFullTest() },
                            enabled = hasProfiles && connection.status == ConnectionState.Status.CONNECTED && !report.running,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(if (report.running) "Проверка…" else "Проверить сайты и пинги")
                        }
                        if (connection.status != ConnectionState.Status.CONNECTED) {
                            Text("Подключитесь к серверу на главном экране, чтобы протестировать трафик через туннель.")
                        }
                        report.probeMs?.let { ms ->
                            Text(if (ms >= 0) "Прямая проверка сервера: $ms мс" else "Прямая проверка не удалась")
                        }
                        report.error?.let { Text(it) }
                        report.sites.forEach {
                            Text("${if (it.ok) "OK" else "FAIL"}  ${it.url}  ${it.httpCode ?: ""}  ${it.latencyMs} мс")
                        }
                        report.pings.forEach {
                            Text("${if (it.ok) "OK" else "FAIL"}  ${it.host}:${it.port}  ${it.latencyMs} мс")
                        }
                    }
                }
            }

            item { SectionTitle("Через Wi-Fi") }
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Button(onClick = { viewModel.runWifiTest() }, enabled = hasProfiles && !wifiState.running) {
                            Text(if (wifiState.running) "Проверка…" else "Проверить через Wi-Fi")
                        }
                        if (wifiState.running) CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                        if (wifiState.networkAcquired == false) {
                            Text("Wi-Fi не подключён или недоступен.")
                        }
                        wifiState.serverPing?.let { Text("Сервер: ${if (it.ok) "${it.latencyMs} мс" else "ошибка"}") }
                        wifiState.sites.forEach {
                            Text("${it.url}: ${if (it.ok) "OK ${it.latencyMs}мс" else "ошибка"}")
                        }
                        wifiState.pings.forEach {
                            Text("${it.host}:${it.port} -> ${if (it.ok) "${it.latencyMs}мс" else "ошибка"}")
                        }
                    }
                }
            }

            item { SectionTitle("По SIM-операторам") }
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Button(
                            onClick = { viewModel.loadSims(); viewModel.runOperatorTest() },
                            enabled = hasProfiles && !operatorState.running
                        ) {
                            Text(if (operatorState.running) "Проверка…" else "Проверить все SIM")
                        }
                        if (operatorState.running) CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                        if (operatorState.noSimsFound) {
                            Text("SIM-карты не найдены или нет разрешения READ_PHONE_STATE.", modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
            items(operatorState.results) { result -> OperatorResultCard(result) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
}

@Composable
private fun OperatorResultCard(result: OperatorTestResult) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${result.sim.carrierName} (слот ${result.sim.slotIndex + 1})")
            if (!result.networkAcquired) {
                Text("Не удалось получить сотовую сеть для этой SIM")
                return@Column
            }
            result.serverPing?.let { Text("Сервер: ${if (it.ok) "${it.latencyMs} мс" else "ошибка"}") }
            result.sites.forEach { site ->
                Text("${site.url}: ${if (site.ok) "OK ${site.latencyMs}мс" else "ошибка"}")
            }
            result.pings.forEach { ping ->
                Text("${ping.host}:${ping.port} -> ${if (ping.ok) "${ping.latencyMs}мс" else "ошибка"}")
            }
        }
    }
}
