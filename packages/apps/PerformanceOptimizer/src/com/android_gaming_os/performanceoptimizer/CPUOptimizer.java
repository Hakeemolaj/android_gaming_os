package com.android_gaming_os.performanceoptimizer;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class responsible for optimizing CPU performance.
 * Handles CPU frequency scaling, governor settings, and core management.
 */
public class CPUOptimizer {
    private static final String TAG = "CPUOptimizer";
    
    // CPU sysfs paths
    private static final String CPU_BASE_PATH = "/sys/devices/system/cpu/";
    private static final String CPU_ONLINE_PATH = CPU_BASE_PATH + "cpu%d/online";
    private static final String CPU_GOVERNOR_PATH = CPU_BASE_PATH + "cpu%d/cpufreq/scaling_governor";
    private static final String CPU_MIN_FREQ_PATH = CPU_BASE_PATH + "cpu%d/cpufreq/scaling_min_freq";
    private static final String CPU_MAX_FREQ_PATH = CPU_BASE_PATH + "cpu%d/cpufreq/scaling_max_freq";
    private static final String CPU_AVAILABLE_GOVERNORS_PATH = CPU_BASE_PATH + "cpu0/cpufreq/scaling_available_governors";
    private static final String CPU_AVAILABLE_FREQUENCIES_PATH = CPU_BASE_PATH + "cpu0/cpufreq/scaling_available_frequencies";
    
    // CPU governors
    private static final String GOVERNOR_PERFORMANCE = "performance";
    private static final String GOVERNOR_POWERSAVE = "powersave";
    private static final String GOVERNOR_ONDEMAND = "ondemand";
    private static final String GOVERNOR_INTERACTIVE = "interactive";
    private static final String GOVERNOR_SCHEDUTIL = "schedutil";
    
    // Optimization levels
    public static final int LEVEL_LOW = 0;      // Battery saving
    public static final int LEVEL_MEDIUM = 1;   // Balanced
    public static final int LEVEL_HIGH = 2;     // Performance
    public static final int LEVEL_EXTREME = 3;  // Maximum performance
    
    private int mNumCores;
    private List<String> mAvailableGovernors;
    private Map<Integer, String> mOriginalGovernors;
    private Map<Integer, String> mOriginalMinFreqs;
    private Map<Integer, String> mOriginalMaxFreqs;
    private Map<Integer, Boolean> mOriginalOnlineStatus;
    
    public CPUOptimizer() {
        mNumCores = getNumCores();
        mAvailableGovernors = getAvailableGovernors();
        mOriginalGovernors = new HashMap<>();
        mOriginalMinFreqs = new HashMap<>();
        mOriginalMaxFreqs = new HashMap<>();
        mOriginalOnlineStatus = new HashMap<>();
        
        Log.i(TAG, "CPUOptimizer initialized with " + mNumCores + " cores");
        Log.i(TAG, "Available governors: " + mAvailableGovernors);
    }
    
