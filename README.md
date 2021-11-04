# Compile
```bash
gradlew clean build
docker build --tag <tagname> .
```

#Pre-Commit
```bash
rm -rf gradle/wrapper gradlew gradlew.bat
gradle wrapper --gradle-version=7.1 --distribution-type=all
git add gradle/wrapper/ gradlew gradlew.bat
git update-index --chmod=+x gradle/wrapper/gradle-wrapper.jar gradlew gradlew.bat
git add gradle/wrapper/ gradlew gradlew.bat
```

# Publish
Requires `GIT_USERNAME` and `GIT_TOKEN` environment variables to be set
```bash
gradle publish
```