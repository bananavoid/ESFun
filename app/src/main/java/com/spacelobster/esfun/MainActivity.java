package com.spacelobster.esfun;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
	private static MainActivity master = null;

    private GLSurfaceView mGLView;
    private MyRenderer renderer = null;
    private FrameBuffer fb = null;
    private World world1 = null;
    private RGBColor back = new RGBColor(50, 50, 100);

    private float touchTurn = 0;
    private float touchTurnUp = 0;

    private float xpos = -1;
    private float ypos = -1;

    private Object3D object = null;
    private int fps = 0;

    private Light sun = null;

    private String thingName = "hand";


    protected void onCreate(Bundle savedInstanceState) {

        Logger.log("onCreate");

        if (master != null) {
            copy(master);
        }

        super.onCreate(savedInstanceState);
        mGLView = new GLSurfaceView(getApplication());

        mGLView.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
            public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
                // Ensure that we get a 16bit framebuffer. Otherwise, we'll fall
                // back to Pixelflinger on some device (read: Samsung I7500)
                int[] attributes = new int[] { EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE };
                EGLConfig[] configs = new EGLConfig[1];
                int[] result = new int[1];
                egl.eglChooseConfig(display, attributes, configs, 1, result);
                return configs[0];
            }
        });

        renderer = new MyRenderer();
        mGLView.setRenderer(renderer);
        setContentView(mGLView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
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
				!= PackageManager.PERMISSION_GRANTED) {//Can add more as per requirement

			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
					123);

		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       String permissions[], int[] grantResults)
	{
		switch (requestCode) {
			case 123: {


				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED)     {
					selectImage();
				} else {
					checkPermission();
				}
				return;
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

		Object3D img = Primitives.getPlane(64, 1);
		img.setBillboarding(true);
		img.setTexture("textureTatoo");
		img.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);
		img.strip();
		//img.setTransparency(5);

		img.build();

		world1.addObject(img);
	}

	private void copy(Object src) {
        try {
            Logger.log("Copying data from master Activity!");
            Field[] fs = src.getClass().getDeclaredFields();
            for (Field f : fs) {
                f.setAccessible(true);
                f.set(this, f.get(src));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            touchTurn = 0;
            touchTurnUp = 0;
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_MOVE) {
            float xd = me.getX() - xpos;
            float yd = me.getY() - ypos;

            xpos = me.getX();
            ypos = me.getY();

            touchTurn = xd / -100f;
            touchTurnUp = yd / -100f;
            return true;
        }

        try {
            Thread.sleep(15);
        } catch (Exception e) {
            // No need for this...
        }

        return super.onTouchEvent(me);
    }

    protected boolean isFullscreenOpaque() {
        return true;
    }

    class MyRenderer implements GLSurfaceView.Renderer {

        private long time = System.currentTimeMillis();

        public MyRenderer() {
        }

        public void onSurfaceChanged(GL10 gl, int w, int h) {
            if (fb != null) {
                fb.dispose();
            }
            fb = new FrameBuffer(gl, w, h);

            if (master == null) {

                world1 = new World();

                world1.setAmbientLight(20, 20, 20);

                sun = new Light(world1);
                sun.setIntensity(250, 250, 250);

                // Create a texture out of the icon...:-)
	            TextureManager.getInstance().addTexture("textureSkin", new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.skin)), 64, 64), true));
//	            TextureManager.getInstance().addTexture("textureTatoo", new Texture(BitmapHelper.rescale(BitmapHelper.convert(getResources().getDrawable(R.drawable.tatoo)), 64, 64), true));
//
//
                try {
                    object = loadModel(thingName + ".3ds", 10);
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

//	            object = Primitives.getCylinder(10);
                object.calcTextureWrapSpherical();
                object.setTexture("textureSkin");
	            //object.setAdditionalColor(23, 45, 90);
                object.strip();
                object.build();

                world1.addObject(object);

                Camera cam = world1.getCamera();
                cam.moveCamera(Camera.CAMERA_MOVEOUT, 50);
                cam.lookAt(object.getTransformedCenter());

                SimpleVector sv = new SimpleVector();
                sv.set(object.getTransformedCenter());
                sv.y -= 100;
                sv.z -= 100;
                sun.setPosition(sv);
                MemoryHelper.compact();

                if (master == null) {
                    Logger.log("Saving master Activity!");
                    master = MainActivity.this;
                }
            }
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        }

        public void onDrawFrame(GL10 gl) {
	        //if (object == null) return;

            if (touchTurn != 0) {
	            if (object != null) {
		            object.rotateY(touchTurn);
		            touchTurn = 0;
	            }
            }

            if (touchTurnUp != 0) {
	            if (object != null) {
		            object.rotateX(touchTurnUp);
		            touchTurnUp = 0;
	            }
            }

            fb.clear(back);
            world1.renderScene(fb);
            world1.draw(fb);
            fb.display();

            if (System.currentTimeMillis() - time >= 1000) {
                Logger.log(fps + "fps");
                fps = 0;
                time = System.currentTimeMillis();
            }
            fps++;
        }

        private Object3D loadModel(String filename, float scale) throws UnsupportedEncodingException {

            //InputStream stream = new ByteArrayInputStream(filename.getBytes("UTF-8"));
            InputStream stream = null;
            try {
                stream = getResources().getAssets().open(filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Object3D[] model = Loader.load3DS(stream, scale);
            Object3D o3d = new Object3D(0);
            Object3D temp = null;
            for (int i = 0; i < model.length; i++) {
                temp = model[i];
                temp.setCenter(SimpleVector.ORIGIN);
                temp.rotateX((float)( -.5*Math.PI));
                temp.rotateMesh();
                temp.setRotationMatrix(new Matrix());
                o3d = Object3D.mergeObjects(o3d, temp);
                o3d.build();
            }
            return o3d;
        }

    }
}