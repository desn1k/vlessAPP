package com.desn1k.vlessapp

import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.desn1k.vlessapp.core.CoreManager
import com.desn1k.vlessapp.data.Profile
import com.desn1k.vlessapp.ui.EditProfileScreen
import com.desn1k.vlessapp.ui.MainViewModel
import com.desn1k.vlessapp.ui.ProfileListScreen
import com.desn1k.vlessapp.ui.TestScreen
import com.desn1k.vlessapp.ui.theme.VlessAppTheme
import com.desn1k.vlessapp.vpn.XrayVpnService

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var pendingProfile: Profile? = null

    private val vpnPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            pendingProfile?.let { XrayVpnService.start(this, it) }
        }
        pendingProfile = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoreManager.ensureInitialized(applicationContext)
        viewModel.vpnPermissionRequester = { profile -> requestVpnAndConnect(profile) }

        intent?.dataString
            ?.takeIf { it.startsWith("vless://") }
            ?.let { link -> runCatching { viewModel.importLink(link) } }

        setContent {
            VlessAppTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "list") {
                    composable("list") {
                        ProfileListScreen(
                            viewModel = viewModel,
                            onAddProfile = { navController.navigate("edit/new") },
                            onEditProfile = { id -> navController.navigate("edit/$id") },
                            onOpenTests = { navController.navigate("tests") }
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
                        TestScreen(viewModel = viewModel)
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
