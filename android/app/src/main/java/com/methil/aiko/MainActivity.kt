package com.methil.aiko

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.methil.aiko.bridge.AikoConfig
import com.methil.aiko.data.TokenManager
import com.methil.aiko.ui.navigation.AikoNavigation
import com.methil.aiko.ui.theme.AikoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        setContent {
            AikoTheme {
                AikoNavigation()
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
                unlockCharacter(tagId)
            }
        }
    }

    private fun unlockCharacter(tagId: String) {
        val token = TokenManager(this).getToken() ?: return
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val jsonBody = """{"nfc_tag": "$tagId"}"""
                val body = RequestBody.create("application/json".toMediaType(), jsonBody)
                
                val request = Request.Builder()
                    .url("${AikoConfig.BASE_URL}/characters/unlock")
                    .post(body)
                    .header("Authorization", "Bearer $token")
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("NFC_SCANNER", "Character unlocked successfully: $responseBody")
                } else {
                    val errorBody = response.body?.string()
                    Log.e("NFC_SCANNER", "Failed to unlock character: ${response.code} $errorBody")
                }
            } catch (e: Exception) {
                Log.e("NFC_SCANNER", "Error unlocking character", e)
            }
        }
    }
}
