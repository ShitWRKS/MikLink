# MIKLINK - UX/UI SPECIFICATION (Stile Ubiquiti)
**Versione**: 2.0  
**Data**: 2025-01-15  
**Design System**: Ubiquiti-inspired

---

## 🎨 DESIGN PRINCIPLES

### Core Values
1. **Chiarezza**: Ogni azione deve essere immediatamente comprensibile
2. **Coerenza**: Stesso pattern visivo in tutte le schermate
3. **Feedback**: Ogni operazione mostra stato (loading/success/error)
4. **Professionalità**: Estetica pulita e moderna per uso enterprise

### Target Audience
- **Primario**: Tecnici di rete certificatori
- **Secondario**: Installatori cavi strutturati
- **Terziario**: Manager IT per review report

---

## 🎭 PALETTE COLORI

### Colori Primari (Ubiquiti-Style)
```kotlin
// Color.kt
val UbiquitiBlue = Color(0xFF0559C9)         // Primary brand
val UbiquitiLightBlue = Color(0xFF4A9AFF)    // Accents
val UbiquitiDarkBlue = Color(0xFF003D82)     // Dark variant

val UbiquitiGray900 = Color(0xFF2C3E50)      // Text primary
val UbiquitiGray700 = Color(0xFF4A5568)      // Text secondary
val UbiquitiGray300 = Color(0xFFD1D5DB)      // Borders
val UbiquitiGray100 = Color(0xFFECF0F1)      // Backgrounds

val UbiquitiGreen = Color(0xFF00C896)        // Success
val UbiquitiYellow = Color(0xFFF59E0B)       // Warning
val UbiquitiRed = Color(0xFFE74C3C)          // Error
```

### Applicazione Palette
| Elemento | Light Theme | Dark Theme |
|----------|-------------|-----------|
| Background | `Color.White` | `UbiquitiGray900` |
| Surface | `UbiquitiGray100` | `UbiquitiGray700` |
| Primary | `UbiquitiBlue` | `UbiquitiLightBlue` |
| On-Primary | `Color.White` | `UbiquitiGray900` |
| Success | `UbiquitiGreen` | `UbiquitiGreen` |
| Error | `UbiquitiRed` | `UbiquitiRed` |

---

## 📐 TYPOGRAPHY

### Font Family
```kotlin
// Type.kt
val UbiquitiFontFamily = FontFamily.Default // O custom se richiesto

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = UbiquitiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = UbiquitiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    titleLarge = TextStyle(
        fontFamily = UbiquitiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = UbiquitiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = UbiquitiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = UbiquitiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = UbiquitiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)
```

---

## 🧱 COMPONENT STANDARDS

### Card
```kotlin
@Composable
fun UbiquitiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        content = {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    )
}
```

**Regole**:
- **Shape**: `RoundedCornerShape(12.dp)` per tutte le card
- **Elevation**: `2.dp` default, `4.dp` pressed
- **Padding interno**: `16.dp`
- **Spacing tra card**: `12.dp`

---

### Button
```kotlin
@Composable
fun UbiquitiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = UbiquitiBlue,
            contentColor = Color.White,
            disabledContainerColor = UbiquitiGray300,
            disabledContentColor = UbiquitiGray700
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            disabledElevation = 0.dp
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun UbiquitiSuccessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = UbiquitiGreen,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun UbiquitiErrorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = UbiquitiRed,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
```

**Regole**:
- **Height principale**: `56.dp` (pulsanti primari), `48.dp` (secondari)
- **Shape**: `RoundedCornerShape(12.dp)`
- **Icon size**: `20.dp` con `8.dp` spacing

---

