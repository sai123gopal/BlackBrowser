package com.saigopal.browser.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saigopal.browser.ui.design.theme.BBSpace

data class OnboardingPrivacyPreferences(
    val adBlockEnabled: Boolean = true,
    val incognitoByDefault: Boolean = false,
    val highSecurityGuard: Boolean = true,
    val clearOnExit: Boolean = false,
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: (OnboardingPrivacyPreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentSlide by remember { mutableStateOf(0) }
    var privacyPrefs by remember { mutableStateOf(OnboardingPrivacyPreferences()) }

    var dragAccumulator by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D0F))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragAccumulator < -60f && currentSlide < 3) {
                            currentSlide++
                        } else if (dragAccumulator > 60f && currentSlide > 0) {
                            currentSlide--
                        }
                        dragAccumulator = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount
                    },
                )
            },
    ) {
        // Ambient background glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-right emerald ambient flare
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3520E398),
                        Color(0x1000897B),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.85f, h * 0.15f),
                    radius = w * 0.8f,
                )
            )

            // Bottom-left subtle glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x2000BFA5),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.1f, h * 0.85f),
                    radius = w * 0.6f,
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Top Bar: Brand & Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Black",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "Browser",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF20E398),
                    )
                }

                if (currentSlide < 3) {
                    TextButton(
                        onClick = { currentSlide = 3 },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF7E8B92)),
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            // Main Content Area (Animated Slides)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "OnboardingSlides",
                ) { slide ->
                    when (slide) {
                        0 -> SpeedSlide()
                        1 -> GesturesSlide()
                        2 -> DarkModeSlide()
                        3 -> PrivacySettingsSlide(
                            prefs = privacyPrefs,
                            onPrefsChange = { privacyPrefs = it },
                        )
                    }
                }
            }

            // Bottom Navigation & Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Slide Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(4) { index ->
                        val isActive = index == currentSlide
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isActive) 26.dp else 7.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isActive) Color(0xFF20E398)
                                    else Color(0xFF202B30)
                                ),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Action Button
                if (currentSlide < 3) {
                    Button(
                        onClick = { currentSlide++ },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF20E398),
                            contentColor = Color(0xFF090D0F),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "Continue",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { onFinish(privacyPrefs) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF20E398),
                            contentColor = Color(0xFF090D0F),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "Get Started & Explore",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Slide 1: Blazing Speed & Low-RAM Engineering
// -----------------------------------------------------------------------------
@Composable
private fun SpeedSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Graphic Illustration
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2.3f

                // Outer decorative dashed track
                drawCircle(
                    color = Color(0xFF162024),
                    radius = radius,
                    style = Stroke(width = 4f),
                )

                // Speedometer sweep arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0x2020E398),
                            Color(0xFF20E398),
                            Color(0xFF00E676),
                        ),
                        center = center,
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 10f, cap = StrokeCap.Round),
                )

                // Energy Core Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x5020E398), Color.Transparent),
                        center = center,
                        radius = radius * 0.7f,
                    ),
                )
            }

            Surface(
                shape = CircleShape,
                color = Color(0xFF10161A),
                border = BorderStroke(2.dp, Color(0xFF20E398)),
                modifier = Modifier.size(90.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Speed,
                        contentDescription = null,
                        tint = Color(0xFF20E398),
                        modifier = Modifier.size(46.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Engineered for Extreme Speed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Sub-millisecond startup with intelligent background tab suspension. Specifically tuned for 4GB RAM devices with zero sluggishness or battery drain.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8E9BA2),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}

// -----------------------------------------------------------------------------
// Slide 2: Fluid Gesture Navigation & Floating Dock
// -----------------------------------------------------------------------------
@Composable
private fun GesturesSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Graphic Illustration
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Background ripples
                drawCircle(
                    color = Color(0xFF131A1E),
                    radius = size.width * 0.44f,
                )
                drawCircle(
                    color = Color(0xFF182228),
                    radius = size.width * 0.32f,
                )

                // Swipe Arrow Paths
                val arrowPath = Path().apply {
                    moveTo(size.width * 0.22f, center.y)
                    lineTo(size.width * 0.78f, center.y)
                }
                drawPath(
                    path = arrowPath,
                    color = Color(0xFF20E398).copy(alpha = 0.6f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF10161A),
                    border = BorderStroke(1.5.dp, Color(0xFF20E398)),
                    modifier = Modifier
                        .width(180.dp)
                        .height(52.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "◀",
                            color = Color(0xFF20E398),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Swipe,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Swipe Bar",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "▶",
                            color = Color(0xFF20E398),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Fluid Gesture Navigation",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Effortlessly swipe left or right directly on the bottom dock to flip between tabs. As you scroll, the dock smoothly shrinks into a mini chip to maximize your reading view.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8E9BA2),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}

// -----------------------------------------------------------------------------
// Slide 3: Pure AMOLED Dark Mode
// -----------------------------------------------------------------------------
@Composable
private fun DarkModeSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Graphic Illustration
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Outer Moon Aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x4020E398), Color.Transparent),
                        center = center,
                        radius = size.width * 0.46f,
                    ),
                )

                // Crescent Moon Shape
                drawCircle(
                    color = Color(0xFF20E398),
                    radius = 48.dp.toPx(),
                    center = center,
                )
                drawCircle(
                    color = Color(0xFF090D0F),
                    radius = 42.dp.toPx(),
                    center = Offset(center.x + 18.dp.toPx(), center.y - 14.dp.toPx()),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "True Black AMOLED Mode",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Experience a stunning deep-black design tailored for OLED displays. Saves device battery life, reduces eye strain at night, and brings vibrant web colors to life.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8E9BA2),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}

