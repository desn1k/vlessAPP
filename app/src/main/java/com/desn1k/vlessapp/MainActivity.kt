package com.desn1k.vlessapp

import android.Manifest
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.desn1k.vlessapp.core.CoreManager
import com.desn1k.vlessapp.data.Profile
import com.desn1k.vlessapp.ui.EditProfileScreen
import com.desn1k.vlessapp.ui.MainViewModel
import com.desn1k.vlessapp.ui.ProfileListScreen
import com.desn1k.vlessapp.ui.SettingsScreen
import com.desn1k.vlessapp.ui.TestsScreen
import com.desn1k.vlessapp.ui.theme.VlessAppTheme
import com.desn1k.vlessapp.vpn.XrayVpnService
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab("list", "Главная", Icons.Filled.Home),
    BottomTab("tests", "Тесты", Icons.Filled.NetworkCheck),
    BottomTab("settings", "Настройки", Icons.Filled.Settings)
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var pendingProfile: Profile? = null

    private val vpnPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            pendingProfile?.let { XrayVpnService.start(this, it) }
        }
        pendingProfile = null
    }

    private val phoneStatePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result observed via SimManager.hasPermission */ }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchQrScanner()
        }

    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.takeIf { it.startsWith("vless://") }?.let { viewModel.importLink(it) }
    }

    private val exportBackupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                val json = viewModel.exportBackupAsync()
                contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
        }

    private val importBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            json?.let { viewModel.importBackupJson(it) }
        }

    private fun launchQrScanner() {
        qrScanLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Сканируйте QR-код профиля")
                .setBeepEnabled(false)
        )
    }

    fun requestQrScan() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun requestExportBackup() {
        exportBackupLauncher.launch("vless-checker-backup.json")
    }

    fun requestImportBackup() {
        importBackupLauncher.launch(arrayOf("application/json"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoreManager.ensureInitialized(applicationContext)
        viewModel.vpnPermissionRequester = { profile -> requestVpnAndConnect(profile) }
        phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        viewModel.checkForUpdate()

        intent?.dataString
            ?.takeIf { it.startsWith("vless://") }
            ?.let { link -> runCatching { viewModel.importLink(link) } }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            VlessAppTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
                val updateState by viewModel.updateState.collectAsState()
                val showBottomBar = bottomTabs.any { it.route == currentRoute }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                bottomTabs.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            if (tab.route == "settings" && updateState.updateAvailable) {
                                                BadgedBox(badge = { Badge(modifier = Modifier.size(8.dp)) }) {
                                                    Icon(tab.icon, contentDescription = tab.label)
                                                }
                                            } else {
                                                Icon(tab.icon, contentDescription = tab.label)
                                            }
                                        },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = "list",
                        modifier = Modifier.padding(padding)
                    ) {
                        composable("list") {
                            ProfileListScreen(
                                viewModel = viewModel,
                                onAddProfile = { navController.navigate("edit/new") },
                                onEditProfile = { id -> navController.navigate("edit/$id") },
                                onScanQr = { requestQrScan() }
                            )
                        }
                        composable("edit/{id}") { backStackEntry ->
                            val idArg = backStackEntry.arguments?.getString("id")
                            val profileId = idArg?.toLongOrNull()
                            EditProfileScreen(
                                viewModel = viewModel,
                                profileId = profileId,
                                onDone = { navController.popBackStack() }
                            )
                        }
                        composable("tests") {
                            TestsScreen(viewModel = viewModel)
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onExportBackup = { requestExportBackup() },
                                onImportBackup = { requestImportBackup() }
                            )
                        }
                    }
                }
            }
        }
    }

    /** Call this instead of XrayVpnService.start directly so the VpnService.prepare() flow is honoured. */
    fun requestVpnAndConnect(profile: Profile) {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            pendingProfile = profile
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            XrayVpnService.start(this, profile)
        }
    }
}
