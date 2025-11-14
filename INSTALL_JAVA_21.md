# 📦 Installazione Java 21 LTS per Android Studio

**Data:** 2025-01-14  
**Versione Java:** 21 LTS (Long Term Support)  
**Sistema Operativo:** Windows 11

---

## 🎯 **Opzione A: Installazione Automatica tramite Android Studio (RACCOMANDATO)**

### **Passaggi:**

1. **Apri Android Studio**
2. **File → Settings** (o `Ctrl+Alt+S`)
3. **Build, Execution, Deployment → Build Tools → Gradle**
4. In **Gradle JDK**, clicca sul menu a tendina
5. Seleziona **Download JDK...**
6. Nella finestra di dialogo:
   - **Version:** `21`
   - **Vendor:** `Eclipse Temurin (AdoptOpenJDK HotSpot)` o `Oracle OpenJDK`
   - **Location:** Lascia default
7. Clicca **Download**
8. Attendi che Android Studio scarichi e installi Java 21
9. Clicca **OK** per chiudere le impostazioni

### **Verifica:**

Dopo l'installazione, Android Studio configurerà automaticamente il progetto per usare Java 21.

---

## 🎯 **Opzione B: Installazione Manuale**

Se preferisci scaricare manualmente Java 21:

### **1. Scarica Java 21**

**Link ufficiali:**

- **Eclipse Temurin (Raccomandato):**  
  https://adoptium.net/temurin/releases/?version=21
  
  - Seleziona: **Windows x64**, **JDK**, **21 (LTS)**
  - Download: `OpenJDK21U-jdk_x64_windows_hotspot_21.0.x_xx.msi`

- **Oracle OpenJDK:**  
  https://www.oracle.com/java/technologies/downloads/#java21
  
  - Seleziona: **Windows**, **x64 Installer**

### **2. Installa Java 21**

1. Esegui il file `.msi` scaricato
2. Segui la procedura guidata
3. **IMPORTANTE:** Annota il percorso di installazione (es. `C:\Program Files\Eclipse Adoptium\jdk-21.0.x`)

### **3. Configura Android Studio**

1. **File → Settings** (`Ctrl+Alt+S`)
2. **Build, Execution, Deployment → Build Tools → Gradle**
3. In **Gradle JDK**, clicca menu a tendina
4. Seleziona **Add JDK...**
5. Naviga alla cartella di installazione Java 21
6. Seleziona la cartella (es. `C:\Program Files\Eclipse Adoptium\jdk-21.0.x`)
7. Clicca **OK**

### **4. Imposta come JDK di Default**

1. **File → Project Structure** (`Ctrl+Alt+Shift+S`)
2. **SDK Location**
3. In **JDK location**, verifica che punti a Java 21
4. Clicca **OK**

---

## 🎯 **Opzione C: Usa SDKMAN (Avanzato - Solo per utenti esperti)**

Se hai WSL (Windows Subsystem for Linux):

```bash
# Installa SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Installa Java 21
sdk install java 21.0.1-tem

# Imposta come default
sdk default java 21.0.1-tem
```

---

## ✅ **Verifica Installazione**

### **Verifica da Terminale:**

Apri PowerShell o CMD e esegui:

```powershell
java -version
```

**Output atteso:**
```
openjdk version "21.0.x" 2024-xx-xx LTS
OpenJDK Runtime Environment Temurin-21.0.x+x (build 21.0.x+x-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.x+x (build 21.0.x+x-LTS, mixed mode, sharing)
```

### **Verifica da Android Studio:**

1. Apri il progetto MikLink
2. **View → Tool Windows → Build**
3. Esegui il comando:
   ```
   ./gradlew --version
   ```
4. Verifica che mostri:
   ```
   JVM:          21.0.x (Eclipse Adoptium 21.0.x+x)
   ```

---

## 🔧 **Dopo l'Installazione: Sync Gradle**

1. **File → Sync Project with Gradle Files**
2. Oppure clicca sull'icona **"Sync Now"** che apparirà in alto a destra
3. Attendi che Gradle sincronizzi tutte le dipendenze

---

## 🚀 **Build del Progetto**

Dopo aver configurato Java 21:

```powershell
cd C:\Users\dot\AndroidStudioProjects\MikLink
.\gradlew assembleDebug
```

**Output atteso:**
```
BUILD SUCCESSFUL in XXs
```

---

## ⚠️ **Problemi Comuni**

### **Problema 1: "Cannot find Java 21"**

**Soluzione:**
- Verifica che Java 21 sia installato correttamente
- Riavvia Android Studio
- Vai in **File → Invalidate Caches → Invalidate and Restart**

### **Problema 2: "Unsupported class file major version 65"**

**Soluzione:**
- Questo significa che stai usando Java 21 ma Gradle usa una versione vecchia
- Aggiorna Gradle wrapper:
  ```powershell
  .\gradlew wrapper --gradle-version=8.5
  ```

### **Problema 3: "JAVA_HOME non impostato"**

**Soluzione:**
1. Cerca "Variabili d'ambiente" in Windows
2. Clicca **Variabili d'ambiente**
3. In **Variabili di sistema**, clicca **Nuova**
4. Nome: `JAVA_HOME`
5. Valore: `C:\Program Files\Eclipse Adoptium\jdk-21.0.x` (percorso tuo)
6. Clicca **OK**
7. Riavvia il PC

---

## 📊 **Versioni Configurate nel Progetto**

| Componente | Versione |
|------------|----------|
| Java (JVM) | **21 LTS** |
| Kotlin | 2.0.0 |
| Android Gradle Plugin | 8.4.1 |
| Gradle | 8.13 |
| Compile SDK | 34 (Android 14) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## 🎯 **Vantaggi di Java 21**

- ✅ **Performance migliorate** (fino a 30% più veloce)
- ✅ **Virtual Threads** (concorrenza semplificata)
- ✅ **Pattern Matching** migliorato
- ✅ **Records** e sealed classes
- ✅ **Supporto LTS** fino al 2029
- ✅ **Compatibile** con Android Gradle Plugin 8.x

---

## 📚 **Risorse Utili**

- **Java 21 Release Notes:** https://openjdk.org/projects/jdk/21/
- **Eclipse Temurin:** https://adoptium.net/
- **Android Gradle Plugin Compatibility:** https://developer.android.com/build/releases/gradle-plugin

---

**Procedi con l'installazione e poi esegui la build! 🚀**

