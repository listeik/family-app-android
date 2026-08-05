package com.listeik.familyapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.listeik.familyapp.data.repository.FirestoreFamilyRepository
import com.listeik.familyapp.ui.FamilyApp
import com.listeik.familyapp.ui.FamilyViewModel
import com.listeik.familyapp.ui.FirebaseSetupScreen
import com.listeik.familyapp.ui.theme.FamilyAppTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val familyViewModel: FamilyViewModel by viewModels {
        FamilyViewModel.factory(FirestoreFamilyRepository(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firebaseReady = FirebaseApp.initializeApp(this) != null

        setContent {
            FamilyAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (firebaseReady) {
                        FamilyApp(familyViewModel)
                    } else {
                        FirebaseSetupScreen()
                    }
                }
            }
        }

        if (
            firebaseReady &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
