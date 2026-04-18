# IDE Setup Instructions

## IntelliJ IDEA Configuration

### Fix Missing Dependencies in IDE

The error `NoClassDefFoundError: org/json/JSONObject` indicates the IDE isn't loading Gradle dependencies.

**Solution:**

1. **Refresh Gradle Dependencies**
   - Open the Gradle tool window (View → Tool Windows → Gradle)
   - Right-click on the project → "Reload Gradle Project"
   - Or click the "Reload" button in the Gradle tool window

2. **Enable Gradle Dependency Management**
   - File → Settings → Build, Execution, Deployment → Build Tools → Gradle
   - Set "Build and run using" to: Gradle
   - Set "Run tests using" to: Gradle

3. **Verify Dependencies**
   - File → Project Structure → Modules → Dependencies
   - Ensure all Gradle dependencies are listed
   - Should include: org.json:json, jsoup, gson, luaj-jse, rhino, etc.

### Alternative: Create Run Configuration

1. Run → Edit Configurations
2. Click "+" → Application
3. Set:
   - **Name**: LeechText App
   - **Main class**: `dark.leech.text.ui.main.App`
   - **VM options**: `-Xmx1g`
   - **Working directory**: `$ProjectFileDir$`
   - **Use classpath of module**: `leechtext-java.main`
4. Click "OK"

### Quick Test

After refreshing dependencies, try running:

```bash
# From terminal using Gradle (should work)
./gradlew run

# Or use the JAR file (always works)
java -jar build/libs/leechtext-java-2019.03.30.jar
```

### If Issues Persist

The JAR file always includes all dependencies:
```bash
make build  # Creates fat JAR
java -jar build/libs/leechtext-java-2019.03.30.jar
```

For development, the IDE classpath issue should be resolved by reloading Gradle dependencies.
