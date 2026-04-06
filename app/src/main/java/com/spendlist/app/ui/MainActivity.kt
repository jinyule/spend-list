package com.spendlist.app.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spendlist.app.data.datastore.UserPreferences
import com.spendlist.app.ui.navigation.BottomNavBar
import com.spendlist.app.ui.navigation.Screen
import com.spendlist.app.ui.navigation.SpendListNavHost
import com.spendlist.app.ui.theme.SpendListTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferences.themeMode.collectAsState(initial = 0)

            SpendListTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // SAF export state
                var pendingExportData by remember { mutableStateOf<String?>(null) }

                // SAF JSON export launcher
                val exportJsonLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json")
                ) { uri: Uri? ->
                    Log.d("MainActivity", "JSON Export result: uri=$uri, data length=${pendingExportData?.length}")
                    if (uri != null && pendingExportData != null) {
                        try {
                            contentResolver.openOutputStream(uri)?.use { outputStream ->
                                outputStream.write(pendingExportData!!.toByteArray())
                                Log.d("MainActivity", "JSON Export success: ${pendingExportData!!.length} bytes written")
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "JSON Export failed", e)
                        }
                    }
                    pendingExportData = null
                }

                // SAF CSV export launcher
                var pendingCsvData by remember { mutableStateOf<String?>(null) }
                val exportCsvLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/csv")
                ) { uri: Uri? ->
                    Log.d("MainActivity", "CSV Export result: uri=$uri, data length=${pendingCsvData?.length}")
                    if (uri != null && pendingCsvData != null) {
                        try {
                            contentResolver.openOutputStream(uri)?.use { outputStream ->
                                outputStream.write(pendingCsvData!!.toByteArray())
                                Log.d("MainActivity", "CSV Export success: ${pendingCsvData!!.length} bytes written")
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "CSV Export failed", e)
                        }
                    }
                    pendingCsvData = null
                }

                // SAF import launcher
                val importLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            val content = inputStream.bufferedReader().readText()
                            // Pass content to SettingsViewModel via navigation
                            navController.currentBackStackEntry?.savedStateHandle?.set("import_content", content)
                        }
                    }
                }

                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Stats.route,
                    Screen.Settings.route
                )

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    SpendListNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        onExportData = { data, filename ->
                            Log.d("MainActivity", "onExportData called: filename=$filename, data length=${data.length}")
                            if (filename.endsWith(".csv")) {
                                pendingCsvData = data
                                exportCsvLauncher.launch(filename)
                            } else {
                                pendingExportData = data
                                exportJsonLauncher.launch(filename)
                            }
                        },
                        onRequestImport = {
                            importLauncher.launch(arrayOf("application/json", "text/csv"))
                        }
                    )
                }
            }
        }
    }
}
