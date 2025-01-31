package cf.tenseventyseven.fresh.zest.sub;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.dlyt.yanndroid.oneui.layout.PreferenceFragment;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.preference.HorizontalRadioPreference;
import de.dlyt.yanndroid.oneui.preference.Preference;
import cf.tenseventyseven.fresh.R;
import cf.tenseventyseven.fresh.utils.Experience;
import cf.tenseventyseven.fresh.utils.Preferences;

public class ScreenResolutionActivity extends AppCompatActivity {
    public static String SCREEN_RESOLUTION = "device_screen_resolution_int";
    private static ExecutorService mExecutor;

    private static Context mContext;
    private static Activity mActivity;

    @BindView(R.id.toolbar_layout)
    ToolbarLayout toolbar;

    public static String getResolution(Context context) {
        String[] mResolutionValues = context.getResources().getStringArray(R.array.zest_screen_resolution_setting_main_summary);
        int setResolution = getResolutionInt(context);
        return mResolutionValues[setResolution];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExecutor = Executors.newSingleThreadExecutor();
        setContentView(R.layout.zest_activity_screen_resolution);
        ButterKnife.bind(this);

        mContext = this;
        mActivity = this;

        toolbar.setNavigationButtonTooltip(getString(R.string.sesl_navigate_up));
        toolbar.setNavigationButtonOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar.getToolbar());

