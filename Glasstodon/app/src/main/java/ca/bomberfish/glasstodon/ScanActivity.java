package ca.bomberfish.glasstodon;
import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Hashtable;

import android.content.Intent;
import android.hardware.Camera;
import android.util.Log;
import java.io.IOException;
import java.util.Vector;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.camera.CameraManager;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.MultiFormatReader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ScanActivity extends Activity implements SurfaceHolder.Callback {
    private CameraManager mgr;
    private MultiFormatReader reader;
    private boolean scanning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        reader = new MultiFormatReader();
        Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>();
        Vector<BarcodeFormat> formats = new Vector<BarcodeFormat>();
        formats.add(BarcodeFormat.QR_CODE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        reader.setHints(hints);
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        mgr = new CameraManager(getApplicationContext());
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_surface);
        SurfaceHolder holder = surfaceView.getHolder();

        holder.addCallback(this);
    }

    @Override
    protected void onPause() {
        mgr.stopPreview();
        mgr.closeDriver();

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_surface);
        surfaceView.getHolder().removeCallback(this);

        super.onPause();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mgr.openDriver(holder);
            mgr.startPreview();
            scanning = true;
            requestNextFrame();
        } catch (IOException e) {
            Log.e("ScanActivity", "Error initializing camera: " + e.getMessage());
            finish();
        } catch (InterruptedException e) {
            Log.e("ScanActivity", "Camera initialization interrupted: " + e.getMessage());
            finish();
        } catch (RuntimeException e) {
            Log.e("ScanActivity", "Unexpected error initializing camera: " + e.getMessage());
            finish();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}
        
    private void requestNextFrame() {
        Camera camera = mgr.getCamera();
        if (camera != null) {
            camera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    decodeFrame(data, camera);
                }
            });
        }
    }

    private void decodeFrame(byte[] data, Camera camera) {
        if (!scanning) return;

        Camera.Parameters params = camera.getParameters();
        Camera.Size size = params.getPreviewSize();
        PlanarYUVLuminanceSource source = mgr.buildLuminanceSource(data, size.width, size.height);

        if (source == null) {
            requestNextFrame();
            return;
        }

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            Result result = reader.decodeWithState(bitmap);
            scanning = false;
            handleScanResult(result.getText());
        } catch (NotFoundException e) {
            // No QR code found in this frame, request another
            requestNextFrame();
        } finally {
            reader.reset();
        }
    }

    private void handleScanResult(String text) {
        try {
            Gson gson = new Gson();
            JsonObject obj = gson.fromJson(text, JsonObject.class);
            String instanceUrl = obj.get("instance").getAsString();
            String accessToken = obj.get("token").getAsString();

            Intent result = new Intent();
            result.putExtra("InstanceURL", instanceUrl);
            result.putExtra("AccessToken", accessToken);
            setResult(RESULT_OK, result);
            finish();
        } catch (Exception e) {
            Log.e("ScanActivity", "Couldn't parse QR code:  Parsing '"  + text + "' failed with error " + e.getMessage());
            Log.e("ScanActivity", "Full stack trace: ", e);
            scanning = true;
            requestNextFrame();
        }
    }
}
