package com.example.ui.screens.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppLanguage
import com.example.ui.components.IosTopBar
import com.example.util.Strings

@Composable
fun CreateModeSelectionScreen(
    language: AppLanguage,
    onBackClick: () -> Unit,
    onSelectFastMode: () -> Unit,
    onSelectExpertMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("create_mode_selection_screen")
    ) {
        IosTopBar(
            title = Strings.get("create_mode_selection_title", language),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header / Subtitle Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = Strings.get("create_mode_selection_subtitle", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (language == AppLanguage.TURKISH) 
                        "İhtiyacınıza uygun olan yöntemi seçerek uygulamanızı saniyeler içinde oluşturun." 
                    else 
                        "Select the workflow that best fits your goals to generate your Android APK.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Mode 1: FAST MODE CARD
            ModeSelectionCard(
                title = Strings.get("mode_fast_title", language),
                badgeText = Strings.get("mode_fast_badge", language),
                badgeColor = Color(0xFF34C759),
                description = Strings.get("mode_fast_desc", language),
                icon = Icons.Default.Bolt,
                iconBackgroundColor = Color(0xFF007AFF),
                highlights = listOf(
                    if (language == AppLanguage.TURKISH) "Hızlı ve sade giriş formu" else "Simplified input form",
                    if (language == AppLanguage.TURKISH) "HTML, URL veya ZIP kaynak desteği" else "HTML, URL, or ZIP archive source",
                    if (language == AppLanguage.TURKISH) "Otomatik paket ve derleme ayarları" else "Automated package & build configuration",
                    if (language == AppLanguage.TURKISH) "Tek tıkla APK derleme" else "One-tap APK compilation"
                ),
                onClick = onSelectFastMode,
                testTag = "btn_mode_fast"
            )

            // Mode 2: EXPERT MODE CARD
            ModeSelectionCard(
                title = Strings.get("mode_expert_title", language),
                badgeText = Strings.get("mode_expert_badge", language),
                badgeColor = Color(0xFF5856D6),
                description = Strings.get("mode_expert_desc", language),
                icon = Icons.Default.Tune,
                iconBackgroundColor = Color(0xFF5856D6),
                highlights = listOf(
                    if (language == AppLanguage.TURKISH) "Özel Paket Kimliği ve Sürüm yönetimi" else "Custom Package ID & Versioning",
                    if (language == AppLanguage.TURKISH) "Uygulama simgesi ve görünüm özelleştirme" else "App icon & display customization",
                    if (language == AppLanguage.TURKISH) "JavaScript, Depolama ve Çevrimdışı önbellek" else "JavaScript, Storage & Offline caching",
                    if (language == AppLanguage.TURKISH) "Tam kontrol ve gelişmiş parametreler" else "Full control & advanced parameters"
                ),
                onClick = onSelectExpertMode,
                testTag = "btn_mode_expert"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ModeSelectionCard(
    title: String,
    badgeText: String,
    badgeColor: Color,
    description: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    highlights: List<String>,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(18.dp)
            .testTag(testTag)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        // Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Highlights
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                highlights.forEach { highlight ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = highlight,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }
    }
}
