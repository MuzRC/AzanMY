package com.musa.azanmy.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musa.azanmy.data.JakimZone
import com.musa.azanmy.data.JakimZoneData
import com.musa.azanmy.data.ZoneLocator
import com.musa.azanmy.util.PermissionHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZonePickerSheet(
    selectedZoneCode: String,
    onZoneSelected: (JakimZone) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalExtendedColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var isLocating by remember { mutableStateOf(false) }
    var gpsError by remember { mutableStateOf<String?>(null) }

    val filteredZones = remember(searchQuery) {
        JakimZoneData.search(searchQuery)
    }

    val groupedZones = remember(filteredZones) {
        filteredZones.groupBy { it.state }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            scope.launch {
                isLocating = true
                gpsError = null
                ZoneLocator.detectNearestZone(context)
                    .onSuccess { zone -> onZoneSelected(zone) }
                    .onFailure { err -> gpsError = err.message }
                isLocating = false
            }
        } else {
            gpsError = "Izin lokasi diperlukan untuk kesan zon automatik."
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .padding(horizontal = 18.dp)
        ) {
            Text(
                text = "Pilih Zon Waktu Solat JAKIM",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (PermissionHelper.hasLocationPermission(context)) {
                        scope.launch {
                            isLocating = true
                            gpsError = null
                            ZoneLocator.detectNearestZone(context)
                                .onSuccess { zone -> onZoneSelected(zone) }
                                .onFailure { err -> gpsError = err.message }
                            isLocating = false
                        }
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    }
                },
                enabled = !isLocating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ext.goldContainer,
                    contentColor = ext.goldBright
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ext.goldBright
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kesan Zon Automatik Guna GPS",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            if (gpsError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = gpsError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari daerah, negeri atau kod (cth: Beaufort, SBH09)") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Padam")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedZones.forEach { (stateName, zonesInState) ->
                    item(key = "header_$stateName") {
                        Text(
                            text = stateName.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ext.goldAccent,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }

                    items(zonesInState, key = { it.code }) { zone ->
                        val isSelected = zone.code.equals(selectedZoneCode, ignoreCase = true)
                        Surface(
                            onClick = { onZoneSelected(zone) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) ext.goldContainer else ext.cardBackground,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) ext.goldAccent else ext.cardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = zone.code,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = ext.goldBright
                                    )
                                    Text(
                                        text = zone.areas,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Dipilih",
                                        tint = BrightGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
