package com.android_gaming_os.performanceoptimizer;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
 * Class responsible for optimizing memory usage.
 * Handles memory management, swappiness, and background app limits.
 */
public class MemoryOptimizer {
    private static final String TAG = "MemoryOptimizer";
    
    // Memory sysfs paths
    private static final String VM_SWAPPINESS_PATH = "/proc/sys/vm/swappiness";
    private static final String VM_VFS_CACHE_PRESSURE_PATH = "/proc/sys/vm/vfs_cache_pressure";
    private static final String VM_DIRTY_RATIO_PATH = "/proc/sys/vm/dirty_ratio";
    private static final String VM_DIRTY_BACKGROUND_RATIO_PATH = "/proc/sys/vm/dirty_background_ratio";
    private static final String VM_MIN_FREE_KBYTES_PATH = "/proc/sys/vm/min_free_kbytes";
    
    // LMK paths
    private static final String LMK_MINFREE_PATH = "/sys/module/lowmemorykiller/parameters/minfree";
    private static final String LMK_ADJ_PATH = "/sys/module/lowmemorykiller/parameters/adj";
    
    // Optimization levels
    public static final int LEVEL_LOW = 0;      // Battery saving
    public static final int LEVEL_MEDIUM = 1;   // Balanced
    public static final int LEVEL_HIGH = 2;     // Performance
    public static final int LEVEL_EXTREME = 3;  // Maximum performance
    
    private final Context mContext;
    private final Handler mHandler;
    private final Map<String, String> mOriginalSettings;
    private boolean mIsRunning;
    private int mCurrentLevel;
    
    // Background app limits for different profiles
    private static final int[] BG_APP_LIMITS = {
        3,  // LEVEL_LOW - Battery saving
        5,  // LEVEL_MEDIUM - Balanced
        10, // LEVEL_HIGH - Performance
        15  // LEVEL_EXTREME - Maximum performance
    };
    
    public MemoryOptimizer(Context context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mOriginalSettings = new HashMap<>();
        mIsRunning = false;
        mCurrentLevel = LEVEL_MEDIUM;
        
        Log.i(TAG, "MemoryOptimizer initialized");
    }
    
    /**
     * Apply memory optimizations based on the specified level
     */
    public void applyOptimizations(int level) {
        Log.i(TAG, "Applying memory optimizations for level: " + level);
        
        // Save original settings before applying optimizations
        saveOriginalSettings();
        
        mCurrentLevel = level;
        
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
        
        // Start periodic memory optimization
        startPeriodicOptimization();
    }
    
    /**
     * Restore original memory settings
     */
    public void restoreOriginalSettings() {
        Log.i(TAG, "Restoring original memory settings");
        
        // Stop periodic optimization
        stopPeriodicOptimization();
        
        // Restore original settings
        for (Map.Entry<String, String> entry : mOriginalSettings.entrySet()) {
            writeFile(entry.getKey(), entry.getValue());
        }
        
        // Reset background app limit
        setBackgroundProcessLimit(-1); // Default
    }
    
    /**
     * Save original memory settings
     */
    private void saveOriginalSettings() {
        saveFileSetting(VM_SWAPPINESS_PATH);
        saveFileSetting(VM_VFS_CACHE_PRESSURE_PATH);
        saveFileSetting(VM_DIRTY_RATIO_PATH);
        saveFileSetting(VM_DIRTY_BACKGROUND_RATIO_PATH);
        saveFileSetting(VM_MIN_FREE_KBYTES_PATH);
        saveFileSetting(LMK_MINFREE_PATH);
        saveFileSetting(LMK_ADJ_PATH);
    }
    
    private void saveFileSetting(String path) {
        String value = readFile(path);
        if (value != null) {
            mOriginalSettings.put(path, value);
        }
    }
    
    /**
     * Apply battery saving profile
     * - Higher swappiness
     * - Lower background app limit
     * - More aggressive LMK
     */
    private void applyBatterySavingProfile() {
        Log.i(TAG, "Applying battery saving memory profile");
        
        // Set swappiness to a higher value to use swap more aggressively
        writeFile(VM_SWAPPINESS_PATH, "80");
        
        // Increase cache pressure to free memory more aggressively
        writeFile(VM_VFS_CACHE_PRESSURE_PATH, "200");
        
        // Set dirty ratios to flush to disk more frequently
        writeFile(VM_DIRTY_RATIO_PATH, "20");
        writeFile(VM_DIRTY_BACKGROUND_RATIO_PATH, "10");
        
        // Set min free kbytes to a moderate value
        writeFile(VM_MIN_FREE_KBYTES_PATH, "4096");
        
        // Set LMK parameters to be more aggressive
        setLowMemoryKiller(true);
        
        // Limit background processes
        setBackgroundProcessLimit(BG_APP_LIMITS[LEVEL_LOW]);
    }
    
    /**
     * Apply balanced profile
     * - Moderate swappiness
     * - Moderate background app limit
     * - Default LMK
     */
    private void applyBalancedProfile() {
        Log.i(TAG, "Applying balanced memory profile");
        
        // Set swappiness to a moderate value
        writeFile(VM_SWAPPINESS_PATH, "60");
        
        // Set cache pressure to default
        writeFile(VM_VFS_CACHE_PRESSURE_PATH, "100");
        
        // Set dirty ratios to default values
        writeFile(VM_DIRTY_RATIO_PATH, "30");
        writeFile(VM_DIRTY_BACKGROUND_RATIO_PATH, "15");
        
        // Set min free kbytes to a moderate value
        writeFile(VM_MIN_FREE_KBYTES_PATH, "8192");
        
        // Set LMK parameters to default
        setLowMemoryKiller(false);
        
        // Set moderate background process limit
        setBackgroundProcessLimit(BG_APP_LIMITS[LEVEL_MEDIUM]);
    }
    
