package com.casedhara.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.SplashThemeBackground
import com.casedhara.ui.components.design.GlassSurface
import java.io.File
import com.casedhara.ui.navigation.NavRoutes
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val profileImagePath by settingsViewModel.profileImagePath.collectAsStateWithLifecycle()

    val initialName = user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore("@") ?: ""
    val userEmail = user?.email ?: "Not signed in"

    var displayName by remember { mutableStateOf(initialName) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }

    var isSavingName by remember { mutableStateOf(false) }
    var isSavingPassword by remember { mutableStateOf(false) }
    var snackMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackMessage = null
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            val path = SettingsViewModel.saveProfileImage(context, uri)
            settingsViewModel.saveProfileImagePath(path)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) photoPicker.launch("image/*")
    }

    fun openPhotoPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            photoPicker.launch("image/*")
        } else {
            permissionLauncher.launch(permission)
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        SplashThemeBackground {
            AshokaChakraAnimation(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(0.15f),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {

            // ── Avatar ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box {
                    val imagePath = profileImagePath
                    if (!imagePath.isNullOrBlank() && File(imagePath).exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imagePath)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(96.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    // Camera badge
                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .clickable { openPhotoPicker() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Change photo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // ── Display Name ────────────────────────────────────────────────
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                glowColor = MaterialTheme.colorScheme.primary,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Display Name",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Your name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )
                    Button(
                        onClick = {
                            if (displayName.isBlank()) return@Button
                            isSavingName = true
                            val req = UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName.trim())
                                .build()
                            user?.updateProfile(req)?.addOnCompleteListener { task ->
                                isSavingName = false
                                snackMessage = if (task.isSuccessful) "Name updated successfully"
                                else task.exception?.message ?: "Failed to update name"
                            }
                        },
                        enabled = !isSavingName,
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        if (isSavingName) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save Name")
                        }
                    }
                }
            }

            // ── Email (read-only) ───────────────────────────────────────────
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                glowColor = MaterialTheme.colorScheme.primary,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            "Email",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // ── Change Password ─────────────────────────────────────────────
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                glowColor = MaterialTheme.colorScheme.primary,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Change Password",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Current password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                Icon(
                                    if (showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (showCurrentPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("New password") },
                        leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                Icon(
                                    if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (showNewPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(10.dp),
                    )
                    Button(
                        onClick = {
                            if (currentPassword.isBlank() || newPassword.isBlank()) {
                                snackMessage = "Please fill both password fields"
                                return@Button
                            }
                            if (newPassword.length < 6) {
                                snackMessage = "New password must be at least 6 characters"
                                return@Button
                            }
                            isSavingPassword = true
                            val email = user?.email ?: return@Button
                            val credential = EmailAuthProvider.getCredential(email, currentPassword)
                            user.reauthenticate(credential).addOnCompleteListener { reauth ->
                                if (reauth.isSuccessful) {
                                    user.updatePassword(newPassword).addOnCompleteListener { update ->
                                        isSavingPassword = false
                                        snackMessage = if (update.isSuccessful) {
                                            currentPassword = ""
                                            newPassword = ""
                                            "Password changed successfully"
                                        } else update.exception?.message ?: "Failed to change password"
                                    }
                                } else {
                                    isSavingPassword = false
                                    snackMessage = "Current password is incorrect"
                                }
                            }
                        },
                        enabled = !isSavingPassword,
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        if (isSavingPassword) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Update Password")
                        }
                    }
                }
            }

            // ── Logout ──────────────────────────────────────────────────────
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Logout")
            }

            Spacer(Modifier.height(8.dp))
            }
        }
    }
}