    /**
     * Apply CPU optimizations based on the specified level
     */
    public void applyOptimizations(int level) {
        Log.i(TAG, "Applying CPU optimizations for level: " + level);
        
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
     * Restore original CPU settings
     */
    public void restoreOriginalSettings() {
        Log.i(TAG, "Restoring original CPU settings");
        
        for (int core = 0; core < mNumCores; core++) {
            if (mOriginalGovernors.containsKey(core)) {
                setGovernor(core, mOriginalGovernors.get(core));
            }
            
            if (mOriginalMinFreqs.containsKey(core)) {
                setMinFrequency(core, mOriginalMinFreqs.get(core));
            }
            
            if (mOriginalMaxFreqs.containsKey(core)) {
                setMaxFrequency(core, mOriginalMaxFreqs.get(core));
            }
            
            if (mOriginalOnlineStatus.containsKey(core) && core > 0) {
                setCoreOnline(core, mOriginalOnlineStatus.get(core));
            }
        }
    }
    
    /**
     * Save original CPU settings
     */
    private void saveOriginalSettings() {
        for (int core = 0; core < mNumCores; core++) {
            mOriginalGovernors.put(core, getGovernor(core));
            mOriginalMinFreqs.put(core, getMinFrequency(core));
            mOriginalMaxFreqs.put(core, getMaxFrequency(core));
            
            if (core > 0) { // Core 0 is always online
                mOriginalOnlineStatus.put(core, isCoreOnline(core));
            }
        }
    }
    
    /**
     * Apply battery saving profile
     * - Use powersave governor
     * - Limit max frequency
     * - Disable some cores
     */
    private void applyBatterySavingProfile() {
        Log.i(TAG, "Applying battery saving CPU profile");
        
        // Use powersave governor on all cores
        String governor = getBestAvailableGovernor(GOVERNOR_POWERSAVE, GOVERNOR_ONDEMAND);
        for (int core = 0; core < mNumCores; core++) {
            setGovernor(core, governor);
        }
        
        // Limit max frequency to 60% of max
        List<String> freqs = getAvailableFrequencies();
        if (!freqs.isEmpty()) {
            int maxIndex = (int) (freqs.size() * 0.6);
            if (maxIndex < freqs.size()) {
                String limitedFreq = freqs.get(maxIndex);
                for (int core = 0; core < mNumCores; core++) {
                    setMaxFrequency(core, limitedFreq);
                }
            }
        }
        
        // Disable half of the cores if we have more than 2
        if (mNumCores > 2) {
            for (int core = mNumCores / 2; core < mNumCores; core++) {
                setCoreOnline(core, false);
            }
        }
    }
    
    /**
     * Apply balanced profile
     * - Use ondemand/interactive governor
     * - Normal frequency range
     * - All cores enabled
     */
    private void applyBalancedProfile() {
        Log.i(TAG, "Applying balanced CPU profile");
        
        // Use balanced governor on all cores
        String governor = getBestAvailableGovernor(GOVERNOR_INTERACTIVE, GOVERNOR_ONDEMAND, GOVERNOR_SCHEDUTIL);
        for (int core = 0; core < mNumCores; core++) {
            setGovernor(core, governor);
        }
        
        // Use full frequency range
        List<String> freqs = getAvailableFrequencies();
        if (!freqs.isEmpty()) {
            String minFreq = freqs.get(0);
            String maxFreq = freqs.get(freqs.size() - 1);
            
            for (int core = 0; core < mNumCores; core++) {
                setMinFrequency(core, minFreq);
                setMaxFrequency(core, maxFreq);
            }
        }
        
        // Enable all cores
        for (int core = 0; core < mNumCores; core++) {
            setCoreOnline(core, true);
        }
    }
    
    /**
     * Apply performance profile
     * - Use performance governor
     * - Higher min frequency
     * - Max frequency at 100%
     * - All cores enabled
     */
    private void applyPerformanceProfile() {
        Log.i(TAG, "Applying performance CPU profile");
        
        // Use performance governor on all cores
        String governor = getBestAvailableGovernor(GOVERNOR_PERFORMANCE, GOVERNOR_INTERACTIVE);
        for (int core = 0; core < mNumCores; core++) {
            setGovernor(core, governor);
        }
        
        // Set min frequency to 30% of max
        List<String> freqs = getAvailableFrequencies();
        if (!freqs.isEmpty()) {
            int minIndex = (int) (freqs.size() * 0.3);
            String minFreq = freqs.get(minIndex);
            String maxFreq = freqs.get(freqs.size() - 1);
            
            for (int core = 0; core < mNumCores; core++) {
                setMinFrequency(core, minFreq);
                setMaxFrequency(core, maxFreq);
            }
        }
        
        // Enable all cores
        for (int core = 0; core < mNumCores; core++) {
            setCoreOnline(core, true);
        }
    }
    
    /**
     * Apply extreme performance profile
     * - Use performance governor
     * - High min frequency
     * - Max frequency at 100%
     * - All cores enabled
     */
    private void applyExtremePerformanceProfile() {
        Log.i(TAG, "Applying extreme performance CPU profile");
        
        // Use performance governor on all cores
        String governor = getBestAvailableGovernor(GOVERNOR_PERFORMANCE);
        for (int core = 0; core < mNumCores; core++) {
            setGovernor(core, governor);
        }
        
        // Set min frequency to 50% of max
        List<String> freqs = getAvailableFrequencies();
        if (!freqs.isEmpty()) {
            int minIndex = (int) (freqs.size() * 0.5);
            String minFreq = freqs.get(minIndex);
            String maxFreq = freqs.get(freqs.size() - 1);
            
            for (int core = 0; core < mNumCores; core++) {
                setMinFrequency(core, minFreq);
                setMaxFrequency(core, maxFreq);
            }
        }
        
        // Enable all cores
        for (int core = 0; core < mNumCores; core++) {
            setCoreOnline(core, true);
        }
    }
    
    /**
     * Get the best available governor from the provided options
     */
    private String getBestAvailableGovernor(String... preferredGovernors) {
        for (String governor : preferredGovernors) {
            if (mAvailableGovernors.contains(governor)) {
                return governor;
            }
        }
        
        // Default to the first available governor if none of the preferred ones are available
        return mAvailableGovernors.isEmpty() ? GOVERNOR_ONDEMAND : mAvailableGovernors.get(0);
    }
    
    /**
     * Get the number of CPU cores
     */
    private int getNumCores() {
        int cores = 0;
        while (new File(String.format(CPU_BASE_PATH + "cpu%d", cores)).exists()) {
            cores++;
        }
        return Math.max(1, cores); // Ensure at least 1 core
    }
    
    /**
     * Get available CPU governors
     */
    private List<String> getAvailableGovernors() {
        List<String> governors = new ArrayList<>();
        String content = readFile(CPU_AVAILABLE_GOVERNORS_PATH);
        
        if (content != null) {
            for (String governor : content.split("\\s+")) {
                if (!governor.trim().isEmpty()) {
                    governors.add(governor.trim());
                }
            }
        }
        
        // Add default governors if none were found
        if (governors.isEmpty()) {
            governors.add(GOVERNOR_ONDEMAND);
            governors.add(GOVERNOR_PERFORMANCE);
            governors.add(GOVERNOR_POWERSAVE);
        }
        
        return governors;
    }
    
    /**
     * Get available CPU frequencies
     */
    private List<String> getAvailableFrequencies() {
        List<String> frequencies = new ArrayList<>();
        String content = readFile(CPU_AVAILABLE_FREQUENCIES_PATH);
        
        if (content != null) {
            for (String freq : content.split("\\s+")) {
                if (!freq.trim().isEmpty()) {
                    frequencies.add(freq.trim());
                }
            }
        }
        
        return frequencies;
    }
    
    /**
     * Get current governor for a CPU core
     */
    private String getGovernor(int core) {
        return readFile(String.format(CPU_GOVERNOR_PATH, core));
    }
    
    /**
     * Set governor for a CPU core
     */
    private boolean setGovernor(int core, String governor) {
        return writeFile(String.format(CPU_GOVERNOR_PATH, core), governor);
    }
    
    /**
     * Get minimum frequency for a CPU core
     */
    private String getMinFrequency(int core) {
        return readFile(String.format(CPU_MIN_FREQ_PATH, core));
    }
    
    /**
     * Set minimum frequency for a CPU core
     */
    private boolean setMinFrequency(int core, String frequency) {
        return writeFile(String.format(CPU_MIN_FREQ_PATH, core), frequency);
    }
    
    /**
     * Get maximum frequency for a CPU core
     */
    private String getMaxFrequency(int core) {
        return readFile(String.format(CPU_MAX_FREQ_PATH, core));
    }
    
    /**
     * Set maximum frequency for a CPU core
     */
    private boolean setMaxFrequency(int core, String frequency) {
        return writeFile(String.format(CPU_MAX_FREQ_PATH, core), frequency);
    }
    
    /**
     * Check if a CPU core is online
     */
    private boolean isCoreOnline(int core) {
        if (core == 0) return true; // Core 0 is always online
        
        String status = readFile(String.format(CPU_ONLINE_PATH, core));
        return status != null && status.trim().equals("1");
    }
    
    /**
     * Set a CPU core online or offline
     */
    private boolean setCoreOnline(int core, boolean online) {
        if (core == 0) return true; // Core 0 is always online
        
        return writeFile(String.format(CPU_ONLINE_PATH, core), online ? "1" : "0");
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
