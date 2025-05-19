#!/bin/bash

# Gaming Android OS build environment setup script

function hmm() {
  echo "Gaming Android OS build system commands:"
  echo "  lunch             - select a target to build"
  echo "  m                 - build all of the modules in the current directory"
  echo "  mm                - build all of the modules in the current directory, but not their dependencies"
  echo "  mma               - build all of the modules in the current directory, and their dependencies"
  echo "  mmm               - build all of the modules in the supplied directories"
  echo "  mmma              - build all of the modules in the supplied directories, and their dependencies"
  echo "  croot             - change directory to the top of the tree"
  echo "  gaming_mode       - enable gaming mode optimizations"
  echo "  perf_profile      - set performance profile (balanced, gaming, battery)"
  echo
  echo "Environment options:"
  echo "  GAMING_PROFILE    - set to 'extreme', 'balanced', or 'battery' (default: balanced)"
  echo "  LOW_LATENCY       - set to 'true' or 'false' to enable/disable low latency mode (default: true)"
  echo "  THERMAL_THROTTLE  - set to 'aggressive', 'normal', or 'disabled' (default: normal)"
}

function lunch() {
  echo "Available targets:"
  echo "  1. gaming_x86_64-eng (PC/VirtualBox build)"
  echo "  2. gaming_x86-eng (32-bit PC build)"
  echo "  3. gaming_arm64-eng (ARM64 device build)"
  echo "  4. gaming_arm-eng (ARM device build)"
  echo
  echo "Enter target number or name:"
  read target
  
  case $target in
    1|gaming_x86_64-eng)
      export TARGET_PRODUCT=gaming_x86_64
      export TARGET_BUILD_VARIANT=eng
      ;;
    2|gaming_x86-eng)
      export TARGET_PRODUCT=gaming_x86
      export TARGET_BUILD_VARIANT=eng
      ;;
    3|gaming_arm64-eng)
      export TARGET_PRODUCT=gaming_arm64
      export TARGET_BUILD_VARIANT=eng
      ;;
    4|gaming_arm-eng)
      export TARGET_PRODUCT=gaming_arm
      export TARGET_BUILD_VARIANT=eng
      ;;
    *)
      echo "Invalid target"
      return 1
      ;;
  esac
  
  echo "Building for $TARGET_PRODUCT-$TARGET_BUILD_VARIANT"
}

function gaming_mode() {
  echo "Enabling gaming mode optimizations"
  export GAMING_PROFILE=extreme
  export LOW_LATENCY=true
  export THERMAL_THROTTLE=normal
}

function perf_profile() {
  case $1 in
    balanced)
      export GAMING_PROFILE=balanced
      export LOW_LATENCY=true
      export THERMAL_THROTTLE=normal
      ;;
    gaming)
      export GAMING_PROFILE=extreme
      export LOW_LATENCY=true
      export THERMAL_THROTTLE=normal
      ;;
    battery)
      export GAMING_PROFILE=battery
      export LOW_LATENCY=false
      export THERMAL_THROTTLE=aggressive
      ;;
    *)
      echo "Usage: perf_profile [balanced|gaming|battery]"
      return 1
      ;;
  esac
  
  echo "Performance profile set to $1"
}

# Set default environment variables
export GAMING_PROFILE=balanced
export LOW_LATENCY=true
export THERMAL_THROTTLE=normal

# Add build tools to path
export PATH=$PATH:$(pwd)/build/tools

# Display help message
hmm
