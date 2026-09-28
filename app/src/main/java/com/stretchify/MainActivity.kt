package com.stretchify

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stretchify.ui.SessionViewModel
import com.stretchify.ui.StretchifyApp
import com.stretchify.ui.StretchifyEvent
import com.stretchify.ui.theme.StretchifyTheme
import com.stretchify.widget.StretchWidgetProvider

class MainActivity : ComponentActivity()
{
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by sessionViewModel.uiState.collectAsStateWithLifecycle()
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            val reminderPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted)
                {
                    sessionViewModel.onEvent(StretchifyEvent.SetRemindersEnabled(true))
                }
            }
            StretchifyTheme(
                themePreference = uiState.themePreference,
                liquidPreset = uiState.liquidPreset,
                glassFinish = uiState.glassFinish
            ) {
                StretchifyApp(
                    sessionViewModel = sessionViewModel,
                    requestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED)
                        {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    enableRemindersWithPermission = {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED)
                        {
                            sessionViewModel.onEvent(StretchifyEvent.SetRemindersEnabled(true))
                        }
                        else
                        {
                            reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
        }
        if (savedInstanceState == null)
        {
            startWidgetRoutine(intent)
        }
    }

    override fun onNewIntent(intent: Intent)
    {
        super.onNewIntent(intent)
        startWidgetRoutine(intent)
    }

    private fun startWidgetRoutine(intent: Intent)
    {
        if (intent.action == StretchWidgetProvider.ACTION_START_ROUTINE)
        {
            val routineId = intent.getStringExtra(StretchWidgetProvider.EXTRA_ROUTINE_ID) ?: return
            sessionViewModel.onEvent(StretchifyEvent.StartRoutineById(routineId))
        }
    }
}
