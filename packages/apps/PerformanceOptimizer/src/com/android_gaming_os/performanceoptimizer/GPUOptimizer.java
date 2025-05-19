package com.android_gaming_os.performanceoptimizer;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class responsible for optimizing GPU performance.
 * Handles GPU frequency scaling and power management.
 */
public class GPUOptimizer {
    private static final String TAG = "GPUOptimizer";
    
    // GPU sysfs paths - these vary by device and GPU vendor
    private static final String[] POSSIBLE_GPU_PATHS = {
        "/sys/class/kgsl/kgsl-3d0/",                // Qualcomm Adreno
        "/sys/devices/platform/kgsl-3d0.0/kgsl/kgsl-3d0/",  // Alternate Adreno
        "/sys/class/devfreq/gpufreq/",              // Mali
        "/sys/devices/platform/mali.0/",            // Alternate Mali
        "/sys/class/misc/mali0/device/",            // Another Mali variant
        "/sys/class/powervr/",                      // PowerVR
    };
    
    // GPU frequency paths
    private static final String[] FREQ_MIN_PATHS = {
        "min_freq",
        "min_pwrlevel",
        "min_clock",
        "dvfs_min_lock",
    };
    
    private static final String[] FREQ_MAX_PATHS = {
        "max_freq",
        "max_pwrlevel",
        "max_clock",
        "dvfs_max_lock",
    };
    
    // GPU governor paths
    private static final String[] GOVERNOR_PATHS = {
        "governor",
        "pwrscale/trustzone/governor",
        "devfreq/governor",
    };
    
    // Optimization levels
    public static final int LEVEL_LOW = 0;      // Battery saving
    public static final int LEVEL_MEDIUM = 1;   // Balanced
    public static final int LEVEL_HIGH = 2;     // Performance
    public static final int LEVEL_EXTREME = 3;  // Maximum performance
    
    private String mGpuBasePath;
    private String mMinFreqPath;
    private String mMaxFreqPath;
    private String mGovernorPath;
    
    private Map<String, String> mOriginalSettings;
    
    public GPUOptimizer() {
        mOriginalSettings = new HashMap<>();
        detectGpuPaths();
        
        Log.i(TAG, "GPUOptimizer initialized");
        Log.i(TAG, "GPU base path: " + (mGpuBasePath != null ? mGpuBasePath : "Not found"));
        Log.i(TAG, "Min freq path: " + (mMinFreqPath != null ? mMinFreqPath : "Not found"));
        Log.i(TAG, "Max freq path: " + (mMaxFreqPath != null ? mMaxFreqPath : "Not found"));
        Log.i(TAG, "Governor path: " + (mGovernorPath != null ? mGovernorPath : "Not found"));
    }
    
    /**
     * Apply GPU optimizations based on the specified level
     */
    public void applyOptimizations(int level) {
        Log.i(TAG, "Applying GPU optimizations for level: " + level);
        
        if (mGpuBasePath == null) {
            Log.e(TAG, "Cannot apply GPU optimizations: GPU path not found");
            return;
        }
        
        // Save original settings before applying optimizations
        saveOriginalSettings();
        
        switch (level) {
            case LEVEL_LOW:
                applyBatterySavingProfile();
                break;
            case LEVEL_MEDIUM:
                applyBalancedProfile();
                break;
            case LEVEL_HIGH:
                applyPerformanceProfile();
                break;
            case LEVEL_EXTREME:
                applyExtremePerformanceProfile();
                break;
        }
    }
    