    /**
     * Apply performance profile
     * - Lower swappiness
     * - Higher background app limit
     * - Less aggressive LMK
     */
    private void applyPerformanceProfile() {
        Log.i(TAG, "Applying performance memory profile");
        
        // Set swappiness to a lower value to keep more in RAM
        writeFile(VM_SWAPPINESS_PATH, "40");
        
        // Decrease cache pressure to keep more in cache
        writeFile(VM_VFS_CACHE_PRESSURE_PATH, "50");
        
        // Set dirty ratios to higher values to flush less frequently
        writeFile(VM_DIRTY_RATIO_PATH, "40");
        writeFile(VM_DIRTY_BACKGROUND_RATIO_PATH, "20");
        
        // Set min free kbytes to a higher value
        writeFile(VM_MIN_FREE_KBYTES_PATH, "16384");
        
        // Set LMK parameters to be less aggressive
        setLowMemoryKiller(false);
        
        // Allow more background processes
        setBackgroundProcessLimit(BG_APP_LIMITS[LEVEL_HIGH]);
    }
    
    /**
     * Apply extreme performance profile
     * - Minimal swappiness
     * - Maximum background app limit
     * - Least aggressive LMK
     */
    private void applyExtremePerformanceProfile() {
        Log.i(TAG, "Applying extreme performance memory profile");
        
        // Set swappiness to a very low value to keep as much as possible in RAM
        writeFile(VM_SWAPPINESS_PATH, "10");
        
        // Decrease cache pressure significantly
        writeFile(VM_VFS_CACHE_PRESSURE_PATH, "10");
        
        // Set dirty ratios to high values
        writeFile(VM_DIRTY_RATIO_PATH, "60");
        writeFile(VM_DIRTY_BACKGROUND_RATIO_PATH, "30");
        
        // Set min free kbytes to a high value
        writeFile(VM_MIN_FREE_KBYTES_PATH, "32768");
        
        // Set LMK parameters to be least aggressive
        setLowMemoryKiller(false);
        
        // Allow maximum background processes
        setBackgroundProcessLimit(BG_APP_LIMITS[LEVEL_EXTREME]);
    }
    
    /**
     * Set low memory killer parameters
     * @param aggressive Whether to be more aggressive in killing apps
     */
    private void setLowMemoryKiller(boolean aggressive) {
        // LMK minfree values (in pages)
        // Format: pages at which to kill processes with adj values 0, 1, 2, 4, 9, 15
        String minfree;
        
        if (aggressive) {
            // More aggressive - kill apps earlier
            minfree = "4096,6144,8192,12288,16384,20480";
        } else {
            // Less aggressive - keep apps longer
            minfree = "2048,3072,4096,8192,12288,16384";
        }
        
        writeFile(LMK_MINFREE_PATH, minfree);
        
        // LMK adj values
        // Default is "0,1,2,4,9,15"
        // We'll keep the default
    }
    
    /**
     * Set the background process limit
     */
    private void setBackgroundProcessLimit(int limit) {
        try {
            ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.getClass()
                  .getMethod("setProcessLimit", int.class)
                  .invoke(am, limit);
                
                Log.i(TAG, "Set background process limit to " + limit);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting background process limit", e);
        }
    }
    
    /**
     * Start periodic memory optimization
     */
    private void startPeriodicOptimization() {
        if (!mIsRunning) {
            mIsRunning = true;
            mHandler.postDelayed(mOptimizeRunnable, 60000); // Run every minute
        }
    }
    
    /**
     * Stop periodic memory optimization
     */
    private void stopPeriodicOptimization() {
        mIsRunning = false;
        mHandler.removeCallbacks(mOptimizeRunnable);
    }
    
    /**
     * Runnable that performs periodic memory optimization
     */
    private final Runnable mOptimizeRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsRunning) {
                optimizeMemory();
                mHandler.postDelayed(this, 60000); // Run every minute
            }
        }
    };
    
    /**
     * Perform memory optimization
     */
    private void optimizeMemory() {
        Log.i(TAG, "Performing memory optimization");
        
        // Get memory info
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        
        // Calculate available memory percentage
        long totalMem = memoryInfo.totalMem;
        long availMem = memoryInfo.availMem;
        float memoryPercentage = (float) availMem / totalMem * 100;
        
        Log.i(TAG, "Memory: " + availMem / 1048576 + "MB available out of " + 
                   totalMem / 1048576 + "MB total (" + memoryPercentage + "%)");
        
        // If memory is low, take action based on current level
        if (memoryPercentage < 15) {
            Log.i(TAG, "Memory is low, performing cleanup");
            
            // Kill background processes based on level
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes != null) {
                for (ActivityManager.RunningAppProcessInfo process : processes) {
                    // Only kill background processes
                    if (process.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        // In extreme performance mode, be less aggressive
                        if (mCurrentLevel == LEVEL_EXTREME && 
                            process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                            continue;
                        }
                        
                        // In high performance mode, keep services
                        if (mCurrentLevel == LEVEL_HIGH && 
                            process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                            continue;
                        }
                        
                        // Kill the process
                        if (process.pkgList != null && process.pkgList.length > 0) {
                            for (String pkg : process.pkgList) {
                                Log.i(TAG, "Killing background process: " + pkg);
                                am.killBackgroundProcesses(pkg);
                            }
                        }
                    }
                }
            }
            
            // Force garbage collection
            System.gc();
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
