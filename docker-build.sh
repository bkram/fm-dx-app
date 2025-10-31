#!/usr/bin/env bash
# Install dependencies
export DEBIAN_FRONTEND=noninteractive
export ANDROID_SDK_ROOT=/usr/lib/android-sdk/
export ANDROID_HOME=/usr/lib/android-sdk/

apt-get update && apt-get install -y wget unzip curl openjdk-21-jdk && rm -rf /var/lib/apt/lists/*

echo "Getting Android Studio"
wget -O /tmp/android-commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip

echo "Unpacking Android Studio"
unzip -u /tmp/android-commandlinetools.zip -d /usr/lib/android-sdk

echo "Updating sdkmanager"
/usr/lib/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=/usr/lib/android-sdk/ --update

echo "Installing Android SDK"
bash -c 'yes | /usr/lib/android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=/usr/lib/android-sdk/ "platforms;android-36.1"'


echo sdk.dir=$ANDROID_SDK_ROOT >  local.properties



# Build
./gradlew -version

./gradlew assembleDebug

#Where: sdk.dir property in local.properties file. Problem: Directory does not exist
