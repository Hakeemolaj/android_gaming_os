package com.android_gaming_os.gamemode;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "GameModePrefs";
    private static final String KEY_MODE = "mode";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_AUTO_DETECT = "auto_detect";
    private static final String KEY_BLOCK_NOTIFICATIONS = "block_notifications";
    private static final String KEY_LOCK_BRIGHTNESS = "lock_brightness";
    
    private RadioGroup mModeRadioGroup;
    private RadioGroup mProfileRadioGroup;
    private Switch mAutoDetectSwitch;
    private Switch mNotificationsSwitch;
    private Switch mBrightnessSwitch;
    private RecyclerView mGamesRecyclerView;
    private Button mScanGamesButton;
    
    private SharedPreferences mPrefs;
    private GameDetector mGameDetector;
    private List<GameInfo> mGamesList;
    private GamesAdapter mGamesAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Initialize preferences
        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Initialize UI components
        initializeUI();
        
        // Initialize game detector
        mGameDetector = new GameDetector(this);
        
        // Load saved preferences
        loadPreferences();
        
        // Load detected games
        loadDetectedGames();
        
        // Start the GameMode service
        startGameModeService();
    }
    
    private void initializeUI() {
        // Find views
        mModeRadioGroup = findViewById(R.id.mode_radio_group);
        mProfileRadioGroup = findViewById(R.id.profile_radio_group);
        mAutoDetectSwitch = findViewById(R.id.switch_auto_detect);
        mNotificationsSwitch = findViewById(R.id.switch_notifications);
        mBrightnessSwitch = findViewById(R.id.switch_brightness);
        mGamesRecyclerView = findViewById(R.id.games_recycler_view);
        mScanGamesButton = findViewById(R.id.button_scan_games);
        
        // Set up RecyclerView
        mGamesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mGamesList = new ArrayList<>();
        mGamesAdapter = new GamesAdapter(mGamesList);
        mGamesRecyclerView.setAdapter(mGamesAdapter);
        
        // Set up listeners
        mModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.radio_disabled) {
                mode = GameModeService.STATE_DISABLED;
            } else if (checkedId == R.id.radio_enabled) {
                mode = GameModeService.STATE_ENABLED;
            } else {
                mode = GameModeService.STATE_AUTO;
            }
            saveMode(mode);
            updateGameModeService();
        });
        
        mProfileRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int profile;
            if (checkedId == R.id.radio_performance) {
                profile = GameModeService.PROFILE_PERFORMANCE;
            } else if (checkedId == R.id.radio_battery) {
                profile = GameModeService.PROFILE_BATTERY;
            } else {
                profile = GameModeService.PROFILE_BALANCED;
            }
            saveProfile(profile);
            updateGameModeService();
        });
        
        mAutoDetectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveAutoDetect(isChecked);
            updateGameModeService();
        });
        
        mNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveBlockNotifications(isChecked);
        });
        
        mBrightnessSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveLockBrightness(isChecked);
        });
        
        mScanGamesButton.setOnClickListener(v -> {
            scanForGames();
        });
        
        // Set up FAB
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            showAboutDialog();
        });
    }
    
    private void loadPreferences() {
        int mode = mPrefs.getInt(KEY_MODE, GameModeService.STATE_AUTO);
        int profile = mPrefs.getInt(KEY_PROFILE, GameModeService.PROFILE_BALANCED);
        boolean autoDetect = mPrefs.getBoolean(KEY_AUTO_DETECT, true);
        boolean blockNotifications = mPrefs.getBoolean(KEY_BLOCK_NOTIFICATIONS, true);
        boolean lockBrightness = mPrefs.getBoolean(KEY_LOCK_BRIGHTNESS, true);
        
        // Set UI state based on preferences
        switch (mode) {
            case GameModeService.STATE_DISABLED:
                mModeRadioGroup.check(R.id.radio_disabled);
                break;
            case GameModeService.STATE_ENABLED:
                mModeRadioGroup.check(R.id.radio_enabled);
                break;
            case GameModeService.STATE_AUTO:
            default:
                mModeRadioGroup.check(R.id.radio_auto);
                break;
        }
        
        switch (profile) {
            case GameModeService.PROFILE_PERFORMANCE:
                mProfileRadioGroup.check(R.id.radio_performance);
                break;
            case GameModeService.PROFILE_BATTERY:
                mProfileRadioGroup.check(R.id.radio_battery);
                break;
            case GameModeService.PROFILE_BALANCED:
            default:
                mProfileRadioGroup.check(R.id.radio_balanced);
                break;
        }
        
        mAutoDetectSwitch.setChecked(autoDetect);
        mNotificationsSwitch.setChecked(blockNotifications);
        mBrightnessSwitch.setChecked(lockBrightness);
    }
    
    private void saveMode(int mode) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(KEY_MODE, mode);
        editor.apply();
    }
    
    private void saveProfile(int profile) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(KEY_PROFILE, profile);
        editor.apply();
    }
    
    private void saveAutoDetect(boolean autoDetect) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(KEY_AUTO_DETECT, autoDetect);
        editor.apply();
    }
    
    private void saveBlockNotifications(boolean blockNotifications) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(KEY_BLOCK_NOTIFICATIONS, blockNotifications);
        editor.apply();
    }
    
    private void saveLockBrightness(boolean lockBrightness) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(KEY_LOCK_BRIGHTNESS, lockBrightness);
        editor.apply();
    }
    
    private void startGameModeService() {
        Intent intent = new Intent(this, GameModeService.class);
        startService(intent);
    }
    
    private void updateGameModeService() {
        Intent intent = new Intent(this, GameModeService.class);
        
        int mode = mPrefs.getInt(KEY_MODE, GameModeService.STATE_AUTO);
        switch (mode) {
            case GameModeService.STATE_DISABLED:
                intent.setAction("com.android_gaming_os.gamemode.ACTION_DISABLE");
                break;
            case GameModeService.STATE_ENABLED:
                intent.setAction("com.android_gaming_os.gamemode.ACTION_ENABLE");
                break;
            case GameModeService.STATE_AUTO:
                intent.setAction("com.android_gaming_os.gamemode.ACTION_AUTO");
                break;
        }
        
        startService(intent);
        
        // Update profile
        intent = new Intent(this, GameModeService.class);
        intent.setAction("com.android_gaming_os.gamemode.ACTION_SET_PROFILE");
        intent.putExtra("profile", mPrefs.getInt(KEY_PROFILE, GameModeService.PROFILE_BALANCED));
        startService(intent);
        
        // Update auto-detect
        intent = new Intent(this, GameModeService.class);
        intent.setAction("com.android_gaming_os.gamemode.ACTION_SET_AUTO_DETECT");
        intent.putExtra("auto_detect", mPrefs.getBoolean(KEY_AUTO_DETECT, true));
        startService(intent);
    }
    
    private void loadDetectedGames() {
        // This would normally load from a database or shared preferences
        // For now, we'll just create some sample data
        mGamesList.clear();
        mGamesList.add(new GameInfo("PUBG Mobile", "com.tencent.ig", true));
        mGamesList.add(new GameInfo("Call of Duty Mobile", "com.activision.callofduty.shooter", true));
        mGamesList.add(new GameInfo("Minecraft", "com.mojang.minecraftpe", true));
        mGamesAdapter.notifyDataSetChanged();
    }
    
    private void scanForGames() {
        // This would normally scan the device for installed games
        // For now, we'll just show a toast
        Toast.makeText(this, "Scanning for games...", Toast.LENGTH_SHORT).show();
        
        // Simulate finding a new game
        mGamesList.add(new GameInfo("Fortnite", "com.epicgames.fortnite", true));
        mGamesAdapter.notifyDataSetChanged();
        
        Toast.makeText(this, "Found 1 new game", Toast.LENGTH_SHORT).show();
    }
    
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_about_title)
            .setMessage(R.string.dialog_about_message)
            .setPositiveButton(R.string.dialog_ok, null)
            .show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            // Open settings activity
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    // Simple class to hold game information
    public static class GameInfo {
        public String name;
        public String packageName;
        public boolean enabled;
        
        public GameInfo(String name, String packageName, boolean enabled) {
            this.name = name;
            this.packageName = packageName;
            this.enabled = enabled;
        }
    }
    
    // Adapter for the games list
    private class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.ViewHolder> {
        private List<GameInfo> mGames;
        
        public GamesAdapter(List<GameInfo> games) {
            mGames = games;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            GameInfo game = mGames.get(position);
            holder.text1.setText(game.name);
            holder.text2.setText(game.packageName);
        }
        
        @Override
        public int getItemCount() {
            return mGames.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView text1;
            android.widget.TextView text2;
            
            public ViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
