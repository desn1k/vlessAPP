package com.desn1k.vlessapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.desn1k.vlessapp.BuildConfig
import com.desn1k.vlessapp.VlessApp
import com.desn1k.vlessapp.core.CoreManager
import com.desn1k.vlessapp.data.Profile
import com.desn1k.vlessapp.data.ProfileRepository
import com.desn1k.vlessapp.prefs.AppPreferences
import com.desn1k.vlessapp.prefs.ThemeMode
import com.desn1k.vlessapp.sim.MultiSimTester
import com.desn1k.vlessapp.sim.OperatorTestResult
import com.desn1k.vlessapp.sim.SimInfo
import com.desn1k.vlessapp.sim.SimManager
import com.desn1k.vlessapp.sim.WifiNetworkBinder
import com.desn1k.vlessapp.test.ConnectivityTester
import com.desn1k.vlessapp.update.ApkInstaller
import com.desn1k.vlessapp.update.GitHubRelease
import com.desn1k.vlessapp.update.UpdateChecker
import com.desn1k.vlessapp.vless.SubscriptionImporter
import com.desn1k.vlessapp.vless.VlessLink
import com.desn1k.vlessapp.vless.XrayConfigFactory
import com.desn1k.vlessapp.vpn.ConnectionState
import com.desn1k.vlessapp.vpn.XrayVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class TestReport(
    val probeMs: Long? = null,
    val sites: List<ConnectivityTester.SiteResult> = emptyList(),
    val pings: List<ConnectivityTester.PingResult> = emptyList(),
    val running: Boolean = false,
    val error: String? = null
)

data class UpdateState(
    val checking: Boolean = false,
    val release: GitHubRelease? = null,
    val updateAvailable: Boolean = false,
    val downloadPercent: Int? = null,
    val downloadedFile: File? = null,
    val error: String? = null
)

data class OperatorTestState(
    val running: Boolean = false,
    val results: List<OperatorTestResult> = emptyList(),
    val noSimsFound: Boolean = false
)

