package com.andorid.ninmods.ui.setup

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andorid.ninmods.R
import com.andorid.ninmods.data.DriverRepository
import com.andorid.ninmods.ui.theme.APatchTheme
import com.andorid.ninmods.util.RootDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { DriverRepository(context) }

    var rootManager by remember { mutableStateOf("Detecting...") }
    var isSuperUserGranted by remember { mutableStateOf(false) }
    var isStorageGranted by remember { mutableStateOf(false) }
    var isInstalling by remember { mutableStateOf(false) }
    var installStatus by remember { mutableStateOf("Preparing environment") }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it } || permissions.isEmpty()
        isStorageGranted = true
    }

    LaunchedEffect(Unit) {
        val info = withContext(Dispatchers.IO) {
            RootDetector.detect(context)
        }
        rootManager = info.managerName

        val granted = withContext(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                val output = process.inputStream.bufferedReader().readText()
                output.contains("uid=0")
            } catch (e: Exception) {
                false
            }
        }
        isSuperUserGranted = granted

        storagePermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }

    LaunchedEffect(isSuperUserGranted, isStorageGranted) {
        if (isSuperUserGranted && isStorageGranted && !isInstalling) {
            isInstalling = true
            scope.launch(Dispatchers.IO) {
                repository.extractDriversIfNeeded { progress ->
                    installStatus = progress
                }
                withContext(Dispatchers.Main) {
                    onSetupComplete()
                }
            }
        }
    }

    APatchTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(76.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_n),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "NinMods Setup",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(28.dp))

                PermissionStatusCard(
                    icon = Icons.Default.Security,
                    title = "Root Manager",
                    status = "Detected: $rootManager",
                    isGranted = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionStatusCard(
                    icon = Icons.Default.VpnKey,
                    title = "SuperUser Access",
                    status = if (isSuperUserGranted) "Granted (uid=0)" else "Requesting SuperUser...",
                    isGranted = isSuperUserGranted
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionStatusCard(
                    icon = Icons.Default.Folder,
                    title = "Storage Access",
                    status = if (isStorageGranted) "Permission Granted" else "Awaiting Permission...",
                    isGranted = isStorageGranted
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isInstalling) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Install",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        PulsingDotsAnimation()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = installStatus,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!isSuperUserGranted) {
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                                    val output = process.inputStream.bufferedReader().readText()
                                    if (output.contains("uid=0")) {
                                        withContext(Dispatchers.Main) {
                                            isSuperUserGranted = true
                                        }
                                    }
                                } catch (e: Exception) {
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Authorize SuperUser",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String,
    isGranted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGranted) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Done",
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PulsingDotsAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "DotsPulse")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Dot3"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(alpha1)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(alpha2)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(alpha3)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
