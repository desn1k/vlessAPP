package com.desn1k.vlessapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.desn1k.vlessapp.VlessApp
import com.desn1k.vlessapp.core.CoreManager
import com.desn1k.vlessapp.data.Profile
import com.desn1k.vlessapp.data.ProfileRepository
import com.desn1k.vlessapp.test.ConnectivityTester
import com.desn1k.vlessapp.vless.VlessLink
import com.desn1k.vlessapp.vless.XrayConfigFactory
import com.desn1k.vlessapp.vpn.ConnectionState
import com.desn1k.vlessapp.vpn.XrayVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TestReport(
    val probeMs: Long? = null,
    val sites: List<ConnectivityTester.SiteResult> = emptyList(),
    val pings: List<ConnectivityTester.PingResult> = emptyList(),
    val running: Boolean = false,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository((application as VlessApp).database.profileDao())

    val profiles: StateFlow<List<Profile>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val connectionState: StateFlow<ConnectionState.State> = ConnectionState.state

    private val _selectedProfileId = MutableStateFlow<Long?>(null)
    val selectedProfileId: StateFlow<Long?> = _selectedProfileId

    private val _testReport = MutableStateFlow(TestReport())
    val testReport: StateFlow<TestReport> = _testReport

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError

    /**
     * Set by MainActivity to route connection requests through VpnService.prepare() before
     * the tunnel is actually started (Android requires that prompt to come from an Activity).
     */
    var vpnPermissionRequester: ((Profile) -> Unit)? = null

    fun importLink(link: String) {
        viewModelScope.launch {
            try {
                val profile = VlessLink.parse(link)
                repository.save(profile)
                _importError.value = null
            } catch (t: Throwable) {
                _importError.value = t.message ?: "Invalid vless:// link"
            }
        }
    }

    fun saveProfile(profile: Profile) {
        viewModelScope.launch { repository.save(profile) }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            if (_selectedProfileId.value == profile.id) {
                disconnect()
                _selectedProfileId.value = null
            }
            repository.delete(profile)
        }
    }

    suspend fun getProfile(id: Long): Profile? = repository.getById(id)

    fun connect(profile: Profile) {
        _selectedProfileId.value = profile.id
        ConnectionState.update(ConnectionState.Status.CONNECTING, profile.remark)
        val requester = vpnPermissionRequester
        if (requester != null) requester(profile) else XrayVpnService.start(getApplication(), profile)
    }

    fun disconnect() {
        XrayVpnService.stop(getApplication())
    }

    /** Quick check that doesn't require VPN permission: dials the server directly via Xray. */
    fun quickProbe(profile: Profile) {
        viewModelScope.launch {
            _testReport.value = TestReport(running = true)
            val configJson = XrayConfigFactory.forProbe(profile)
            val ms = CoreManager.probeDelay(getApplication(), configJson, "https://www.google.com/generate_204")
            val report = TestReport(probeMs = ms, running = false, error = if (ms < 0) "Probe failed - server unreachable or misconfigured" else null)
            _testReport.value = report
            if (ms >= 0) repository.recordLatency(profile.id, ms)
        }
    }

    /** Full reachability check (sites + tcp pings) assumed to run while the VPN tunnel is up. */
    fun runFullTest() {
        viewModelScope.launch {
            _testReport.value = _testReport.value.copy(running = true)
            val sites = ConnectivityTester.checkSites()
            val pings = ConnectivityTester.pingAll()
            _testReport.value = TestReport(sites = sites, pings = pings, running = false)
        }
    }
}