data class WifiTestState(
    val running: Boolean = false,
    val networkAcquired: Boolean? = null,
    val serverPing: ConnectivityTester.PingResult? = null,
    val sites: List<ConnectivityTester.SiteResult> = emptyList(),
    val pings: List<ConnectivityTester.PingResult> = emptyList()
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

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState

    private val _operatorTestState = MutableStateFlow(OperatorTestState())
    val operatorTestState: StateFlow<OperatorTestState> = _operatorTestState

    private val _sims = MutableStateFlow<List<SimInfo>>(emptyList())
    val sims: StateFlow<List<SimInfo>> = _sims

    private val _wifiTestState = MutableStateFlow(WifiTestState())
    val wifiTestState: StateFlow<WifiTestState> = _wifiTestState

    private val _themeMode = MutableStateFlow(AppPreferences.getThemeMode(application))
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _subscriptions = MutableStateFlow(AppPreferences.getSubscriptions(application).toList())
    val subscriptions: StateFlow<List<String>> = _subscriptions

    private val _subscriptionError = MutableStateFlow<String?>(null)
    val subscriptionError: StateFlow<String?> = _subscriptionError

    fun setThemeMode(mode: ThemeMode) {
        AppPreferences.setThemeMode(getApplication(), mode)
        _themeMode.value = mode
    }

    /** Fetches a subscription link, imports every vless:// entry it contains, and remembers the URL for later refresh. */
    fun importSubscription(url: String) {
        viewModelScope.launch {
            try {
                val imported = SubscriptionImporter.fetch(url)
                if (imported.isEmpty()) {
                    _subscriptionError.value = "Подписка не содержит ссылок vless://"
                    return@launch
                }
                imported.forEach { repository.saveDeduped(it) }
                AppPreferences.addSubscription(getApplication(), url)
                _subscriptions.value = AppPreferences.getSubscriptions(getApplication()).toList()
                _subscriptionError.value = null
            } catch (t: Throwable) {
                _subscriptionError.value = t.message ?: "Не удалось загрузить подписку"
            }
        }
    }

    fun removeSubscription(url: String) {
        AppPreferences.removeSubscription(getApplication(), url)
        _subscriptions.value = AppPreferences.getSubscriptions(getApplication()).toList()
    }

    /**
     * Set by MainActivity to route connection requests through VpnService.prepare() before
     * the tunnel is actually started (Android requires that prompt to come from an Activity).
     */
    var vpnPermissionRequester: ((Profile) -> Unit)? = null

    fun reportImportError(message: String) {
        _importError.value = message
    }

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

    fun loadSims() {
        _sims.value = SimManager.listActiveSims(getApplication())
    }

    /** Runs site/ping checks pinned to each active SIM's cellular network, plus a TCP ping to the active profile's server. */
    fun runOperatorTest() {
        viewModelScope.launch {
            _operatorTestState.value = OperatorTestState(running = true)
            val app: android.app.Application = getApplication()
            val sims = SimManager.listActiveSims(app)
            if (sims.isEmpty()) {
                _operatorTestState.value = OperatorTestState(running = false, noSimsFound = true)
                return@launch
            }
            val profile = _selectedProfileId.value?.let { repository.getById(it) }
            val results = MultiSimTester.runForAllSims(app, profile)
            _operatorTestState.value = OperatorTestState(running = false, results = results)
        }
    }

    /** Runs the same reachability checks pinned explicitly to the active Wi-Fi network. */
    fun runWifiTest() {
        viewModelScope.launch {
            _wifiTestState.value = WifiTestState(running = true)
            val app: android.app.Application = getApplication()
            val network = WifiNetworkBinder.requestWifiNetwork(app)
            if (network == null) {
                _wifiTestState.value = WifiTestState(running = false, networkAcquired = false)
                return@launch
            }
            val profile = _selectedProfileId.value?.let { repository.getById(it) }
            val serverPing = profile?.let { ConnectivityTester.tcpPing(it.address, it.port, network) }
            val sites = ConnectivityTester.checkSites(network = network)
            val pings = ConnectivityTester.pingAll(network = network)
            _wifiTestState.value = WifiTestState(
                running = false,
                networkAcquired = true,
                serverPing = serverPing,
                sites = sites,
                pings = pings
            )
        }
    }

    private var lastUpdateCheckAtMs = 0L

    /** Guards against hammering the GitHub API (and tripping its rate limit) from repeated taps. */
    fun checkForUpdate() {
        val now = System.currentTimeMillis()
        if (_updateState.value.checking || now - lastUpdateCheckAtMs < UPDATE_CHECK_COOLDOWN_MS) return
        lastUpdateCheckAtMs = now
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(checking = true, error = null)
            val release = UpdateChecker.fetchLatestRelease()
            if (release == null) {
                lastUpdateCheckAtMs = 0L
                _updateState.value = UpdateState(checking = false, error = "Could not check for updates")
                return@launch
            }
            val isNewer = UpdateChecker.isNewer(release.tagName, BuildConfig.VERSION_NAME)
            _updateState.value = UpdateState(checking = false, release = release, updateAvailable = isNewer)
        }
    }

    private companion object {
        const val UPDATE_CHECK_COOLDOWN_MS = 15_000L
    }

    fun downloadAndInstallUpdate() {
        val release = _updateState.value.release ?: return
        val apkUrl = release.apkDownloadUrl ?: return
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(downloadPercent = 0)
            ApkInstaller.download(getApplication(), apkUrl) { progress ->
                when (progress) {
                    is ApkInstaller.DownloadProgress.Progress ->
                        _updateState.value = _updateState.value.copy(downloadPercent = progress.percent)
                    is ApkInstaller.DownloadProgress.Done -> {
                        _updateState.value = _updateState.value.copy(downloadPercent = 100, downloadedFile = progress.file)
                        ApkInstaller.install(getApplication(), progress.file)
                    }
                    is ApkInstaller.DownloadProgress.Failed ->
                        _updateState.value = _updateState.value.copy(downloadPercent = null, error = progress.message)
                }
            }
        }
    }
}
