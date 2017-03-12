package com.spacelobster.esfun;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.*;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.spacelobster.esfun.databinding.ActivityMainBinding;
import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;

public class MainActivity extends AppCompatActivity {

	private static final int PICK_IMAGE_REQUEST = 11;
	private static final int PERMISSIONS_REQUEST = 12;

	private BodyPartRenderer mBodyRenderer;
	private ActivityMainBinding mBinding;

    private float xpos = -1;
    private float ypos = -1;

	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

		mBodyRenderer = new BodyPartRenderer(this, "hand.3ds");
		mBinding.surfaceView.setRenderer(mBodyRenderer);

	    List<String> choices = Arrays.asList("M", "I");
	    mBinding.singleSelectionBtns.setButtons(choices);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBinding.surfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBinding.surfaceView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.select_img:
				selectImage();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void checkPermission() {
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
					PERMISSIONS_REQUEST);

		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSIONS_REQUEST: {
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					selectImage();
				} else {
					checkPermission();
				}
			}
		}
	}

	private void selectImage() {
		checkPermission();
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == PICK_IMAGE_REQUEST &&
				resultCode == RESULT_OK && data != null && data.getData() != null) {
			Uri uri = data.getData();
			Bitmap bitmap = null;
			Bitmap resized = null;
			try {
				bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
				resized = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
				resized.setHasAlpha(true);
				applyImageToPlane(resized);

				bitmap.recycle();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;

	}

	private void applyImageToPlane(Bitmap bitmap) {
		TextureManager.getInstance().addTexture("textureTatoo", new Texture(bitmap, true));

		Object3D overlay = Primitives.getPlane(30, 0.5f);
		overlay.setBillboarding(true);
		overlay.setTexture("textureTatoo");
		overlay.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);
		overlay.strip();

		overlay.build();

		//world1.addObject(overlay);
	}

    public boolean onTouchEvent(MotionEvent me) {

        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            xpos = me.getX();
            ypos = me.getY();
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_UP) {
            xpos = -1;
            ypos = -1;
            mBodyRenderer.setLightTurn(0);
            mBodyRenderer.setmLightTurnUp(0);
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_MOVE) {
            float xd = me.getX() - xpos;
            float yd = me.getY() - ypos;

            xpos = me.getX();
            ypos = me.getY();

            mBodyRenderer.setLightTurn(xd / -100f);
            mBodyRenderer.setmLightTurnUp(yd / -100f);
            return true;
        }

        try {
            Thread.sleep(15);
        } catch (Exception e) {
            // No need for this...
        }

        return super.onTouchEvent(me);
    }
}