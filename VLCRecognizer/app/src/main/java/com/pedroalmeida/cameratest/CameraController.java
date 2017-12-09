package com.pedroalmeida.cameratest;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.util.Log;
import android.util.Range;
import android.util.SizeF;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pedro on 24/10/2017.
 */

public class CameraController {

    private static final String TAG = CameraController.class.getSimpleName();

    public static class CameraFeatures {
        public boolean is_zoom_supported;
        public int max_zoom;
        public List<Integer> zoom_ratios;
        public boolean supports_face_detection;
        public List<CameraController.Size> picture_sizes;
        public List<CameraController.Size> video_sizes;
        public List<CameraController.Size> video_sizes_high_speed;
        public float minimum_focus_distance;
        public boolean is_exposure_lock_supported;
        public boolean is_video_stabilization_supported;
        public boolean is_photo_video_recording_supported;
        public boolean supports_iso_range;
        public int min_iso;
        public int max_iso;
        public boolean supports_exposure_time;
        public long min_exposure_time;
        public long max_exposure_time;
        public int min_exposure;
        public int max_exposure;
        public float exposure_step;
        public boolean can_disable_shutter_sound;
        public boolean supports_expo_bracketing;
        public float view_angle_x; // horizontal angle of view in degrees (when unzoomed)
        public float view_angle_y; // vertical angle of view in degrees (when unzoomed)
    }

