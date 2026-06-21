package com.desn1k.vlessapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.desn1k.vlessapp.sim.OperatorTestResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorTestScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val state by viewModel.operatorTestState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSims() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Проверка по операторам") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(12.dp)) {
            Button(onClick = { viewModel.runOperatorTest() }, enabled = !state.running) {
                Text(if (state.running) "Проверка…" else "Проверить все SIM")
            }
            if (state.running) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp))
            }
            if (state.noSimsFound) {
                Text("SIM-карты не найдены или нет разрешения READ_PHONE_STATE.")
            }
            LazyColumn {
                items(state.results) { result -> OperatorResultCard(result) }
            }
        }
    }
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