### TopAppBar
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UbiquitiTopAppBar(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Dashboard, // O icona specifica schermata
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        navigationIcon = navigationIcon ?: {},
        actions = actions,
        colors = colors
    )
}
```

**Regole**:
- **Icona titolo**: `28.dp`, tint `primary`
- **Container color**: `primaryContainer.copy(alpha = 0.3f)` (semi-trasparente)
- **Back icon**: `Icons.AutoMirrored.Filled.ArrowBack`

---

### StatusBadge
```kotlin
@Composable
fun UbiquitiStatusBadge(
    status: String, // "PASS", "FAIL", "INFO", "SKIPPED"
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor, icon) = when (status.uppercase()) {
        "PASS" -> Triple(UbiquitiGreen.copy(alpha = 0.15f), UbiquitiGreen, Icons.Default.CheckCircle)
        "FAIL" -> Triple(UbiquitiRed.copy(alpha = 0.15f), UbiquitiRed, Icons.Default.Cancel)
        "INFO" -> Triple(UbiquitiBlue.copy(alpha = 0.15f), UbiquitiBlue, Icons.Default.Info)
        "SKIPPED" -> Triple(UbiquitiGray300.copy(alpha = 0.3f), UbiquitiGray700, Icons.Default.SkipNext)
        else -> Triple(UbiquitiGray300.copy(alpha = 0.3f), UbiquitiGray700, Icons.Default.Help)
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                status.uppercase(),
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

---

### ProbeStatusIndicator (LED)
```kotlin
@Composable
fun ProbeStatusLED(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isOnline) UbiquitiGreen else UbiquitiRed)
    )
}

@Composable
fun GlobalProbeStatusBadge(
    currentProbe: ProbeConfig?,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isOnline) UbiquitiGreen.copy(alpha = 0.15f) else UbiquitiRed.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, if (isOnline) UbiquitiGreen else UbiquitiRed)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProbeStatusLED(isOnline)
            Text(
                currentProbe?.name ?: "Nessuna sonda",
                style = MaterialTheme.typography.labelMedium,
                color = if (isOnline) UbiquitiGreen else UbiquitiRed,
                fontWeight = FontWeight.Medium
            )
            Icon(
                Icons.Default.Router,
                contentDescription = null,
                tint = if (isOnline) UbiquitiGreen else UbiquitiRed,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
```

---

## 📱 SCREEN LAYOUTS

### Dashboard (Post-Refactor)

**Layout Structure**:
```
┌─────────────────────────────────────────┐
│ TopAppBar                               │
│ "Dashboard"    [History] [Settings]    │
├─────────────────────────────────────────┤
│ [Global Probe Status Badge]            │ ← NUOVO: top-right floating
├─────────────────────────────────────────┤
│ Header Card (Icon + Title)             │
├─────────────────────────────────────────┤
│ 1. Seleziona Cliente                    │
│    [Card con RadioButton list]          │
│    [Pulsante "Gestisci"]                │
├─────────────────────────────────────────┤
│ 2. Seleziona Profilo Test              │ ← RIMOSSO: card Sonda
│    [Card con RadioButton list]          │
│    [Pulsante "Gestisci"]                │
├─────────────────────────────────────────┤
│ 3. Inserisci ID Presa                   │
│    [OutlinedTextField auto-populated]   │
├─────────────────────────────────────────┤
│                                         │
│ (scroll space)                          │
│                                         │
├─────────────────────────────────────────┤
│ Bottom Bar (Fixed)                      │
│ [Warning chip se sonda offline]         │
│ [AVVIA TEST button - pulsating]         │
└─────────────────────────────────────────┘
```

**Key Changes**:
- ❌ Rimossa card "Seleziona Sonda"
- ✅ Badge globale stato sonda in top-right
- ✅ Warning chip dinamico se sonda offline (non bloccante)

---

### Settings (Post-Refactor)

**Layout Structure**:
```
┌─────────────────────────────────────────┐
│ TopAppBar                               │
│ [←] "Impostazioni"                      │
├─────────────────────────────────────────┤
│ Header Info Card                        │
│ "Configurazione App"                    │
├─────────────────────────────────────────┤
│ SEZIONE: Sonda                          │ ← NUOVO
│ ┌─────────────────────────────────────┐ │
│ │ [Router Icon] Sonda MikroTik        │ │
│ │ 192.168.0.251 | RB4011              │ │
│ │ [LED verde/rosso] ONLINE/OFFLINE    │ │
│ └─────────────────────────────────────┘ │
│ [Pulsante "Configura Sonda"]            │
├─────────────────────────────────────────┤
│ SEZIONE: Aspetto                        │
│ Tema: [Auto] [Chiaro] [Scuro]          │
├─────────────────────────────────────────┤
│ SEZIONE: Template PDF                   │ ← NUOVO
│ [Pulsante "Carica Template"]            │
│ [Preview mini WebView]                  │
│ [Pulsante "Ripristina Default"]         │
├─────────────────────────────────────────┤
│ SEZIONE: Informazioni                   │
│ Versione: 2.0                           │
│ Build: Release                          │
└─────────────────────────────────────────┘
```

**Key Changes**:
- ✅ Sezione "Sonda" con card riepilogo + config button
- ✅ Sezione "Template PDF" con carica/preview/ripristina
- ❌ Rimossa sezione "Gestione Dati" (duplicati Dashboard)

---

### Test Execution

**Layout Structure** (Durante Esecuzione):
```
┌─────────────────────────────────────────┐
│ TopAppBar                               │
│ [←] "Test in corso..."                  │
│     [Hourglass icon animato]            │
├─────────────────────────────────────────┤
│ Header Progress Card                    │
│ [CircularProgressIndicator]             │
│ "Test in esecuzione..."                 │
│ [LinearProgressIndicator]               │
├─────────────────────────────────────────┤
│ [Toggle "Mostra log grezzi"]            │
├─────────────────────────────────────────┤
│ ┌ Informazioni ───────────────────────┐ │
│ │ [Network icon] Rete (DHCP)          │ │
│ │ 192.168.1.100/24 | GW: .1           │ │
│ └─────────────────────────────────────┘ │
│ ┌ Test ───────────────────────────────┐ │
│ │ [Link icon] Link [INFO/spinner]     │ │
│ │ Status: link-ok | Rate: 1Gbps       │ │
│ │ [Ping icon] Ping [IN PROGRESS]      │ │
│ │ Target 1: ... [spinner]             │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**Layout Structure** (Completato):
```
┌─────────────────────────────────────────┐
│ TopAppBar                               │
│ [←] "Test Completato"                   │
│     [CheckCircle verde / Cancel rosso]  │
├─────────────────────────────────────────┤
│ Header Result Card (colorato)           │
│ [Grande icona Check/X in cerchio]       │
│ "TEST SUPERATO / TEST FALLITO"          │
│ Badge: [Sonda: RB4011] [Presa: PRT-001] │
├─────────────────────────────────────────┤
│ "Dettagli Test" [Toggle log button]    │
├─────────────────────────────────────────┤
│ ┌ Informazioni ───────────────────────┐ │
│ │ [Network] Rete [PASS badge]         │ │
│ │ [Dettagli button espandibile]       │ │
│ │ [LLDP] LLDP/CDP [PASS badge]        │ │
│ └─────────────────────────────────────┘ │
│ ┌ Test ───────────────────────────────┐ │
│ │ [Link] Link [PASS/FAIL badge]       │ │
│ │ [Ping] Ping [PASS badge]            │ │
│ │ [Traceroute] Traceroute [FAIL]      │ │
│ │ [TDR] TDR [PASS badge]              │ │
│ └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│ Bottom Bar                              │
│ [CHIUDI] [RIPETI] [SALVA button verde]  │
└─────────────────────────────────────────┘
```

**Animazioni**:
- Card appaiono con `fadeIn() + slideInVertically()` durante esecuzione
- Badge cambiano colore smooth con `animateColorAsState()`
- Progress indicator lineare sotto header

---

### Probe Edit (Sonda Unica)

**Layout Structure**:
```
┌─────────────────────────────────────────┐
│ TopAppBar                               │
│ [←] "Configura Sonda"                   │
├─────────────────────────────────────────┤
│ [OutlinedTextField] Nome Sonda          │
│ [OutlinedTextField] IP Address          │
│ [OutlinedTextField] Username            │
│ [OutlinedTextField] Password            │
│ [Switch] Usa HTTPS (SSL)                │
├─────────────────────────────────────────┤
│ [Button "Verifica Sonda"] (primary)     │ ← NUOVO: test completo
├─────────────────────────────────────────┤
│ [Verification Result Card]              │
│ Board: RB4011iGS+RM                     │
│ TDR Supported: Sì                       │
│ [Dropdown] Interfaccia Test: ether2     │
├─────────────────────────────────────────┤
│ [Smoke Test Results]                    │ ← NUOVO: risultati API test
│ ✓ System Resource                       │
│ ✓ DHCP Client                           │
│ ✓ Interface Toggle                      │
│ ✓ Link Status                           │
│ ✓ Ping                                  │
│ ✓ Traceroute                            │
├─────────────────────────────────────────┤
│ Bottom Bar                              │
│ [SALVA button - primary blue]           │
└─────────────────────────────────────────┘
```

**Key Changes**:
- ✅ Pulsante "Verifica Sonda" esegue smoke test completo
- ✅ Card risultati test mostra tutti i 6 endpoint validati
- ✅ Salvataggio abilitato solo dopo verifica successo

---

## 🎬 ANIMATIONS & TRANSITIONS

### Card Appearance
```kotlin
AnimatedVisibility(
    visible = true,
    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
        initialOffsetY = { it / 4 },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    )
) {
    UbiquitiCard { /* content */ }
}
```

### Status Badge Color Transition
```kotlin
val badgeColor by animateColorAsState(
    targetValue = if (status == "PASS") UbiquitiGreen else UbiquitiRed,
    animationSpec = tween(300)
)
```

### Progress Indicator
```kotlin
// Durante test
LinearProgressIndicator(
    modifier = Modifier.fillMaxWidth(),
    color = UbiquitiBlue,
    trackColor = UbiquitiGray300
)

