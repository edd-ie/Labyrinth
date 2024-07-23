package edd_ie.com.github.labyrinth;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.ar.core.Config;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;
import com.google.ar.core.ArCoreApk.Availability;
import com.google.ar.core.Config.InstantPlacementMode;

import edd_ie.com.github.labyrinth.helpers.CameraPermissionHelper;
import edd_ie.com.github.labyrinth.helpers.DepthSettings;
import edd_ie.com.github.labyrinth.helpers.FullScreenHelper;
import edd_ie.com.github.labyrinth.helpers.InstantPlacementSettings;
import edd_ie.com.github.labyrinth.helpers.SnackbarHelper;
import edd_ie.com.github.labyrinth.helpers.DisplayRotationHelper;
import edd_ie.com.github.labyrinth.renderers.SampleRender;


public class MainActivity extends AppCompatActivity implements SampleRender.Renderer {
    private Session session;
    private boolean installRequested;

    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private static final String TAG = MainActivity.class.getSimpleName();
    private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
    private boolean[] instantPlacementSettingsMenuDialogCheckboxes = new boolean[1];

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;
    private DisplayRotationHelper displayRotationHelper;
    private final DepthSettings depthSettings = new DepthSettings();
    private boolean[] depthSettingsMenuDialogCheckboxes = new boolean[2];
    private SampleRender render;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/* context= */ this);

        // Set up touch listener.
//        tapHelper = new TapHelper(/* context= */ this);
//        surfaceView.setOnTouchListener(tapHelper);

        // Set up renderer.
        render = new SampleRender(surfaceView, this, getAssets());

        installRequested = false;

        depthSettings.onCreate(this);
        instantPlacementSettings.onCreate(this);
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            session.close();
            session = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                // Always check the latest availability.
                Availability availability = ArCoreApk.getInstance().checkAvailability(this);

                // In all other cases, try to install ARCore and handle installation failures.
                if (availability != Availability.SUPPORTED_INSTALLED) {
                    switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                        case INSTALL_REQUESTED:
                            installRequested = true;
                            return;
                        case INSTALLED:
                            break;
                    }
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                // Create the session.
                session = new Session(/* context= */ this);
            } catch (UnavailableArcoreNotInstalledException
                     | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            configureSession();
            // To record a live camera session for later playback, call
            // `session.startRecording(recordingConfig)` at anytime. To playback a previously recorded AR
            // session instead of using the live camera feed, call
            // `session.setPlaybackDatasetUri(Uri)` before calling `session.resume()`. To
            // learn more about recording and playback, see:
            // https://developers.google.com/ar/develop/java/recording-and-playback
            session.resume();
        } catch (CameraNotAvailableException e) {
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
            session = null;
            return;
        }

        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            // Use toast instead of snack bar here since the activity will exit.
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    /** Configures the session with feature settings. */
    private void configureSession() {
        Config config = session.getConfig();
        config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        } else {
            config.setDepthMode(Config.DepthMode.DISABLED);
        }
        if (instantPlacementSettings.isInstantPlacementEnabled()) {
            config.setInstantPlacementMode(InstantPlacementMode.LOCAL_Y_UP);
        } else {
            config.setInstantPlacementMode(InstantPlacementMode.DISABLED);
        }
        session.configure(config);
    }


    @Override
    public void onSurfaceCreated(SampleRender render) {

    }

    @Override
    public void onSurfaceChanged(SampleRender render, int width, int height) {

    }

    @Override
    public void onDrawFrame(SampleRender render) {

    }
}
