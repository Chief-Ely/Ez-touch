#!/bin/bash
set -e

echo "=== Installing Android SDK & Command Line Tools ==="

# Define paths
export ANDROID_HOME="/workspaces/android-sdk"
mkdir -p $ANDROID_HOME
cd $ANDROID_HOME

# Download Android Command Line Tools
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
curl -s -o cmdline-tools.zip $CMDLINE_TOOLS_URL
mkdir -p temp-tools
unzip -q cmdline-tools.zip -d temp-tools
mkdir -p cmdline-tools/latest
mv temp-tools/cmdline-tools/* cmdline-tools/latest/
rm -rf cmdline-tools.zip temp-tools

# Set Environment Variables permanently in bashrc
echo "export ANDROID_HOME=$ANDROID_HOME" >> ~/.bashrc
echo "export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools" >> ~/.bashrc

# Export for current session
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Accept licenses and install platform-tools, build-tools, and platform 34
yes | sdkmanager --licenses > /dev/null 2br
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "=== Android Environment Setup Complete! ==="