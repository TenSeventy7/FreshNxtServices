package io.tensevntysevn.fresh.zest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.preference.ListPreference;
import de.dlyt.yanndroid.oneui.preference.Preference;
import de.dlyt.yanndroid.oneui.layout.PreferenceFragment;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.preference.SwitchPreferenceScreen;
import de.dlyt.yanndroid.oneui.preference.internal.PreferencesRelatedCard;
import io.tensevntysevn.fresh.ExperienceUtils;
import io.tensevntysevn.fresh.R;
import io.tensevntysevn.fresh.renoir.RenoirService;
import io.tensevntysevn.fresh.zest.sub.RenoirSettingsActivity;
import io.tensevntysevn.fresh.services.OverlayService;
import io.tensevntysevn.fresh.utils.Preferences;
import io.tensevntysevn.fresh.zest.sub.MaverickSettingsActivity;
import io.tensevntysevn.fresh.zest.sub.ScreenResolutionActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String TITLE_TAG = "settingsActivityTitle";

    @BindView(R.id.zest_main_toolbar)
    ToolbarLayout toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zest_activity_main);
        ButterKnife.bind(this);

        toolbar.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        toolbar.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar.getToolbar());
        RenoirService.setupCustomizationNotifChannel(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.zest_settings_main_layout, new ZestMainFragment())
                    .commit();
        } else {
            toolbar.setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        toolbar.inflateToolbarMenu(R.menu.settings_search);
        toolbar.setOnToolbarMenuItemClickListener(this::onOptionsItemSelected);
        return true;
    }

    private boolean onOptionsItemSelected(de.dlyt.yanndroid.oneui.menu.MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.zest_settings_shortcut) {
            ComponentName cn = new ComponentName("com.android.settings.intelligence", "com.android.settings.intelligence.search.SearchActivity");
            Intent intent = new Intent();
            intent.setComponent(cn);
            this.startActivity(intent);
        }
        return true;
    }

    public static class ZestMainFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        private Context mContext;
        private static ExecutorService mExecutor;
        private PreferencesRelatedCard mRelatedCard;

        private static boolean mBackground = false;
        private static Handler mHandler;

        boolean setRenoir = false;
        boolean disableRenoir = false;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mContext = getContext();
            mExecutor = Executors.newCachedThreadPool();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.zest_activity_main_preferences, rootKey);
        }


        @Override
        public void onStart() {
            super.onStart();

            // Get activated color from attr, so it changes based on the app's theme
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = mContext.getTheme();
            theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true);
            @ColorInt int summaryColor = typedValue.data;

            findPreference("sb_icon_style_data").seslSetSummaryColor(summaryColor);
            findPreference("sb_icon_style_wifi").seslSetSummaryColor(summaryColor);
            findPreference("fs_device_resolution").seslSetSummaryColor(summaryColor);

            String setResolution = getResolution(mContext);
            String romVersion = getRomVersion();
            String appVersion = getAppVersion(mContext);
            boolean vbEnabled = ExperienceUtils.isVideoEnhancerEnabled(mContext);
            setRenoir = RenoirService.getRenoirEnabled(mContext);
            boolean mvEnabled = MaverickSettingsActivity.getMaverickState(mContext);
            Preference.OnPreferenceClickListener easterEgg = getVersionEgg(mContext);

            // Color theme
            if (ExperienceUtils.isGalaxyThemeApplied(mContext)) findPreference("fs_color_theme").setSummary(R.string.zest_renoir_settings_unavailable);
            findPreference("fs_color_theme").setEnabled(!ExperienceUtils.isGalaxyThemeApplied(mContext));
            ((SwitchPreferenceScreen) findPreference("fs_color_theme")).setChecked(setRenoir);
            findPreference("fs_color_theme").setOnPreferenceChangeListener(this);

            // System UI icons
            setIconSummary();
            findPreference("sb_icon_style_data").setOnPreferenceChangeListener(this);
            findPreference("sb_icon_style_wifi").setOnPreferenceChangeListener(this);

            // Video preferences
            ((SwitchPreferenceScreen) findPreference("fs_video_brightness")).setChecked(vbEnabled);
            findPreference("fs_video_brightness").setOnPreferenceChangeListener(this);

            /*
            // USB protection
            ((SwitchPreferenceScreen) findPreference("fs_plus_usb_security")).setChecked(mvEnabled);
            findPreference("fs_plus_usb_security").setOnPreferenceChangeListener(this);
             */

            // Screen resolution
            findPreference("fs_device_resolution").setSummary(setResolution);

            // Fresh and Fresh Services versions
            findPreference("zs_fresh_version").setSummary(romVersion);
            findPreference("zs_fresh_version").setOnPreferenceClickListener(easterEgg);
            findPreference("zs_about_fresh_services").setSummary(appVersion);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String prefKey = preference.getKey();
            Handler mHandler = new Handler(Looper.getMainLooper());

            switch (prefKey) {
                case "fs_color_theme":
                    boolean isChecked = (boolean) newValue;
                    if (!(isChecked == setRenoir) && !mBackground) {
                        mBackground = true;
                        preference.setEnabled(false);

                        RenoirService.setRenoirEnabled(mContext, isChecked);

                        mHandler.postDelayed(() -> {
                            mBackground = false;
                            preference.setEnabled(true);
                        }, 1500);
                    }
                    return true;
                case "sb_icon_style_data":
                case "sb_icon_style_wifi":
                    String[] dataIconPackages = this.getResources().getStringArray(R.array.data_connection_icon_packages);
                    String[] wlanIconPackages = this.getResources().getStringArray(R.array.data_connection_icon_packages);
                    String setIcon;

                    if (prefKey.equals("sb_icon_style_data")) {
                        setIcon = Preferences.getDataConnectionIconPackage(mContext);
                        if (setIcon == null)
                            setIcon = dataIconPackages[0];
                    } else {
                        setIcon = Preferences.getWlanConnectionIconPackage(mContext);
                        if (setIcon == null)
                            setIcon = wlanIconPackages[0];
                    }

                    Scanner oldScanner = new Scanner(setIcon);
                    Scanner newScanner = new Scanner(newValue.toString());
                    String scannerDelimit = ":";
                    oldScanner.useDelimiter(scannerDelimit);
                    newScanner.useDelimiter(scannerDelimit);

                    String oldPackage = oldScanner.next();
                    String oldPackageDeX = oldScanner.next();

                    String newPackage = newScanner.next();
                    String newPackageDeX = newScanner.next();

                    if (prefKey.equals("sb_icon_style_data") && !newPackage.equals(oldPackage)) {
                        Preferences.setDataConnectionIconPackage(mContext, newValue.toString());

                        mExecutor.execute(() -> {
                            if (!dataIconPackages[0].contains(newPackage)) {
                                OverlayService.setOverlayState(newPackage, true);
                                OverlayService.setOverlayState(newPackageDeX, true);
                            }

                            if (!dataIconPackages[0].contains(oldPackage)) {
                                OverlayService.setOverlayState(oldPackage, false);
                                OverlayService.setOverlayState(oldPackageDeX, false);
                            }
                        });
                    } else if (prefKey.equals("sb_icon_style_wifi") && !newPackage.equals(oldPackage)) {
                        Preferences.setWlanConnectionIconPackage(mContext, newValue.toString());

                        mExecutor.execute(() -> {
                            if (!wlanIconPackages[0].contains(newPackage)) {
                                OverlayService.setOverlayState(newPackage, true);
                                OverlayService.setOverlayState(newPackageDeX, true);
                            }

                            if (!wlanIconPackages[0].contains(oldPackage)) {
                                OverlayService.setOverlayState(oldPackage, false);
                                OverlayService.setOverlayState(oldPackageDeX, false);
                            }
                        });

                    }

                    setIconSummary();
                    return true;
                case "fs_video_brightness":
                    ExperienceUtils.setVideoEnhancerEnabled(mContext, (boolean) newValue);
                    return true;
                    /*
                case "fs_plus_usb_security":
                    MaverickSettingsActivity.setMaverickState(mContext, (boolean) newValue);
                    return true;

                     */
            }
            return false;
        }

        @Override
        public void onResume() {
            super.onResume();
            // Color theme
            ((SwitchPreferenceScreen) findPreference("fs_color_theme")).setChecked(setRenoir);
            makeRelatedCard(mContext);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getView().setBackgroundColor(getResources().getColor(R.color.item_background_color, mContext.getTheme()));
        }

        private void makeRelatedCard(Context context) {
            String advancedTitle = getString(R.string.zest_relative_link_advanced);
            String themesTitle = getString(R.string.zest_relative_link_themes);
            String dressroomTitle = getString(R.string.zest_relative_link_wallpaper);

            View.OnClickListener advancedIntent = v -> {
                ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$UsefulFeatureMainActivity");
                Intent intent = new Intent();
                intent.setComponent(cn);
                mContext.startActivity(intent);
            };
            View.OnClickListener themesIntent = v -> {
                ComponentName cn = new ComponentName("com.samsung.android.themestore", "com.samsung.android.themestore.activity.MainActivity");
                Intent intent = new Intent();
                intent.setComponent(cn);
                mContext.startActivity(intent);
            };
            View.OnClickListener dressroomIntent = v -> {
                ComponentName cn = new ComponentName("com.samsung.android.app.dressroom", "com.samsung.android.app.dressroom.presentation.settings.WallpaperSettingActivity");
                Intent intent = new Intent();
                intent.setComponent(cn);
                mContext.startActivity(intent);
            };

            if (mRelatedCard == null) {
                mRelatedCard = createRelatedCard(context);
                mRelatedCard.addButton(advancedTitle, advancedIntent)
                        .addButton(dressroomTitle, dressroomIntent);

                if (!ExperienceUtils.isDesktopMode(mContext)) {
                    mRelatedCard.addButton(themesTitle, themesIntent);
                }

                mRelatedCard.show(this);
            }
        }

        private static String getRomVersion() {
            String romPropVersion = ExperienceUtils.getProp("ro.fresh.version");
            String romPropBuild = ExperienceUtils.getProp("ro.fresh.build.version");
            String romPropBranch = ExperienceUtils.getProp("ro.fresh.build.branch");
            String romPropBuildDate = ExperienceUtils.getProp("ro.fresh.build.date");

            String romVersionBranch = "";
            String buildDate = ExperienceUtils.getProp("ro.system.build.date");

            if (!romPropBuildDate.equals("")) {
                buildDate = ExperienceUtils.getProp("ro.fresh.build.date");
            }

            if (!romPropBranch.isEmpty()) {
                romVersionBranch = romPropBranch.substring(0, 1).toUpperCase() +
                        romPropBranch.substring(1).toLowerCase();
            }

            String romVersion = romPropVersion + " " + romVersionBranch + " " + "(" + romPropBuild + ")";

            return romVersion + "\n" + buildDate;
        }

        private static String getAppVersion(Context context) {
            String appVersion;
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                String versionName = packageInfo.versionName;
                @SuppressWarnings("deprecation") int versionCode = packageInfo.versionCode;
                appVersion = versionName + " (" + versionCode + ")";
            } catch (PackageManager.NameNotFoundException e) {
                appVersion = "Unknown";
            }

            return appVersion;
        }

        private static String getResolution(Context context) {
            String[] mResolutionValues = context.getResources().getStringArray(R.array.zest_screen_resolution_setting_main_summary);
            int setResolution = ScreenResolutionActivity.getResolutionInt(context);
            return mResolutionValues[setResolution];
        }

        private void setIconSummary() {
            String[] suiValues = getSuiValues(mContext);
            ((ListPreference) findPreference("sb_icon_style_data")).setValue(suiValues[0]);
            ((ListPreference) findPreference("sb_icon_style_wifi")).setValue(suiValues[1]);
            CharSequence valData = ((ListPreference) findPreference("sb_icon_style_data")).getEntry();
            CharSequence valWlan = ((ListPreference) findPreference("sb_icon_style_wifi")).getEntry();

            findPreference("sb_icon_style_data").setSummary(valData);
            findPreference("sb_icon_style_wifi").setSummary(valWlan);
        }

        private static String[] getSuiValues(Context context) {
            String[] dataIconPackages = context.getResources().getStringArray(R.array.data_connection_icon_packages);
            String[] wlanIconPackages = context.getResources().getStringArray(R.array.wlan_signal_icon_packages);
            String sDataIcon = Preferences.getDataConnectionIconPackage(context);
            String sWlanIcon = Preferences.getWlanConnectionIconPackage(context);

            if (sDataIcon == null)
                sDataIcon = dataIconPackages[0];
            if (sWlanIcon == null)
                sWlanIcon = wlanIconPackages[0];

            return new String[]{sDataIcon, sWlanIcon};
        }

        private static Preference.OnPreferenceClickListener getVersionEgg(Context context) {
            long[] mHits = new long[3];

            return listener -> {
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
                    String url = "https://www.youtube.com/watch?v=XAg1jDDG49Y";
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    context.startActivity(intent);
                }
                return true;
            };
        }
    }
}