// -----------------------------------------------------------------------------
// Slide 4: Privacy & Security Control (Interactive Options)
// -----------------------------------------------------------------------------
@Composable
private fun PrivacySettingsSlide(
    prefs: OnboardingPrivacyPreferences,
    onPrefsChange: (OnboardingPrivacyPreferences) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF131A1E),
            border = BorderStroke(1.5.dp, Color(0xFF20E398)),
            modifier = Modifier.size(62.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Security,
                    contentDescription = null,
                    tint = Color(0xFF20E398),
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Tailor Your Privacy Shield",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Customize your privacy baseline. You can adjust all preferences anytime in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8E9BA2),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(18.dp))

        // 1. Built-in AdBlocker Card
        PrivacyOptionCard(
            icon = Icons.Filled.Security,
            title = "AdBlocker & Tracker Shield",
            badge = "RECOMMENDED",
            description = "Blocks 50,000+ intrusive banner ads, popup overlays, and invasive analytics scripts for 3x faster loading.",
            checked = prefs.adBlockEnabled,
            onCheckedChange = { onPrefsChange(prefs.copy(adBlockEnabled = it)) },
        )

        Spacer(Modifier.height(10.dp))

        // 2. Incognito Private Mode Card
        PrivacyOptionCard(
            icon = Icons.Filled.VisibilityOff,
            title = "Incognito by Default",
            badge = "ZERO TRACE",
            description = "Start all new tabs in private mode. No browsing history, cookie traces, or local web cache will be stored on disk.",
            checked = prefs.incognitoByDefault,
            onCheckedChange = { onPrefsChange(prefs.copy(incognitoByDefault = it)) },
        )

        Spacer(Modifier.height(10.dp))

        // 3. High-Security Host Guard Card
        PrivacyOptionCard(
            icon = Icons.Filled.Lock,
            title = "High-Security Web Guard",
            badge = "MAX SECURITY",
            description = "Strict script sandboxing, anti-fingerprinting defenses, and universal host isolation against web threats.",
            checked = prefs.highSecurityGuard,
            onCheckedChange = { onPrefsChange(prefs.copy(highSecurityGuard = it)) },
        )

        Spacer(Modifier.height(10.dp))

        // 4. Clear History & Data on Exit Card
        PrivacyOptionCard(
            icon = Icons.Filled.DeleteSweep,
            title = "Clear Data on Exit",
            badge = "AUTO CLEAN",
            description = "Automatically purge your browsing history, web cache, and site cookies every time you leave the browser.",
            checked = prefs.clearOnExit,
            onCheckedChange = { onPrefsChange(prefs.copy(clearOnExit = it)) },
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PrivacyOptionCard(
    icon: ImageVector,
    title: String,
    badge: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF12181C),
        border = BorderStroke(
            1.dp,
            if (checked) Color(0xFF20E398).copy(alpha = 0.4f) else Color(0xFF20282C),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (checked) Color(0xFF20E398).copy(alpha = 0.15f) else Color(0xFF1B2226),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (checked) Color(0xFF20E398) else Color(0xFF7E8B92),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF20E398).copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF20E398),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7E8B92),
                    lineHeight = 16.sp,
                )
            }

            Spacer(Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF090D0F),
                    checkedTrackColor = Color(0xFF20E398),
                    uncheckedThumbColor = Color(0xFF7E8B92),
                    uncheckedTrackColor = Color(0xFF1B2226),
                    uncheckedBorderColor = Color(0xFF283238),
                ),
            )
        }
    }
}
