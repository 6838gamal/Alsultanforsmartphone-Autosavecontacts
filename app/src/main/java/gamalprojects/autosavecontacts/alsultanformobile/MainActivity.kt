package gamalprojects.autosavecontacts.alsultanformobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import gamalprojects.autosavecontacts.alsultanformobile.presentation.viewmodel.CrmViewModel
import gamalprojects.autosavecontacts.alsultanformobile.ui.MainScreen
import gamalprojects.autosavecontacts.alsultanformobile.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    private val crmViewModelItem: CrmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Supports full edge-to-edge drawing
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = crmViewModelItem)
            }
        }
    }
}