    public static class Size {
        public final int width;
        public final int height;

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Size))
                return false;
            Size that = (Size) o;
            return this.width == that.width && this.height == that.height;
        }

        @Override
        public int hashCode() {
            // must override this, as we override equals()
            // can't use:
            //return Objects.hash(width, height);
            // as this requires API level 19
            // so use this from http://stackoverflow.com/questions/11742593/what-is-the-hashcode-for-a-custom-class-having-just-two-int-properties
            return width * 31 + height;
        }
    }

    private CameraCharacteristics characteristics;

    public CameraFeatures getCameraFeatures() {
        Log.d(TAG, "getCameraFeatures()");
        CameraFeatures camera_features = new CameraFeatures();

        int hardware_level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (hardware_level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
            Log.d(TAG, "Hardware Level: LEGACY");
        else if (hardware_level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
            Log.d(TAG, "Hardware Level: LIMITED");
        else if (hardware_level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
            Log.d(TAG, "Hardware Level: FULL");
        else if (hardware_level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)
            Log.d(TAG, "Hardware Level: Level 3");
        else
            Log.e(TAG, "Unknown Hardware Level: " + hardware_level);

        int[] nr_modes = characteristics.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES);
        Log.d(TAG, "nr_modes:");
        if (nr_modes == null) {
            Log.d(TAG, "    none");
        } else {
            for (int i = 0; i < nr_modes.length; i++) {
                Log.d(TAG, "    " + i + ": " + nr_modes[i]);
            }
        }

        float max_zoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        camera_features.is_zoom_supported = max_zoom > 0.0f;
        Log.d(TAG, "max_zoom: " + max_zoom);
        if (camera_features.is_zoom_supported) {
            // set 20 steps per 2x factor
            final int steps_per_2x_factor = 20;
            //final double scale_factor = Math.pow(2.0, 1.0/(double)steps_per_2x_factor);
            int n_steps = (int) ((steps_per_2x_factor * Math.log(max_zoom + 1.0e-11)) / Math.log(2.0));
            final double scale_factor = Math.pow(max_zoom, 1.0 / (double) n_steps);
            Log.d(TAG, "n_steps: " + n_steps);
            Log.d(TAG, "scale_factor: " + scale_factor);
            camera_features.zoom_ratios = new ArrayList<>();
            camera_features.zoom_ratios.add(100);
            double zoom = 1.0;
            for (int i = 0; i < n_steps - 1; i++) {
                zoom *= scale_factor;
                camera_features.zoom_ratios.add((int) (zoom * 100));
            }
            camera_features.zoom_ratios.add((int) (max_zoom * 100));
            camera_features.max_zoom = camera_features.zoom_ratios.size() - 1;
        }

        int[] face_modes = characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
        camera_features.supports_face_detection = false;
        for (int face_mode : face_modes) {
            Log.d(TAG, "face detection mode: " + face_mode);
            // we currently only make use of the "SIMPLE" features, documented as:
            // "Return face rectangle and confidence values only."
            // note that devices that support STATISTICS_FACE_DETECT_MODE_FULL (e.g., Nexus 6) don't return
            // STATISTICS_FACE_DETECT_MODE_SIMPLE in the list, so we have check for either
            if (face_mode == CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE) {
                camera_features.supports_face_detection = true;
                Log.d(TAG, "supports simple face detection mode");
            } else if (face_mode == CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_FULL) {
                camera_features.supports_face_detection = true;
                Log.d(TAG, "supports full face detection mode");
            }
        }
        if (camera_features.supports_face_detection) {
            int face_count = characteristics.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT);
            if (face_count <= 0) {
                camera_features.supports_face_detection = false;
            }
        }

        int[] ois_modes = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION); // may be null on some devices
        if (ois_modes != null) {
            for (int ois_mode : ois_modes) {
                Log.d(TAG, "ois mode: " + ois_mode);
                if (ois_mode == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) {
                    Log.d(TAG, "supports ois");
                }
            }
        }

        int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        boolean capabilities_raw = false;
        boolean capabilities_high_speed_video = false;
        for (int capability : capabilities) {
            if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) {
                capabilities_raw = true;
            } else if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO) {
                capabilities_high_speed_video = true;
            }
        }
        Log.d(TAG, "capabilities_raw?: " + capabilities_raw);
        Log.d(TAG, "capabilities_high_speed_video?: " + capabilities_high_speed_video);


        StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        android.util.Size[] camera_picture_sizes = configs.getOutputSizes(ImageFormat.JPEG);
        camera_features.picture_sizes = new ArrayList<>();
        for (android.util.Size camera_size : camera_picture_sizes) {
            Log.d(TAG, "picture size: " + camera_size.getWidth() + " x " + camera_size.getHeight());
            camera_features.picture_sizes.add(new CameraController.Size(camera_size.getWidth(), camera_size.getHeight()));
        }

        if (capabilities_raw) {
            android.util.Size[] raw_camera_picture_sizes = configs.getOutputSizes(ImageFormat.RAW_SENSOR);
            if (raw_camera_picture_sizes == null) {
                Log.d(TAG, "RAW not supported, failed to get RAW_SENSOR sizes");
            }
        } else {
            Log.d(TAG, "RAW capability not supported");
        }

        android.util.Size[] camera_video_sizes = configs.getOutputSizes(MediaRecorder.class);
        camera_features.video_sizes = new ArrayList<>();
        for (android.util.Size camera_size : camera_video_sizes) {
            Log.d(TAG, "video size: " + camera_size.getWidth() + " x " + camera_size.getHeight());
            if (camera_size.getWidth() > 4096 || camera_size.getHeight() > 2160)
                continue; // Nexus 6 returns these, even though not supported?!
            camera_features.video_sizes.add(new CameraController.Size(camera_size.getWidth(), camera_size.getHeight()));
        }

        if (capabilities_high_speed_video) {
            android.util.Size[] camera_video_sizes_high_speed = configs.getHighSpeedVideoSizes();
            camera_features.video_sizes_high_speed = new ArrayList<>();
            for (android.util.Size camera_size : camera_video_sizes_high_speed) {
                Log.d(TAG, "high speed video size: " + camera_size.getWidth() + " x " + camera_size.getHeight());
                if (camera_size.getWidth() > 4096 || camera_size.getHeight() > 2160)
                    continue; // just in case? see above
                camera_features.video_sizes_high_speed.add(new CameraController.Size(camera_size.getWidth(), camera_size.getHeight()));
            }
        }

        Float minimum_focus_distance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE); // may be null on some devices
        if (minimum_focus_distance != null) {
            camera_features.minimum_focus_distance = minimum_focus_distance;
            Log.d(TAG, "minimum_focus_distance: " + camera_features.minimum_focus_distance);
        } else {
            camera_features.minimum_focus_distance = 0.0f;
        }

        camera_features.is_exposure_lock_supported = true;
        camera_features.is_video_stabilization_supported = true;

        // although we currently require at least LIMITED to offer Camera2, we explicitly check here in case we do ever support
        // LEGACY devices
        camera_features.is_photo_video_recording_supported = CameraController.isHardwareLevelSupported(characteristics, CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);

        Range<Integer> iso_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE); // may be null on some devices
        if (iso_range != null) {
            camera_features.supports_iso_range = true;
            camera_features.min_iso = iso_range.getLower();
            camera_features.max_iso = iso_range.getUpper();
            // we only expose exposure_time if iso_range is supported
            Range<Long> exposure_time_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE); // may be null on some devices
            if (exposure_time_range != null) {
                camera_features.supports_exposure_time = true;
                camera_features.supports_expo_bracketing = true;
                camera_features.min_exposure_time = exposure_time_range.getLower();
                camera_features.max_exposure_time = exposure_time_range.getUpper();
            }
        }

        Range<Integer> exposure_range = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        camera_features.min_exposure = exposure_range.getLower();
        camera_features.max_exposure = exposure_range.getUpper();
        camera_features.exposure_step = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP).floatValue();

        camera_features.can_disable_shutter_sound = true;

        {
            // Calculate view angles
            // Note this is an approximation (see http://stackoverflow.com/questions/39965408/what-is-the-android-camera2-api-equivalent-of-camera-parameters-gethorizontalvie ).
            // Potentially we could do better, taking into account the aspect ratio of the current resolution.
            // Note that we'd want to distinguish between the field of view of the preview versus the photo (or view) (for example,
            // DrawPreview would want the preview's field of view).
            // Also if we wanted to do this, we'd need to make sure that this was done after the caller had set the desired preview
            // and photo/video resolutions.
            SizeF physical_size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            float[] focal_lengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            camera_features.view_angle_x = (float) Math.toDegrees(2.0 * Math.atan2(physical_size.getWidth(), (2.0 * focal_lengths[0])));
            camera_features.view_angle_y = (float) Math.toDegrees(2.0 * Math.atan2(physical_size.getHeight(), (2.0 * focal_lengths[0])));

            Log.d(TAG, "view_angle_x: " + camera_features.view_angle_x);
            Log.d(TAG, "view_angle_y: " + camera_features.view_angle_y);
        }

        return camera_features;
    }

    /* Returns true if the device supports the required hardware level, or better.
     * From http://msdx.github.io/androiddoc/docs//reference/android/hardware/camera2/CameraCharacteristics.html#INFO_SUPPORTED_HARDWARE_LEVEL
	 * From Android N, higher levels than "FULL" are possible, that will have higher integer values.
	 * Also see https://sourceforge.net/p/opencamera/tickets/141/ .
	 */
    static boolean isHardwareLevelSupported(CameraCharacteristics c, int requiredLevel) {
        int deviceLevel = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
            Log.d(TAG, "Camera has LEGACY Camera2 support");
        else if (deviceLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
            Log.d(TAG, "Camera has LIMITED Camera2 support");
        else if (deviceLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
            Log.d(TAG, "Camera has FULL Camera2 support");
        else
            Log.d(TAG, "Camera has unknown Camera2 support: " + deviceLevel);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }


    private boolean setExposureTime(CaptureRequest.Builder builder, long exposure_time, int iso) {
        Log.d(TAG, "setExposureTime");
        Log.d(TAG, "manual mode");
        Log.d(TAG, "iso: " + iso);
        Log.d(TAG, "exposure_time: " + exposure_time);
        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposure_time);
        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

        return false;
    }
}