    /**
     * Restore original GPU settings
     */
    public void restoreOriginalSettings() {
        Log.i(TAG, "Restoring original GPU settings");
        
        if (mGpuBasePath == null) {
            Log.e(TAG, "Cannot restore GPU settings: GPU path not found");
            return;
        }
        
        for (Map.Entry<String, String> entry : mOriginalSettings.entrySet()) {
            writeFile(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Save original GPU settings
     */
    private void saveOriginalSettings() {
        if (mMinFreqPath != null) {
            String minFreq = readFile(mMinFreqPath);
            if (minFreq != null) {
                mOriginalSettings.put(mMinFreqPath, minFreq);
            }
        }
        
        if (mMaxFreqPath != null) {
            String maxFreq = readFile(mMaxFreqPath);
            if (maxFreq != null) {
                mOriginalSettings.put(mMaxFreqPath, maxFreq);
            }
        }
        
        if (mGovernorPath != null) {
            String governor = readFile(mGovernorPath);
            if (governor != null) {
                mOriginalSettings.put(mGovernorPath, governor);
            }
        }
    }
    
    /**
     * Apply battery saving profile
     * - Lower max frequency
     * - Power-saving governor
     */
    private void applyBatterySavingProfile() {
        Log.i(TAG, "Applying battery saving GPU profile");
        
        // Set governor to powersave if available
        if (mGovernorPath != null) {
            writeFile(mGovernorPath, "powersave");
        }
        
        // Limit max frequency if possible
        if (mMaxFreqPath != null && mMinFreqPath != null) {
            // For Adreno, higher pwrlevel value means lower frequency
            if (mGpuBasePath.contains("kgsl")) {
                // Set to a higher power level (lower frequency)
                writeFile(mMaxFreqPath, "3"); // Higher number = lower frequency
            } else {
                // For other GPUs, try to set to 60% of max frequency
                String maxFreq = readFile(mMaxFreqPath);
                String minFreq = readFile(mMinFreqPath);
                
                if (maxFreq != null && minFreq != null) {
                    try {
                        long max = Long.parseLong(maxFreq.trim());
                        long min = Long.parseLong(minFreq.trim());
                        long target = min + (long)((max - min) * 0.6);
                        writeFile(mMaxFreqPath, String.valueOf(target));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing GPU frequencies", e);
                    }
                }
            }
        }
    }
    
    /**
     * Apply balanced profile
     * - Normal frequency range
     * - Balanced governor
     */
    private void applyBalancedProfile() {
        Log.i(TAG, "Applying balanced GPU profile");
        
        // Set governor to msm-adreno-tz or simple_ondemand if available
        if (mGovernorPath != null) {
            if (mGpuBasePath.contains("kgsl")) {
                writeFile(mGovernorPath, "msm-adreno-tz");
            } else {
                writeFile(mGovernorPath, "simple_ondemand");
            }
        }
        
        // Reset to default frequency range
        if (mMaxFreqPath != null && mMinFreqPath != null) {
            // For Adreno
            if (mGpuBasePath.contains("kgsl")) {
                writeFile(mMinFreqPath, "7"); // Lower number = higher frequency
                writeFile(mMaxFreqPath, "0");
            } else {
                // Try to read available frequencies and set accordingly
                // This is a simplified approach
                String maxFreq = readFile(mMaxFreqPath.replace("dvfs_max_lock", "available_frequencies"));
                String minFreq = readFile(mMinFreqPath.replace("dvfs_min_lock", "available_frequencies"));
                
                if (maxFreq != null && minFreq != null) {
                    String[] freqs = maxFreq.split("\\s+");
                    if (freqs.length > 0) {
                        writeFile(mMinFreqPath, freqs[0]);
                        writeFile(mMaxFreqPath, freqs[freqs.length - 1]);
                    }
                }
            }
        }
    }
    
    /**
     * Apply performance profile
     * - Higher min frequency
     * - Max frequency at 100%
     * - Performance-oriented governor
     */
    private void applyPerformanceProfile() {
        Log.i(TAG, "Applying performance GPU profile");
        
        // Set governor to performance if available
        if (mGovernorPath != null) {
            writeFile(mGovernorPath, "performance");
        }
        
        // Set frequency range for performance
        if (mMaxFreqPath != null && mMinFreqPath != null) {
            // For Adreno
            if (mGpuBasePath.contains("kgsl")) {
                writeFile(mMinFreqPath, "5"); // Set min to a moderate level
                writeFile(mMaxFreqPath, "0"); // Set max to highest
            } else {
                // Try to read available frequencies and set accordingly
                String maxFreq = readFile(mMaxFreqPath.replace("dvfs_max_lock", "available_frequencies"));
                
                if (maxFreq != null) {
                    String[] freqs = maxFreq.split("\\s+");
                    if (freqs.length > 0) {
                        int minIndex = Math.max(0, freqs.length / 3); // Set min to ~33% of max
                        writeFile(mMinFreqPath, freqs[minIndex]);
                        writeFile(mMaxFreqPath, freqs[freqs.length - 1]);
                    }
                }
            }
        }
    }
    
    /**
     * Apply extreme performance profile
     * - High min frequency
     * - Max frequency at 100%
     * - Performance governor
     */
    private void applyExtremePerformanceProfile() {
        Log.i(TAG, "Applying extreme performance GPU profile");
        
        // Set governor to performance if available
        if (mGovernorPath != null) {
            writeFile(mGovernorPath, "performance");
        }
        
        // Set frequency range for extreme performance
        if (mMaxFreqPath != null && mMinFreqPath != null) {
            // For Adreno
            if (mGpuBasePath.contains("kgsl")) {
                writeFile(mMinFreqPath, "3"); // Set min to a high level
                writeFile(mMaxFreqPath, "0"); // Set max to highest
            } else {
                // Try to read available frequencies and set accordingly
                String maxFreq = readFile(mMaxFreqPath.replace("dvfs_max_lock", "available_frequencies"));
                
                if (maxFreq != null) {
                    String[] freqs = maxFreq.split("\\s+");
                    if (freqs.length > 0) {
                        int minIndex = Math.max(0, freqs.length / 2); // Set min to ~50% of max
                        writeFile(mMinFreqPath, freqs[minIndex]);
                        writeFile(mMaxFreqPath, freqs[freqs.length - 1]);
                    }
                }
            }
        }
    }
    
    /**
     * Detect GPU paths based on device
     */
    private void detectGpuPaths() {
        // Find GPU base path
        for (String path : POSSIBLE_GPU_PATHS) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                mGpuBasePath = path;
                break;
            }
        }
        
        if (mGpuBasePath == null) {
            Log.e(TAG, "Could not find GPU path");
            return;
        }
        
        // Find min frequency path
        for (String subPath : FREQ_MIN_PATHS) {
            String fullPath = mGpuBasePath + subPath;
            File file = new File(fullPath);
            if (file.exists() && file.canRead()) {
                mMinFreqPath = fullPath;
                break;
            }
        }
        
        // Find max frequency path
        for (String subPath : FREQ_MAX_PATHS) {
            String fullPath = mGpuBasePath + subPath;
            File file = new File(fullPath);
            if (file.exists() && file.canRead()) {
                mMaxFreqPath = fullPath;
                break;
            }
        }
        
        // Find governor path
        for (String subPath : GOVERNOR_PATHS) {
            String fullPath = mGpuBasePath + subPath;
            File file = new File(fullPath);
            if (file.exists() && file.canRead()) {
                mGovernorPath = fullPath;
                break;
            }
        }
    }
    
    /**
     * Read content from a file
     */
    private String readFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.canRead()) {
                return null;
            }
            
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String content = reader.readLine();
            reader.close();
            return content;
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + filePath, e);
            return null;
        }
    }
    
    /**
     * Write content to a file
     */
    private boolean writeFile(String filePath, String content) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.canWrite()) {
                Log.e(TAG, "Cannot write to file: " + filePath);
                return false;
            }
            
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing to file: " + filePath, e);
            return false;
        }
    }
}
