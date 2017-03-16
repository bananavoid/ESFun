package com.spacelobster.esfun;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.nex3z.togglebuttongroup.ToggleButtonGroup;
import com.spacelobster.esfun.databinding.ActivityMainBinding;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

public class MainActivity extends AppCompatActivity {

	private static final int PICK_IMAGE_REQUEST = 11;
	private static final int PERMISSIONS_REQUEST = 12;
	private static final int MODE_BODY = 8;
	private static final int MODE_IMAGE = 9;

	private int mCurrentMode = MODE_BODY;

	private BodyPartRenderer mBodyRenderer;
//	private ImageRenderer mImageRenderer;
	private ActivityMainBinding mBinding;

    private float xpos = -1;
    private float ypos = -1;
	private float mScaleFactor;
	private ScaleGestureDetector mScaleDetector;

	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

		mBodyRenderer = new BodyPartRenderer(this, "hand.3ds");
		//mImageRenderer = new ImageRenderer(this);

		mBinding.surfaceView.setRenderer(mBodyRenderer);
		//mBinding.imageSurfaceView.setRenderer(mImageRenderer);

	    List<String> choices = Arrays.asList("M", "I");
	    mBinding.singleSelectionBtns.setButtons(choices);

		mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());

		mBinding.singleSelectionBtns.setOnCheckedChangeListener(new ToggleButtonGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChange(int position, boolean isChecked) {
				if (isChecked && position == 0)
					mCurrentMode = MODE_BODY;
				else if (isChecked && position == 1)
					mCurrentMode = MODE_IMAGE;
			}
		});
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
		//mImageRenderer.addObject(bitmap);
		TextureManager.getInstance().addTexture("textureTatoo", new Texture(bitmap, true));

		Object3D mObject = Primitives.getPlane(30, 0.5f);
		mObject.setBillboarding(true);
		mObject.setTexture("textureTatoo");
		mObject.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);
		mObject.strip();

		mObject.build();

		mBodyRenderer.addObjectToThisWorld(mObject);
	}

    public boolean onTouchEvent(MotionEvent me) {

	    mScaleDetector.onTouchEvent(me);

        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            xpos = me.getX();
            ypos = me.getY();
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_UP) {
            xpos = -1;
            ypos = -1;
            mBodyRenderer.setLightTurn(0);
            mBodyRenderer.setLightTurnUp(0);
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_MOVE) {
            float xd = me.getX() - xpos;
            float yd = me.getY() - ypos;

            xpos = me.getX();
            ypos = me.getY();

            mBodyRenderer.setLightTurn(xd / -100f);
            mBodyRenderer.setLightTurnUp(yd / -100f);
            return true;
        }

        try {
            Thread.sleep(15);
        } catch (Exception e) {
            // No need for this...
        }

        return super.onTouchEvent(me);
    }

	private class ScaleListener
			extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor *= detector.getScaleFactor();

			// Don't let the object get too small or too large.
			mScaleFactor = Math.max(0.01f, Math.min(mScaleFactor, 0.2f));
			mBodyRenderer.setCameraFOV(mScaleFactor);
			Log.d("SCALE", "factor: " + mScaleFactor);

			return true;
		}
	}
}