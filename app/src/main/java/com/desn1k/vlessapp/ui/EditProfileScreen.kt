package com.desn1k.vlessapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.desn1k.vlessapp.data.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: MainViewModel,
    profileId: Long?,
    onDone: () -> Unit
) {
    var remark by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }
    var uuid by remember { mutableStateOf("") }
    var flow by remember { mutableStateOf("xtls-rprx-vision") }
    var network by remember { mutableStateOf("tcp") }
    var security by remember { mutableStateOf("reality") }
    var sni by remember { mutableStateOf("") }
    var fingerprint by remember { mutableStateOf("chrome") }
    var publicKey by remember { mutableStateOf("") }
    var shortId by remember { mutableStateOf("") }
    var wsPath by remember { mutableStateOf("") }
    var wsHost by remember { mutableStateOf("") }
    var grpcServiceName by remember { mutableStateOf("") }
    var existingId by remember { mutableStateOf(0L) }

    LaunchedEffect(profileId) {
        if (profileId != null) {
            viewModel.getProfile(profileId)?.let { p ->
                existingId = p.id
                remark = p.remark
                address = p.address
                port = p.port.toString()
                uuid = p.uuid
                flow = p.flow
                network = p.network
                security = p.security
                sni = p.sni
                fingerprint = p.fingerprint
                publicKey = p.publicKey
                shortId = p.shortId
                wsPath = p.wsPath
                wsHost = p.wsHost
                grpcServiceName = p.grpcServiceName
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(if (profileId == null) "Новый профиль" else "Редактировать") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Field("Название", remark) { remark = it }
            Field("Адрес сервера", address) { address = it }
            Field("Порт", port) { port = it }
            Field("UUID", uuid) { uuid = it }
            Field("Flow (xtls-rprx-vision или пусто)", flow) { flow = it }
            Field("Network (tcp/ws/grpc)", network) { network = it }
            Field("Security (none/tls/reality)", security) { security = it }
            Field("SNI", sni) { sni = it }
            Field("Fingerprint", fingerprint) { fingerprint = it }
            Field("Reality public key (pbk)", publicKey) { publicKey = it }
            Field("Reality short id (sid)", shortId) { shortId = it }
            Field("WS path", wsPath) { wsPath = it }
            Field("WS host", wsHost) { wsHost = it }
            Field("gRPC serviceName", grpcServiceName) { grpcServiceName = it }

            Button(
                onClick = {
                    viewModel.saveProfile(
                        Profile(
                            id = existingId,
                            remark = remark.ifBlank { address },
                            address = address,
                            port = port.toIntOrNull() ?: 443,
                            uuid = uuid,
                            flow = flow,
                            network = network,
                            security = security,
                            sni = sni,
                            fingerprint = fingerprint,
                            publicKey = publicKey,
                            shortId = shortId,
                            wsPath = wsPath,
                            wsHost = wsHost,
                            grpcServiceName = grpcServiceName
                        )
                    )
                    onDone()
                },
                modifier = Modifier.padding(top = 16.dp)
            ) { Text("Сохранить") }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
