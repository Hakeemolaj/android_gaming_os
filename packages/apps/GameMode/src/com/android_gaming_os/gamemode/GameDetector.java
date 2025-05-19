package com.android_gaming_os.gamemode;

import android.app.ActivityManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class responsible for detecting when games are launched and closed.
 */
public class GameDetector {
    private static final String TAG = "GameDetector";
    
    // Polling interval for checking foreground apps (in milliseconds)
    private static final long POLLING_INTERVAL = 2000;
    
    private final Context mContext;
    private final Handler mHandler;
    private final Set<String> mKnownGamePackages;
    private final List<GameListener> mListeners;
    private String mCurrentGamePackage;
    private boolean mIsRunning;
    
    /**
     * Interface for listening to game state changes
     */
    public interface GameListener {
        void onGameStarted(String packageName);
        void onGameStopped(String packageName);
    }
    
    public GameDetector(Context context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mKnownGamePackages = new HashSet<>();
        mListeners = new ArrayList<>();
        mCurrentGamePackage = null;
        mIsRunning = false;
        
        // Initialize the list of known game packages
        initializeKnownGames();
    }
    
    /**
     * Start monitoring for game launches
     */
    public void startMonitoring() {
        if (!mIsRunning) {
            mIsRunning = true;
            mHandler.post(mGameCheckRunnable);
            Log.i(TAG, "Game detection started");
        }
    }
    
    /**
     * Stop monitoring for game launches
     */
    public void stopMonitoring() {
        if (mIsRunning) {
            mIsRunning = false;
            mHandler.removeCallbacks(mGameCheckRunnable);
            Log.i(TAG, "Game detection stopped");
            
            // If a game was running, notify that it stopped
            if (mCurrentGamePackage != null) {
                notifyGameStopped(mCurrentGamePackage);
                mCurrentGamePackage = null;
            }
        }
    }
    
    /**
     * Add a listener for game state changes
     */
    public void addListener(GameListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }
    
    /**
     * Remove a listener for game state changes
     */
    public void removeListener(GameListener listener) {
        mListeners.remove(listener);
    }
    
    /**
     * Initialize the list of known game packages
     */
    private void initializeKnownGames() {
        // Add known game packages
        // This could be loaded from a database or configuration file
        mKnownGamePackages.add("com.tencent.ig");  // PUBG Mobile
        mKnownGamePackages.add("com.epicgames.fortnite");  // Fortnite
        mKnownGamePackages.add("com.supercell.clashofclans");  // Clash of Clans
        mKnownGamePackages.add("com.mojang.minecraftpe");  // Minecraft
        mKnownGamePackages.add("com.activision.callofduty.shooter");  // Call of Duty Mobile
        
        // Detect games based on category
        detectGamesByCategory();
    }
    
    /**
     * Detect games based on their category in the Play Store
     */
    private void detectGamesByCategory() {
        PackageManager pm = mContext.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        
        for (ApplicationInfo app : apps) {
            try {
                // Check if the app is a game
                if ((app.category == ApplicationInfo.CATEGORY_GAME) || 
                    (app.metaData != null && app.metaData.getBoolean("isGame", false))) {
                    mKnownGamePackages.add(app.packageName);
                    Log.d(TAG, "Detected game: " + app.packageName);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error detecting game: " + e.getMessage());
            }
        }
    }
    
    /**
     * Runnable that checks for foreground games
     */
    private final Runnable mGameCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsRunning) {
                checkForForegroundGame();
                mHandler.postDelayed(this, POLLING_INTERVAL);
            }
        }
    };
    
    /**
     * Check if a game is in the foreground
     */
    private void checkForForegroundGame() {
        String foregroundPackage = getForegroundPackage();
        
        if (foregroundPackage != null) {
            boolean isGame = mKnownGamePackages.contains(foregroundPackage);
            
            // If not in our known list, check if it might be a game
            if (!isGame) {
                isGame = checkIfPackageIsGame(foregroundPackage);
                if (isGame) {
                    // Add to our known list for future reference
                    mKnownGamePackages.add(foregroundPackage);
                }
            }
            
            // Handle game state changes
            if (isGame) {
                if (mCurrentGamePackage == null || !mCurrentGamePackage.equals(foregroundPackage)) {
                    // A new game has started
                    if (mCurrentGamePackage != null) {
                        notifyGameStopped(mCurrentGamePackage);
                    }
                    mCurrentGamePackage = foregroundPackage;
                    notifyGameStarted(mCurrentGamePackage);
                }
            } else if (mCurrentGamePackage != null) {
                // The game is no longer in the foreground
                notifyGameStopped(mCurrentGamePackage);
                mCurrentGamePackage = null;
            }
        } else if (mCurrentGamePackage != null) {
            // No foreground app, but we had a game running
            notifyGameStopped(mCurrentGamePackage);
            mCurrentGamePackage = null;
        }
    }
    
    /**
     * Get the package name of the foreground app
     */
    private String getForegroundPackage() {
        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            UsageEvents events = usageStatsManager.queryEvents(time - 10000, time);
            UsageEvents.Event event = new UsageEvents.Event();
            String packageName = null;
            
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    packageName = event.getPackageName();
                }
            }
            
            return packageName;
        } catch (Exception e) {
            Log.e(TAG, "Error getting foreground package: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if a package is likely to be a game
     */
    private boolean checkIfPackageIsGame(String packageName) {
        try {
            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            
            // Check category
            if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                return true;
            }
            
            // Check metadata
            if (appInfo.metaData != null && appInfo.metaData.getBoolean("isGame", false)) {
                return true;
            }
            
            // Check if the app name contains "game" or similar keywords
            String appName = pm.getApplicationLabel(appInfo).toString().toLowerCase();
            return appName.contains("game") || appName.contains("play") || 
                   appName.contains("racing") || appName.contains("shooter");
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking if package is game: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Notify listeners that a game has started
     */
    private void notifyGameStarted(String packageName) {
        Log.i(TAG, "Game started: " + packageName);
        for (GameListener listener : mListeners) {
            listener.onGameStarted(packageName);
        }
    }
    
    /**
     * Notify listeners that a game has stopped
     */
    private void notifyGameStopped(String packageName) {
        Log.i(TAG, "Game stopped: " + packageName);
        for (GameListener listener : mListeners) {
            listener.onGameStopped(packageName);
        }
    }
}
