# Animation Examples & Implementation Guide

## Quick Reference for Animations

### 1. Using Pre-built Animations

#### Scale In Effect
```kotlin
@Composable
fun MyComponent() {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = FastAnimation,
        label = "scaleIn"
    )
    
    Box(modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)) {
        // Your content
    }
}
```

#### Press Feedback
```kotlin
@Composable
fun InteractiveElement(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed.value) 0.96f else 1f,
        animationSpec = tween(150),
        label = "pressScale"
    )
    
    Button(
        onClick = onClick,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        interactionSource = interactionSource
    ) {
        Text("Click me")
    }
}
```

#### Shadow with Animation
```kotlin
@Composable
fun AnimatedCard() {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = MediumAnimation,
        label = "cardScale"
    )
    
    Card(
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f)
            )
    ) {
        // Card content
    }
}
```

---

## Design Tokens Usage

### Spacing in Layouts
```kotlin
Column(modifier = Modifier.padding(Spacing.lg)) {
    Text("Title", modifier = Modifier.padding(bottom = Spacing.md))
    Text("Subtitle", modifier = Modifier.padding(bottom = Spacing.sm))
}
```

### Rounded Corners
```kotlin
Surface(
    shape = RoundedCornerShape(Radius.lg),
    modifier = Modifier.shadow(Shadows.md, shape = RoundedCornerShape(Radius.lg))
) {
    // Content
}
```

### Component Sizing
```kotlin
Box(
    modifier = Modifier
        .width(ComponentSizes.buttonHeight)
        .height(ComponentSizes.buttonHeight)
)
```

### Animation Timing
```kotlin
val scale by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(AnimationDurations.medium),
    label = "scale"
)
```

---

## Common Animation Patterns

### Pattern 1: Fade In with Scale
```kotlin
@Composable
fun FadeInScale() {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = FastAnimation,
        label = "fadeAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = FastAnimation,
        label = "fadeScale"
    )
    
    Box(
        modifier = Modifier
            .graphicsLayer(
                alpha = alpha,
                scaleX = scale,
                scaleY = scale
            )
    ) {
        // Content
    }
}
```

### Pattern 2: Slide Up with Fade
```kotlin
@Composable
fun SlideUpFade() {
    val yOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = MediumAnimation,
        label = "slideUp"
    )
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = MediumAnimation,
        label = "slideAlpha"
    )
    
    Box(
        modifier = Modifier
            .graphicsLayer(
                translationY = yOffset,
                alpha = alpha
            )
    ) {
        // Content
    }
}
```

### Pattern 3: Hover Effect
```kotlin
@Composable
fun HoverableCard(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val isHovered = interactionSource.collectIsHoveredAsState()
    
    val elevation by animateFloatAsState(
        targetValue = if (isPressed.value) 4f else if (isHovered.value) 8f else 2f,
        animationSpec = FastAnimation,
        label = "cardElevation"
    )
    
    Card(
        modifier = Modifier
            .shadow(elevation.dp, shape = RoundedCornerShape(Radius.lg))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        // Content
    }
}
```

### Pattern 4: List Item Stagger
```kotlin
@Composable
fun StaggeredList(items: List<String>) {
    items.forEachIndexed { index, item ->
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = AnimationDurations.medium,
                delayMillis = index * 100  // Stagger by 100ms per item
            ),
            label = "itemScale"
        )
        
        Card(
            modifier = Modifier
                .graphicsLayer(
                    scaleY = scale,
                    transformOrigin = TransformOrigin.TopCenter
                )
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Text(item)
        }
    }
}
```

### Pattern 5: Pulse/Loading Effect
```kotlin
@Composable
fun PulsingElement() {
    val pulse by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = Modifier
            .size(50.dp)
            .graphicsLayer(alpha = pulse)
            .background(PundarBlue, CircleShape)
    )
}
```

---

## Color-Matched Shadows

### Blue Element Shadows
```kotlin
modifier.shadow(
    elevation = 8.dp,
    shape = RoundedCornerShape(16.dp),
    ambientColor = PundarBlue.copy(alpha = 0.3f)
)
```

### Gold Element Shadows
```kotlin
modifier.shadow(
    elevation = 8.dp,
    shape = RoundedCornerShape(16.dp),
    ambientColor = PundarGold.copy(alpha = 0.3f)
)
```

### Subtle Black Shadow
```kotlin
modifier.shadow(
    elevation = 4.dp,
    shape = RoundedCornerShape(16.dp),
    ambientColor = Color.Black.copy(alpha = 0.08f)
)
```

---

## Best Practices

### ✅ DO:
- Use design token values for consistency
- Keep animation durations under 600ms for UI feedback
- Use `EaseOutCubic` for natural deceleration
- Apply color-matched shadows for visual cohesion
- Use `graphicsLayer` for GPU-accelerated animations

### ❌ DON'T:
- Create multiple animation states manually
- Use `animatedValue` instead of `animateFloatAsState`
- Apply animations to every single element
- Use durations longer than 1000ms for standard interactions
- Mix conflicting animation timings

---

## Testing Your Animations

### Visual Inspection Checklist
```kotlin
// When implementing animations, check:

// 1. Smooth entrance
// Does the component fade/scale in smoothly?

// 2. Press feedback
// Does button scale down when clicked?

// 3. Navigation transitions
// Do nav items smoothly animate?

// 4. Performance
// Are animations smooth at 60fps?

// 5. Consistency
// Do similar elements animate the same way?
```

---

## Integration with Existing Screens

### Example: Enhance HomeScreen
```kotlin
// In HomeScreen.kt, import:
import com.example.pundarapp.ui.utils.Animations
import com.example.pundarapp.ui.utils.DesignTokens

@Composable
fun HomeScreen() {
    Column(modifier = Modifier.padding(Spacing.lg)) {
        // Use design tokens and animations here
        PundarCard {
            // Card content with animations
        }
    }
}
```

---

## Performance Tips

1. **Avoid Recomposition**: Use `remember` to cache animation states
2. **Use Stable Objects**: Keep animation objects in `remember`
3. **Batch Animations**: Group related animations together
4. **Lazy Loading**: Only animate visible elements
5. **Profile**: Use Android Profiler to monitor animation performance

---

## Troubleshooting

### Animation not playing?
- Check `label` parameter is unique
- Verify `animationSpec` is correct
- Ensure modifier is applied correctly

### Choppy animation?
- Reduce animation duration
- Use `graphicsLayer` instead of layout changes
- Check for heavy recompositions

### Shadow not visible?
- Check `ambientColor` has sufficient alpha
- Verify shape matches modifier
- Ensure background color isn't obscuring shadow

---

## Additional Resources

- Compose Animation Documentation
- Material Design Motion Guidelines
- PUNDAR Theme Color Codes
- Component Library Examples

---

**Last Updated**: 2026-07-11
**Animation Framework Version**: Jetpack Compose 1.6+