// Test completato (animazione finale)
LinearProgressIndicator(
    progress = 1f,
    modifier = Modifier.fillMaxWidth(),
    color = if (isPassed) UbiquitiGreen else UbiquitiRed
)
```

---

## 📊 SPACING & SIZING

### Standard Spacing
| Nome | Valore | Uso |
|------|--------|-----|
| `SpacingXS` | `4.dp` | Icon-Text gap |
| `SpacingS` | `8.dp` | Intra-componente |
| `SpacingM` | `12.dp` | Card spacing |
| `SpacingL` | `16.dp` | Padding card/screen |
| `SpacingXL` | `24.dp` | Section spacing |
| `SpacingXXL` | `32.dp` | Header/Footer |

### Component Sizing
| Componente | Size | Note |
|-----------|------|------|
| Icon principale | `28.dp` | TopAppBar, Header |
| Icon secondaria | `20.dp` | Buttons, Cards |
| Icon piccola | `16.dp` | Badge, Chips |
| LED status | `12.dp` | Indicator online/offline |
| Avatar/Logo | `56.dp` | Circle |
| Button height primario | `56.dp` | Azioni principali |
| Button height secondario | `48.dp` | Azioni secondarie |
| TextField height | `56.dp` | Standard input |

---

## ♿ ACCESSIBILITY

### Touch Targets
- **Minimo**: `48.dp x 48.dp` per tutti i pulsanti cliccabili
- **Raccomandato**: `56.dp x 56.dp` per azioni primarie

### Contrast Ratio
- **Text on Background**: minimo 4.5:1 (WCAG AA)
- **Large Text**: minimo 3:1
- **Success/Error badges**: verificare contrasto con background

### Content Descriptions
```kotlin
Icon(
    Icons.Default.CheckCircle,
    contentDescription = "Test superato", // Sempre presente
    tint = UbiquitiGreen
)
```

---

## 🌓 DARK MODE

### Color Adjustments
```kotlin
@Composable
fun MikLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = UbiquitiLightBlue,
            onPrimary = UbiquitiGray900,
            primaryContainer = UbiquitiDarkBlue,
            surface = UbiquitiGray900,
            onSurface = Color.White,
            surfaceVariant = UbiquitiGray700,
            error = UbiquitiRed,
            background = Color.Black
        )
    } else {
        lightColorScheme(
            primary = UbiquitiBlue,
            onPrimary = Color.White,
            primaryContainer = UbiquitiGray100,
            surface = Color.White,
            onSurface = UbiquitiGray900,
            surfaceVariant = UbiquitiGray100,
            error = UbiquitiRed,
            background = Color.White
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

**Fine UX_UI_SPEC.md**

