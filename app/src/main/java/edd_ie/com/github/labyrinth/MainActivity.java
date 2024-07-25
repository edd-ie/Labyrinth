package edd_ie.com.github.labyrinth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.ArCoreApk.Availability;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Config.InstantPlacementMode;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import edd_ie.com.github.labyrinth.helpers.CameraPermissionHelper;
import edd_ie.com.github.labyrinth.helpers.DepthSettings;
import edd_ie.com.github.labyrinth.helpers.DisplayRotationHelper;
import edd_ie.com.github.labyrinth.helpers.FullScreenHelper;
import edd_ie.com.github.labyrinth.helpers.InstantPlacementSettings;
import edd_ie.com.github.labyrinth.helpers.SnackbarHelper;
import edd_ie.com.github.labyrinth.renderers.Framebuffer;
import edd_ie.com.github.labyrinth.renderers.SampleRender;
import edd_ie.com.github.labyrinth.renderers.arcore.BackgroundRenderer;


public class MainActivity extends AppCompatActivity implements SampleRender.Renderer, SensorEventListener {
    //Views
    private GLSurfaceView surfaceView;
    private View searchPlane;
    private EditText searchBar;
    private View canvas;
    private ImageView map;


    private Session session;
    private boolean installRequested;

    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private static final String TAG = MainActivity.class.getSimpleName();
    private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
    private boolean[] instantPlacementSettingsMenuDialogCheckboxes = new boolean[1];

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private DisplayRotationHelper displayRotationHelper;
    private final DepthSettings depthSettings = new DepthSettings();
    private boolean[] depthSettingsMenuDialogCheckboxes = new boolean[2];
    private SampleRender render;
    private boolean hasSetTextureNames = false;

    private BackgroundRenderer backgroundRenderer;
    private Framebuffer virtualSceneFramebuffer;

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100f;

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];


    //Senors and layout rotation
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private float[] gravity;
    private float[] geomagnetic;
    private float azimuth;

    private FrameLayout layoutToRotate;
    private Location targetLocation;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceview);
        searchPlane = findViewById(R.id.searchPlane);
        searchBar = findViewById(R.id.search_bar);
        canvas = findViewById(R.id.canvas);

        searchFocus();

        displayRotationHelper = new DisplayRotationHelper(/* context= */ this);

        // Set up renderer.
        render = new SampleRender(surfaceView, this, getAssets());

        installRequested = false;

        depthSettings.onCreate(this);
        instantPlacementSettings.onCreate(this);


        // Location and rotation
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        layoutToRotate = findViewById(R.id.layoutToRotate);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        targetLocation = new Location(LocationManager.GPS_PROVIDER);
        targetLocation.setLatitude(37.4219999);  // Example latitude
        targetLocation.setLongitude(-122.0840575);  // Example longitude

        checkLocationPermissionAndGetLocation();

        map = findViewById(R.id.map);
        float radius = 16f; // Adjust as needed

        ShapeAppearanceModel shapeAppearanceModel =new ShapeAppearanceModel.Builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                .setTopRightCorner(CornerFamily.ROUNDED, radius)
                .build();

        MaterialShapeDrawable frameLayoutBackground = new MaterialShapeDrawable(shapeAppearanceModel);
        frameLayoutBackground.setFillColor(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.holo_blue_light)));

        layoutToRotate.setBackground(frameLayoutBackground);

        MaterialShapeDrawable imageViewBackground = new MaterialShapeDrawable(shapeAppearanceModel);
        map.setBackground(imageViewBackground);
        map.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
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

        //Location and rotation
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
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

        //Location and rotation
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, magnetometer);
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
        backgroundRenderer = new BackgroundRenderer(render);
        virtualSceneFramebuffer = new Framebuffer(render, /* width= */ 1, /* height= */ 1);

    }

    @Override
    public void onSurfaceChanged(SampleRender render, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        virtualSceneFramebuffer.resize(width, height);
    }

    @Override
    public void onDrawFrame(SampleRender render) {
        if (session == null) {
            return;
        }

        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
            session.setCameraTextureNames(
                    new int[]{backgroundRenderer.getCameraColorTexture().getTextureId()});
            hasSetTextureNames = true;
        }

        // -- Update per-frame state

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        // Obtain the current frame from the AR Session. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        Frame frame;
        try {
            frame = session.update();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available during onDrawFrame", e);
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
            return;
        }
        Camera camera = frame.getCamera();

    }

    private void searchFocus() {
        if (searchBar != null) {
            searchBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        canvas.setVisibility(View.INVISIBLE);
                        surfaceView.setVisibility(View.INVISIBLE);
                        searchPlane.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.white));
                    } else {
                        searchPlane.setBackgroundColor(Color.TRANSPARENT);
                        canvas.setVisibility(View.VISIBLE);
                        surfaceView.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }


    //Location and Rotation

    private void checkLocationPermissionAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
        else {
            // Permissions already granted
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        // Use the location data here if needed
                        calculateAndRotateLayout(location);
                            } else {
                                Toast.makeText(MainActivity.this, "Unable to get location", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values;
        }

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]); // orientation contains: azimuth, pitch and roll
                azimuth = (azimuth + 360) % 360;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //Rotating layout
    private void calculateAndRotateLayout(Location currentLocation) {
        float bearingToTarget = currentLocation.bearingTo(targetLocation);
        float rotation = (bearingToTarget - azimuth + 360) % 360;
        layoutToRotate.setRotation(rotation);
    }
}
