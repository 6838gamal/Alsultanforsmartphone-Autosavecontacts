package gamalprojects.autosavecontacts.alsultanformobile.ui

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamalprojects.autosavecontacts.alsultanformobile.data.database.CrmContactEntity
import gamalprojects.autosavecontacts.alsultanformobile.presentation.viewmodel.CrmViewModel
import gamalprojects.autosavecontacts.alsultanformobile.ui.theme.*
import gamalprojects.autosavecontacts.alsultanformobile.utils.LogLevel
import gamalprojects.autosavecontacts.alsultanformobile.utils.PermissionHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("الرئيسية", Icons.Filled.Dashboard),
    CONTACTS("العملاء", Icons.Filled.People),
    LOGGER("سجل العمليات", Icons.Filled.List),
    EXPORT("التصدير", Icons.Filled.Share),
    SETTINGS("الإعدادات", Icons.Filled.Settings)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(viewModel: CrmViewModel) {
    val context = LocalContext.current
    var isSplashActive by remember { mutableStateOf(true) }

    // State flows from ViewModel
    val contacts by viewModel.contactsFlow.collectAsStateWithLifecycle()
    val logs by viewModel.logsFlow.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunningFlow.collectAsStateWithLifecycle()
    val totalSavedContacts by viewModel.contactCountFlow.collectAsStateWithLifecycle()

    // 1. Splash Screen Auto-timer
    LaunchedEffect(Unit) {
        delay(2200) // Show splash for 2.2 seconds
        isSplashActive = false
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = SultanBg,
            surface = SultanSurface,
            primary = SultanGold,
            onPrimary = Color.Black,
            secondary = SultanCardBg,
            error = SultanRed
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SultanBg)
        ) {
            if (isSplashActive) {
                SplashScreen()
            } else {
                MainContent(
                    viewModel = viewModel,
                    contacts = contacts,
                    logs = logs,
                    isServiceRunning = isServiceRunning,
                    totalSavedContacts = totalSavedContacts
                )
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashGlow")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SultanBg, SultanSurface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sultan Royal Monogram / Logo Shape
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale)
                    .shadow(16.dp, CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(SultanGoldVariant, SultanBg)
                        ),
                        shape = CircleShape
                    )
                    .border(2.dp, SultanGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Monogram Logo",
                    tint = SultanGold,
                    modifier = Modifier.size(65.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "ALSULTAN",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = SultanGold,
                    letterSpacing = 4.sp
                )
            )
            
            Text(
                text = "AutoSave Contacts CRM",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SultanTextLight
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "نظام التلقائي المنظم لبناء قاعدة البيانات",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SultanTextMuted
                )
            )

            Spacer(modifier = Modifier.height(50.dp))

            CircularProgressIndicator(
                color = SultanGold,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun MainContent(
    viewModel: CrmViewModel,
    contacts: List<CrmContactEntity>,
    logs: List<gamalprojects.autosavecontacts.alsultanformobile.utils.LogEntry>,
    isServiceRunning: Boolean,
    totalSavedContacts: Int
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AppTab.DASHBOARD) }

    // Multi-Permission Launcher Dialog
    val permissionsToRequest = arrayOf(
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.WRITE_CONTACTS,
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.READ_CALL_LOG,
        android.Manifest.permission.READ_PHONE_STATE
    )

    var hasStandardPermissions by remember {
        mutableStateOf(
            PermissionHelper.hasContactsPermissions(context) &&
                    PermissionHelper.hasSmsPermissions(context) &&
                    PermissionHelper.hasCallsPermissions(context)
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
        val granted = resultMap.values.all { it }
        hasStandardPermissions = granted
        if (granted) {
            Toast.makeText(context, "تم منح جميع الصلاحيات اللازمة بنجاح!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "تنبيه: يجب تفعيل الصلاحيات من الإعدادات للعمل بكفاءة.", Toast.LENGTH_LONG).show()
        }
    }

    // Auto-check permissions on view load
    LaunchedEffect(Unit) {
        hasStandardPermissions = PermissionHelper.hasContactsPermissions(context) &&
                PermissionHelper.hasSmsPermissions(context) &&
                PermissionHelper.hasCallsPermissions(context)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = SultanSurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (selectedTab == tab) SultanGold else SultanTextMuted
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == tab) SultanGold else SultanTextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = SultanCardBg
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SultanBg)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SultanSurface)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo & Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SultanGold, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoMode,
                            contentDescription = "CRM Logo",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Alsultan CRM",
                            fontWeight = FontWeight.Bold,
                            color = SultanTextLight,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "AutoSave Contacts Setup",
                            color = SultanGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Active Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (isServiceRunning) SultanGreen.copy(0.12f) else SultanRed.copy(0.12f),
                            RoundedCornerShape(50.dp)
                        )
                        .border(
                            1.dp,
                            if (isServiceRunning) SultanGreen.copy(0.3f) else SultanRed.copy(0.3f),
                            CircleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (isServiceRunning) SultanGreen else SultanRed,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isServiceRunning) "نشط" else "متوقف",
                        color = if (isServiceRunning) SultanGreen else SultanRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tab Switching Animation Container - Made direct children of ColumnScope for compile-safety
            AnimatedVisibility(
                visible = selectedTab == AppTab.DASHBOARD,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    DashboardTab(
                        viewModel = viewModel,
                        isServiceRunning = isServiceRunning,
                        totalSaved = totalSavedContacts,
                        contacts = contacts,
                        hasStandardPermissions = hasStandardPermissions,
                        onRequestPermissions = { launcher.launch(permissionsToRequest) }
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedTab == AppTab.CONTACTS,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ContactsTab(
                        viewModel = viewModel,
                        contacts = contacts
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedTab == AppTab.LOGGER,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LoggerTab(
                        viewModel = viewModel,
                        logs = logs
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedTab == AppTab.EXPORT,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ExportTab(
                        viewModel = viewModel,
                        contacts = contacts
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedTab == AppTab.SETTINGS,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SettingsTab(
                        viewModel = viewModel,
                        isServiceRunning = isServiceRunning
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: CrmViewModel,
    isServiceRunning: Boolean,
    totalSaved: Int,
    contacts: List<CrmContactEntity>,
    hasStandardPermissions: Boolean,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val lastSavedContact = contacts.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Status Title Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SultanSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SultanGold.copy(0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "مرحباً بك في لوحة تحكم السلطان",
                        fontWeight = FontWeight.Bold,
                        color = SultanGold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يتتبع هذا التطبيق المكالمات الواردة، رسائل الـ SMS، وإشعارات الواتساب غير المسجلة محلياً ويقوم بحفظها تلقائياً لتأسيس قاعدة بيانات العملاء.",
                        color = SultanTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                }
            }
        }

        // Stats Counter card
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SultanCardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "إجمالي المحفوظين",
                            color = SultanTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$totalSaved",
                            color = SultanGold,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SultanCardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "نشاط المراقبة",
                            color = SultanTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            imageVector = if (isServiceRunning) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            contentDescription = "Status",
                            tint = if (isServiceRunning) SultanGreen else SultanRed,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Control Toggles Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SultanSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "التحكم في الخدمة الخلفية",
                        fontWeight = FontWeight.Bold,
                        color = SultanTextLight,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startCrmService(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = SultanGreen),
                            enabled = !isServiceRunning,
                            modifier = Modifier.weight(1.0f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Start")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تفعيل الخدمة", color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.stopCrmService(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = SultanRed),
                            enabled = isServiceRunning,
                            modifier = Modifier.weight(1.0f)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "Stop")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إيقاف الخدمة", color = Color.White)
                        }
                    }
                }
            }
        }

        // Permissions Status Overview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SultanSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حالة الصلاحيات والوصول",
                            fontWeight = FontWeight.Bold,
                            color = SultanTextLight
                        )
                        if (!hasStandardPermissions) {
                            Button(
                                onClick = onRequestPermissions,
                                colors = ButtonDefaults.buttonColors(containerColor = SultanGold),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("طلب التفعيل", fontSize = 11.sp, color = Color.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Android runtime permissions
                    PermissionBadge(
                        label = "صلاحية جهات الاتصال (قراءة وكتابة)",
                        isGranted = PermissionHelper.hasContactsPermissions(context)
                    )
                    PermissionBadge(
                        label = "صلاحيات مكالمات الهاتف (Caller ID)",
                        isGranted = PermissionHelper.hasCallsPermissions(context)
                    )
                    PermissionBadge(
                        label = "صلاحية استلام رسائل الـ SMS",
                        isGranted = PermissionHelper.hasSmsPermissions(context)
                    )

                    // 2. Specialty permissions
                    PermissionBadge(
                        label = "خدمة قراءة الإشعارات (WhatsApp)",
                        isGranted = PermissionHelper.isNotificationListenerEnabled(context)
                    )
                    PermissionBadge(
                        label = "خدمة المساعدة وإمكانية الوصول",
                        isGranted = PermissionHelper.isAccessibilityServiceEnabled(context)
                    )
                }
            }
        }

        // Last Saved Contact Preview
        item {
            if (lastSavedContact != null) {
                Column {
                    Text(
                        text = "آخر عميل تم رصده وحفظه بكفاءة",
                        fontWeight = FontWeight.Bold,
                        color = SultanTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.Right
                    )
                    ContactCardItem(
                        contact = lastSavedContact,
                        onRename = { /* Handled in tab */ },
                        onDelete = { /* Handled in tab */ },
                        showActions = false
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SultanSurface.copy(0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.ImportContacts,
                                contentDescription = "",
                                tint = SultanTextMuted.copy(0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "لم يتم رصد أي عملاء جدد بعد.",
                                color = SultanTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionBadge(label: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = if (isGranted) SultanGreen else SultanRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = SultanTextLight, fontSize = 11.sp)
        }
        Text(
            text = if (isGranted) "مفعلة" else "معطلة",
            color = if (isGranted) SultanGreen else SultanRed,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContactsTab(viewModel: CrmViewModel, contacts: List<CrmContactEntity>) {
    val context = LocalContext.current
    var editingContact by remember { mutableStateOf<CrmContactEntity?>(null) }
    var renameValue by remember { mutableStateOf("") }

    var deletingContact by remember { mutableStateOf<CrmContactEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (contacts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonSearch,
                    contentDescription = "",
                    modifier = Modifier.size(80.dp),
                    tint = SultanTextMuted.copy(0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "قاعدة بيانات العملاء فارغة",
                    color = SultanTextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "عندما يتصل رقم غير مسجل أو يتواصل بك عميل، سيظهر هنا تلقائياً.",
                    color = SultanTextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = "سجل العملاء التلقائي (${contacts.size})",
                    fontWeight = FontWeight.Bold,
                    color = SultanGold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Right
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(contacts) { item ->
                        ContactCardItem(
                            contact = item,
                            onRename = {
                                editingContact = item
                                renameValue = item.displayName
                            },
                            onDelete = { deletingContact = item },
                            showActions = true
                        )
                    }
                }
            }
        }

        // Rename Alert Dialog
        if (editingContact != null) {
            AlertDialog(
                onDismissRequest = { editingContact = null },
                title = { Text("تعديل اسم العميل", color = SultanGold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                text = {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("سيتم تغيير الاسم في التطبيق وفي جهات اتصال الهاتف كذلك:", color = SultanTextLight, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedTextField(
                            value = renameValue,
                            onValueChange = { renameValue = it },
                            placeholder = { Text("مثال: عميل السلطان المتميز") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SultanGold,
                                cursorColor = SultanGold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = SultanGold),
                        onClick = {
                            editingContact?.let {
                                viewModel.updateContactName(it, renameValue, context)
                            }
                            editingContact = null
                        }
                    ) {
                        Text("حفظ التعديل", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingContact = null }) {
                        Text("إلغاء لالغاء", color = SultanTextMuted)
                    }
                }
            )
        }

        // Delete Alert Dialog with Choice Options
        if (deletingContact != null) {
            AlertDialog(
                onDismissRequest = { deletingContact = null },
                title = { Text("حذف السجل والخيار اليدوي", color = SultanRed, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                text = {
                    Text(
                        text = "اختر كيفية مسح سجل العميل ${deletingContact?.displayName}:",
                        color = SultanTextLight,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = SultanRed),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                deletingContact?.let {
                                    viewModel.deleteContact(it, deleteFromSystem = true, context = context)
                                }
                                deletingContact = null
                            }
                        ) {
                            Text("حذفه من التطبيق ومن جهات اتصال الهاتف", color = Color.White)
                        }

                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = SultanCardBg),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                deletingContact?.let {
                                    viewModel.deleteContact(it, deleteFromSystem = false, context = context)
                                }
                                deletingContact = null
                            }
                        ) {
                            Text("حذفه من الـ CRM فقط (إبقاؤه بالهاتف)", color = SultanTextLight)
                        }

                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { deletingContact = null }
                        ) {
                            Text("تراجع وإلغاء", color = SultanTextMuted)
                        }
                    }
                },
                dismissButton = {} // confirming everything inside confirming column
            )
        }
    }
}

@Composable
fun ContactCardItem(
    contact: CrmContactEntity,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    showActions: Boolean
) {
    val dateText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(contact.lastActivityTimestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = SultanSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source Label Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val iconSource = when (contact.source) {
                        "Call" -> Icons.Filled.Call
                        "SMS" -> Icons.Filled.Sms
                        "WhatsApp" -> Icons.Filled.ChatBubble
                        else -> Icons.Filled.Person
                    }
                    val iconColor = when (contact.source) {
                        "Call" -> SultanGold
                        "SMS" -> PurpleGrey80
                        "WhatsApp" -> SultanGreen
                        else -> SultanTextMuted
                    }
                    val sourceText = when (contact.source) {
                        "Call" -> "مكالمة"
                        "SMS" -> "رسالة SMS"
                        "WhatsApp" -> "واتساب"
                        else -> contact.source
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(iconColor.copy(0.12f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconSource,
                            contentDescription = "",
                            tint = iconColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sourceText,
                        color = iconColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Saved/Failed Status check
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (contact.status == "Saved") SultanGreen else SultanRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (contact.status == "Saved") "تم الحفظ بالهاتف" else "فشل الحفظ التلقائي",
                        color = if (contact.status == "Saved") SultanGreen else SultanRed,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Contact Name and phone row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = contact.displayName,
                        fontWeight = FontWeight.Bold,
                        color = SultanTextLight,
                        fontSize = 15.sp
                    )
                    Text(
                        text = contact.phoneNumber,
                        color = SultanTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Stats: Communication Count
                Box(
                    modifier = Modifier
                        .background(SultanCardBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "تواصل: ${contact.communicationCount} مرات",
                        fontSize = 11.sp,
                        color = SultanGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Divider(color = SultanCardBg.copy(0.3f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(6.dp))

            // Time and action links
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر تفاعل: $dateText",
                    color = SultanTextMuted,
                    fontSize = 11.sp
                )

                if (showActions) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = SultanGold,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onRename() }
                        )

                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = SultanRed,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoggerTab(viewModel: CrmViewModel, logs: List<gamalprojects.autosavecontacts.alsultanformobile.utils.LogEntry>) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = SultanCardBg),
                onClick = { viewModel.clearUiLogs() },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Filled.ClearAll, contentDescription = "", modifier = Modifier.size(14.dp), tint = SultanRed)
                Spacer(modifier = Modifier.width(4.dp))
                Text("مسح السجل", fontSize = 11.sp, color = SultanRed)
            }

            Text(
                text = "سجل العمليات الخلفية (Logs)",
                fontWeight = FontWeight.Bold,
                color = SultanGold,
                fontSize = 14.sp
            )
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "لا توجد عمليات جارية في الخلفية حالياً.",
                    color = SultanTextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(SultanSurface, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { entry ->
                    val colorLevel = when (entry.level) {
                        LogLevel.SUCCESS -> SultanGreen
                        LogLevel.ERROR -> SultanRed
                        LogLevel.WARNING -> SultanGold
                        LogLevel.INFO -> SultanTextMuted
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString("${entry.formattedTime}: ${entry.message}"))
                                Toast.makeText(context, "تم نسخ السجل للحافظة", Toast.LENGTH_SHORT).show()
                            }
                            .border(1.dp, SultanCardBg.copy(0.4f), RoundedCornerShape(6.dp))
                            .background(SultanBg.copy(0.3f))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "[${entry.tag}]",
                                color = colorLevel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = entry.formattedTime,
                                color = SultanTextMuted,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = entry.message,
                            color = SultanTextLight,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportTab(viewModel: CrmViewModel, contacts: List<CrmContactEntity>) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Icon(
            imageVector = Icons.Filled.CloudDownload,
            contentDescription = "",
            modifier = Modifier.size(90.dp),
            tint = SultanGold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "تصدير قاعدة بيانات العملاء CRM",
            fontWeight = FontWeight.Bold,
            color = SultanTextLight,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "قم بتصدير أرقام العملاء الذين تم التقاطهم وحفظهم تلقائياً إلى ملفات منظمة لمشاركتها أو حفظها بجهازك.",
            color = SultanTextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SultanSurface),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.End) {
                Text(
                    text = "سجلات للتصدير حالياً: ${contacts.size} عميل متاح",
                    fontWeight = FontWeight.Bold,
                    color = SultanTextLight,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Excel Export Button
                Button(
                    onClick = { viewModel.exportToXls(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = SultanGold),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.GridOn, contentDescription = "", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير كـ Excel (XLSX/XML)", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // CSV Export Button
                Button(
                    onClick = { viewModel.exportToCsv(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = SultanCardBg),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Description, contentDescription = "")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير كـ CSV (للأنظمة الأخرى)", color = SultanTextLight)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "ملاحظة: يتم تصدير ملفات متوافقة بالكامل مع برمجيات Microsoft Excel والترميز العربي لمنع حدوث التشوّه في نصوص الأسماء.",
            color = SultanGold.copy(0.8f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

@Composable
fun SettingsTab(viewModel: CrmViewModel, isServiceRunning: Boolean) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "ضبط خدمات النظام الاستثنائية",
                fontWeight = FontWeight.Bold,
                color = SultanGold,
                fontSize = 15.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Right
            )
        }

        // WhatsApp / Notification Listener Access Guide Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SultanSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. التقاط اتصالات WhatsApp (مهم)",
                        fontWeight = FontWeight.Bold,
                        color = SultanTextLight,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "لمراقبة مكالمات ورسائل الواتساب، يتطلب النظام 'صلاحية قراءة إشعارات في أندرويد'. اضغط على الزر أدناه ثم حدّد تطبيقنا 'AutoSave CRM' وقم بتفعيله.",
                        color = SultanTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isWAEnabled = PermissionHelper.isNotificationListenerEnabled(context)
                    Button(
                        onClick = {
                            try {
                                context.startActivity(PermissionHelper.getNotificationListenerSettingsIntent())
                            } catch (e: Exception) {
                                Toast.makeText(context, "لم نتمكن من فتح الإعدادات تلقائياً. ابحث عن 'الوصول للإشعارات' بهاتفك.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isWAEnabled) SultanCardBg else SultanGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isWAEnabled) "إذن قراءة إشعارات WhatsApp مفعل ✓" else "تفعيل إذن قراءة إشعارات WhatsApp",
                            color = if (isWAEnabled) SultanGreen else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Accessibility Service Access Guide Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SultanSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. خدمة المساعدة (Accessibility Service)",
                        fontWeight = FontWeight.Bold,
                        color = SultanTextLight,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تتيح هذه الخدمة المراقبة اللامتناهية للأرقام غير المحفوظة عند فتح شاشات المكالمات أو إشعارات واتساب السريعة.",
                        color = SultanTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isAccessEnabled = PermissionHelper.isAccessibilityServiceEnabled(context)
                    Button(
                        onClick = {
                            try {
                                context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                            } catch (e: Exception) {
                                Toast.makeText(context, "الرجاء الذهاب لـ: الإعدادات > إمكانية الوصول وتفعيل الخدمة يدوياً.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAccessEnabled) SultanCardBg else SultanGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isAccessEnabled) "خدمة إمكانية الوصول مفعلة ✓" else "تفعيل خدمة إمكانية الوصول",
                            color = if (isAccessEnabled) SultanGreen else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Battery Optimization
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SultanSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. العمل دون انقطاع (تحسين البطارية)",
                        fontWeight = FontWeight.Bold,
                        color = SultanTextLight,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "قد يوقف نظام أندرويد خدمة حفظ جهات الاتصال في الخلفية لتوفير شحن البطارية. يجب إلغاء تحسين البطارية للتمتع بمراقبة خلفية دائمة.",
                        color = SultanTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isBatteryExempt = PermissionHelper.isBatteryOptimizationIgnored(context)
                    Button(
                        onClick = {
                            try {
                                context.startActivity(PermissionHelper.getRequestBatteryOptimizationIntent(context))
                            } catch (e: Exception) {
                                Toast.makeText(context, "تحسين البطارية غير مدعوم أو تم تفعيله مسبقاً بهاتفك.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBatteryExempt) SultanCardBg else SultanGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isBatteryExempt) "تحسين البطارية معطل بالكامل ✓" else "تعطيل تحسين البطارية للتطبيق",
                            color = if (isBatteryExempt) SultanGreen else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Clear log option
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SultanSurface.copy(0.6f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Text(
                        "خطر: حذف قاعدة البيانات",
                        color = SultanRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "تنبيه: سيؤدي هذا الإجراء إلى مسح سجلات تطبيق Alsultan CRM بالكامل من الذاكرة الداخلية. لن يقوم بمسح أرقام الهاتف المحفوظة بجهات اتصال هاتفك.",
                        fontSize = 11.sp,
                        color = SultanTextMuted,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            viewModel.clearAllContactsLog()
                            Toast.makeText(context, "تم تصفير سجلات CRM.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = SultanRed)
                    ) {
                        Text("تهيئة السجلات ومسحها", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