        Experience.checkDefaultApiSetting(this);
        Experience.getRealScreenWidth(this, Experience.getActivity(this));

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.zest_screen_resolution_layout, new ScreenResolutionFragment())
                    .commit();
        }
    }

    public static int getResolutionInt(Context context) {
        return Settings.System.getInt(context.getContentResolver(), SCREEN_RESOLUTION, 2);
    }

    @SuppressLint("PrivateApi")
    private static Object getWindowManagerService() throws Exception {
        return Class.forName("android.view.WindowManagerGlobal")
                .getMethod("getWindowManagerService")
                .invoke(null);
    }

    @SuppressLint("PrivateApi")
    private static void setResolution(String value) throws Exception {
        final int USER_CURRENT_OR_SELF = -3;
        Scanner valueScan = new Scanner(value);
        String scannerDelimit = ":";
        valueScan.useDelimiter(scannerDelimit);
        String wmSize = valueScan.next();
        String wmDensity = valueScan.next();

        setResolutionAndRatio(mContext, wmSize, Preferences.getCurrentAspectRatio(mContext, mActivity));

        if (wmDensity.equals("reset")) {
            Class.forName("android.view.IWindowManager")
                    .getMethod("clearForcedDisplayDensityForUser", int.class, int.class)
                    .invoke(getWindowManagerService(), Display.DEFAULT_DISPLAY, USER_CURRENT_OR_SELF);
        } else {
            int density = Integer.parseInt(wmDensity);

            Class.forName("android.view.IWindowManager")
                    .getMethod("setForcedDisplayDensityForUser", int.class, int.class, int.class)
                    .invoke(getWindowManagerService(), Display.DEFAULT_DISPLAY, density, USER_CURRENT_OR_SELF);
        }
    }

    @SuppressLint("PrivateApi")
    public static void setResolutionAndRatio(Context context, String wmSize, String newRatio) {
        try {
            int sWidth, sHeight;

            if (wmSize == null) {
                int resIndex = Settings.System.getInt(context.getContentResolver(), ScreenResolutionActivity.SCREEN_RESOLUTION, 2);
                String resolution = context.getResources().getStringArray(R.array.zest_screen_resolution_setting_values)[resIndex];
                Scanner resScanner = new Scanner(resolution);
                resScanner.useDelimiter(":");
                wmSize = resScanner.next();
            }

            if (wmSize.equals("reset")) {
                if (newRatio.equals("reset")) {
                    Class.forName("android.view.IWindowManager")
                            .getMethod("clearForcedDisplaySize", int.class)
                            .invoke(getWindowManagerService(), Display.DEFAULT_DISPLAY);
                    return;
                }

                // HACK: Calculate actual, physical screen size here
                // Do NOT use getRealSize(). It doesn't work on resolution change.
                sWidth = 1080;
                sHeight = 2340;
            } else {
                Scanner scanner = new Scanner(wmSize);
                scanner.useDelimiter("x");
                sHeight = scanner.nextInt();
                sWidth = scanner.nextInt();
            }

            if (!newRatio.equals("reset")) {
                Scanner scanner = new Scanner(newRatio);
                scanner.useDelimiter(":");
                sHeight = sWidth * scanner.nextInt() / scanner.nextInt();
            }

            Class.forName("android.view.IWindowManager")
                    .getMethod("setForcedDisplaySize", int.class, int.class, int.class)
                    .invoke(getWindowManagerService(), Display.DEFAULT_DISPLAY, sWidth, sHeight);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ScreenResolutionFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        private Context mContext;

        TextView mResolutionSummary;
        MaterialButton mApplyButton;
        static String[] mResolutionValues;
        static int mSetResolution;
        static int mResolution;
        HorizontalRadioPreference preference;

        @Override
        public void onStart() {
            super.onStart();
        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            mResolution = Experience.getRealScreenWidth(mContext, Experience.getActivity(mContext));
            mSetResolution = mResolution;
            setResolutionSummary(preference.getValue());
            mApplyButton.setEnabled(false);
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mContext = getContext();
            mResolutionValues = context.getResources().getStringArray(R.array.zest_screen_resolution_setting_values);
            mSetResolution = getResolutionInt(mContext);
            mResolution = mSetResolution;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            String setResolution = getResolution();
            preference.setValue(setResolution);
            mResolutionSummary = getActivity().findViewById(R.id.resolution_summary);
            mApplyButton = getActivity().findViewById(R.id.resolution_apply);
            setResolutionSummary(preference.getValue());
            mApplyButton.setEnabled(false);

            mApplyButton.setOnClickListener(v -> {
                Settings.System.putInt(mContext.getContentResolver(), SCREEN_RESOLUTION, mResolution);
                mApplyButton.setEnabled(false);
                mSetResolution = mResolution;
                Experience.checkDefaultApiSetting(mContext);
                try {
                    Experience.setBypassBlacklist(mContext, true);
                    setResolution(preference.getValue());

                    // Re-lock APIs for security
                    Experience.setBypassBlacklist(mContext, false);

                    mExecutor.execute(() -> {
                        Experience.stopPackage(mContext, "com.sec.android.app.launcher");
                        Experience.stopPackage(mContext, "com.samsung.android.honeyboard");
                    });
                } catch (Exception e) {
                    Log.e("ScreenResolutionService", "Fail!", e);
                }
            });
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.zest_activity_screen_resolution_settings, rootKey);
            preference = ((HorizontalRadioPreference) findPreference("fs_screen_resolution_radio"));
            preference.setDividerEnabled(true);
            preference.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String prefKey = preference.getKey();
            if ("fs_screen_resolution_radio".equals(prefKey)) {
                setResolutionSummary(newValue.toString());
            }
            return false;
        }

        private static String getResolution() {
            return mResolutionValues[mSetResolution];
        }

        private void setResolutionSummary(String str) {
            String[] resSummary = this.getResources().getStringArray(R.array.zest_screen_resolution_setting_summary);

            if (str.equals(mResolutionValues[1])) {
                mResolutionSummary.setText(resSummary[1]);
                mResolution = 1;
            }  else if (str.equals(mResolutionValues[2])) {
                mResolutionSummary.setText(resSummary[2]);
                mResolution = 2;
            } else {
                mResolutionSummary.setText(resSummary[0]);
                mResolution = 0;
            }

            // Enable Apply button if set resolution is different than selected
            mApplyButton.setEnabled(!(mResolution == mSetResolution));
        }
    }
}