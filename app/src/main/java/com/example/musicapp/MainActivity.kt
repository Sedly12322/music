package com.example.musicapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.example.musicapp.ui.MusicAppScaffold
import com.example.musicapp.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    // ViewModel inicializujeme zde, abychom k němu měli přístup v callbacku oprávnění
    private lateinit var viewModel: MusicViewModel

    // Callback, který se zavolá, když uživatel odklikne oprávnění
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Zkontrolujeme, zda jsou klíčová oprávnění udělena
        val readAudioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] ?: false
        val readImagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        val readExternalGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

        if (readAudioGranted || readExternalGranted) {
            // Pokud máme práva na čtení, znovu načteme celou knihovnu
            // Teď už by se měly načíst i ty cover.jpg obrázky
            viewModel.loadLibrary()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Inicializace ViewModelu
        viewModel = ViewModelProvider(this)[MusicViewModel::class.java]

        // Příprava seznamu potřebných oprávnění
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        // Pokud něco chybí, požádáme o to
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                // ViewModel předáváme dál
                MusicAppScaffold(viewModel)
            }
        }
    }
}