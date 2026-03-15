package com.methil.aiko

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.methil.aiko.data.TokenManager
import com.methil.aiko.ui.navigation.AikoNavigation
import com.methil.aiko.ui.theme.AikoTheme
import com.methil.aiko.ui.viewmodels.MainViewModel
import com.methil.aiko.ui.viewmodels.ProjectViewModelFactory
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private val mainViewModel: MainViewModel by viewModels { ProjectViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        setContent {
            val uiState by mainViewModel.uiState.collectAsState()
            
            AikoTheme {
                // Compose-based UI Dialog
                uiState.unlockResult?.let { result ->
                    AikoDialogFactory.InfoDialog(
                        title = "✨ NOUVEAU PERSONNAGE ✨",
                        message = "Félicitations ! Tu as débloqué ${result.characterName}. Tu peux maintenant discuter avec elle dans la liste des personnages.",
                        onDismiss = { mainViewModel.clearUnlockResult() }
                    )
                }

                AikoNavigation(mainViewModel = mainViewModel)
            }
        }
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == action
        ) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                val tagId = it.id.joinToString("") { byte -> "%02X".format(byte) }
                Log.d("NFC_SCANNER", "NFC Tag Scanned: $tagId")
                mainViewModel.unlockCharacter(tagId)
            }
        }
    }

    }
}
