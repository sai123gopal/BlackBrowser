package com.saigopal.browser.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TutorialOverlay(
    onDismiss: () -> Unit,
    onOpenQuickSite: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.84f))
            .clickable(onClick = { /* absorb clicks */ }),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            // Top Bar: Tutorial Header & Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF20E398).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF20E398).copy(alpha = 0.3f)),
                ) {
                    Text(
                        text = "FEATURE TUTORIAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontSize = 10.sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF20E398),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8E9BA2)),
                ) {
                    Text(
                        text = "Skip Tutorial",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Skip",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Animated Tutorial Card
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TutorialSteps",
            ) { currentStep ->
                when (currentStep) {
                    0 -> Step1PinnedSites(
                        pulseScale = pulseScale,
                        onOpenAmazon = {
                            onOpenQuickSite("https://www.amazon.com/")
                            step = 1
                        },
                        onNext = { step = 1 },
                    )
                    1 -> Step2SwipeTabs(
                        pulseScale = pulseScale,
                        onNext = { step = 2 },
                        onBack = { step = 0 },
                    )
                    2 -> Step3CollapsibleDock(
                        pulseScale = pulseScale,
                        onFinish = onDismiss,
                        onBack = { step = 1 },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

// -----------------------------------------------------------------------------
// Step 1: Open Pinned Tab Quickly (e.g. Amazon)
// -----------------------------------------------------------------------------
@Composable
private fun Step1PinnedSites(
    pulseScale: Float,
    onOpenAmazon: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF12181C),
        border = BorderStroke(1.5.dp, Color(0xFF20E398).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Step Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF20E398),
                    modifier = Modifier.size(20.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "1",
                            color = Color(0xFF090D0F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = "STEP 1 OF 3: PINNED SITES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20E398),
                )
            }

            Spacer(Modifier.height(14.dp))

            // Highlight Target Graphic (Amazon Tile spotlight)
            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .size(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF232F3E),
                    border = BorderStroke(2.dp, Color(0xFF20E398)),
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "a",
                                color = Color(0xFFFF9900),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Amazon",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Instant Quick-Launch Sites",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Tap any pinned tile like Amazon to open your favorite websites instantly with zero typing. Long-press any site to edit, customize or rearrange.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E9BA2),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onOpenAmazon,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF20E398),
                        contentColor = Color(0xFF090D0F),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open Amazon", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                TextButton(
                    onClick = onNext,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("Next Tip →", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Step 2: Swipe Left / Right to Switch Tabs
// -----------------------------------------------------------------------------
@Composable
private fun Step2SwipeTabs(
    pulseScale: Float,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF12181C),
        border = BorderStroke(1.5.dp, Color(0xFF20E398).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Step Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF20E398),
                    modifier = Modifier.size(20.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "2",
                            color = Color(0xFF090D0F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = "STEP 2 OF 3: SWIPE TO SWITCH TABS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20E398),
                )
            }

            Spacer(Modifier.height(18.dp))

            // Animated Gesture Pill
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF182228),
                border = BorderStroke(1.5.dp, Color(0xFF20E398)),
                modifier = Modifier
                    .scale(pulseScale)
                    .width(220.dp)
                    .height(50.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "⟵ Swipe",
                        color = Color(0xFF20E398),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        Icons.Filled.Swipe,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Swipe ⟶",
                        color = Color(0xFF20E398),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Effortless Tab Gestures",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Swipe left or right directly across the bottom floating dock to glide between your open tabs without opening the tab switcher.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E9BA2),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF7E8B92)),
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("← Back", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF20E398),
                        contentColor = Color(0xFF090D0F),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Next: Collapsible Dock", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Step 3: Swipe Up / Down (Collapsible Bottom Dock)
// -----------------------------------------------------------------------------
@Composable
private fun Step3CollapsibleDock(
    pulseScale: Float,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF12181C),
        border = BorderStroke(1.5.dp, Color(0xFF20E398).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Step Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF20E398),
                    modifier = Modifier.size(20.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "3",
                            color = Color(0xFF090D0F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = "STEP 3 OF 3: COLLAPSIBLE DOCK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20E398),
                )
            }

            Spacer(Modifier.height(18.dp))

            // Animated Vertical Scroll Graphic
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Collapsed Chip Demo
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF182228),
                    border = BorderStroke(1.5.dp, Color(0xFF20E398)),
                    modifier = Modifier
                        .scale(pulseScale)
                        .width(130.dp)
                        .height(38.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = Color(0xFF20E398), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mini Chip", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "⇄",
                    color = Color(0xFF20E398),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                // Expanded Dock Demo
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF182228),
                    border = BorderStroke(1.5.dp, Color(0xFF20E398)),
                    modifier = Modifier
                        .width(130.dp)
                        .height(38.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = Color(0xFF20E398), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Full Dock", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Scroll Down to Shrink, Tap to Expand",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Scroll down on any webpage to shrink the toolbar into a tiny floating chip, giving you maximum reading space. Scroll up or tap the chip anytime to expand it.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E9BA2),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF7E8B92)),
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("← Back", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                Button(
                    onClick = onFinish,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF20E398),
                        contentColor = Color(0xFF090D0F),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Got it, Let's Browse!", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
