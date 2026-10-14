GRADLE		= cd android && ./gradlew -q
SDK			?= $(or $(ANDROID_HOME),$(ANDROID_SDK_ROOT),$(HOME)/Android/Sdk)
APK			= android/app/build/outputs/apk/debug/app-debug.apk
ACTIVITY	= com.ravazque.swiftycompanion/.MainActivity
ADB			= $(or $(shell command -v adb),$(SDK)/platform-tools/adb)


help:
	@echo "make install    check the JDK, the Android SDK and .env; nothing is built"
	@echo "make apk        build the debug APK: $(APK)"
	@echo "make run        build, install on the connected device (Gradle's installDebug) and open the app"
	@echo "make test       unit tests on the JVM"
	@echo "make lint       static checks"
	@echo "make clean      Gradle's build output"
	@echo "make fclean     clean, and Gradle's project caches"

# Gradle needs a JDK 17+ to start; it downloads the JDK 25 the build runs on.
install:
	@command -v java >/dev/null || { echo "java not found: install a JDK 17 or newer" >&2; exit 1; }
	@[ -d "$(SDK)/platform-tools" ] || { echo "Android SDK not found in $(SDK): install Android Studio or the command-line tools, or set ANDROID_HOME" >&2; exit 1; }
	@[ -n "$(ANDROID_HOME)$(ANDROID_SDK_ROOT)" ] || [ -f android/local.properties ] || echo "sdk.dir=$(SDK)" > android/local.properties
	@[ -f .env ] || echo "no .env: copy .env.example to .env and fill in the API credentials (the app builds without them)" >&2
	@echo "environment ready: make apk builds the app, make run puts it on a device"

apk: install
	$(GRADLE) assembleDebug
	@echo "built $(APK)"

device:
	@$(ADB) get-state >/dev/null 2>&1 || { echo "no device: start an emulator or connect a phone with USB debugging" >&2; exit 1; }

run: device install
	$(GRADLE) installDebug
	$(ADB) shell am start -n $(ACTIVITY)

test:
	$(GRADLE) testDebugUnitTest

lint:
	$(GRADLE) lintDebug

clean:
	$(GRADLE) clean

fclean: clean
	rm -rf android/.gradle android/.kotlin android/build android/app/build

re: fclean install

.PHONY: help install apk device run test lint clean fclean re
