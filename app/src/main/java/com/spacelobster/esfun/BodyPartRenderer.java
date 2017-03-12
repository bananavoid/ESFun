package com.spacelobster.esfun;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class BodyPartRenderer implements GLSurfaceView.Renderer {
	private Context mContext;
	private FrameBuffer mFrameBuffer = null;
	private World mWorld;
	private RGBColor mBackColor = new RGBColor(50, 50, 100);
	private Light mLight = null;
	private Object3D mObject = null;
	private String mThingName = null;

	private float mLightTurn = 0;
	private float mLightTurnUp = 0;

	private int fps = 0;

	private long time = System.currentTimeMillis();
	private float mScaleFactor;
	private ScaleGestureDetector mScaleDetector;

	public BodyPartRenderer(Context context, String modelFileName) {
		mContext = context;
		mWorld = new World();
		mWorld.setAmbientLight(20, 20, 20);
		mLight = new Light(mWorld);
		mLight.setIntensity(250, 250, 250);

		mThingName = modelFileName;
	}

	public void onSurfaceChanged(GL10 gl, int w, int h) {
		if (mFrameBuffer != null) {
			mFrameBuffer.dispose();
		}

		mFrameBuffer = new FrameBuffer(gl, w, h);

		TextureManager.getInstance().addTexture("textureSkin",
				new Texture(BitmapHelper.rescale(BitmapHelper.convert(mContext.getResources().getDrawable(R.drawable.skin)),
						64, 64), true));
		try {
			mObject = loadModel(mThingName, 10);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		mObject.calcTextureWrapSpherical();
		mObject.setTexture("textureSkin");
		mObject.strip();
		mObject.build();

		mWorld.addObject(mObject);

		mWorld.getCamera().moveCamera(Camera.CAMERA_MOVEOUT, 50);
		mWorld.getCamera().lookAt(mObject.getTransformedCenter());
		mScaleFactor = mWorld.getCamera().getFOV();

		SimpleVector sv = new SimpleVector();
		sv.set(mObject.getTransformedCenter());
		sv.y -= 100;
		sv.z -= 100;
		mLight.setPosition(sv);
		MemoryHelper.compact();
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
	}

	public void onDrawFrame(GL10 gl) {
		//if (object == null) return;

		if (mLightTurn != 0) {
			if (mObject != null) {
				mObject.rotateY(mLightTurn);
				mLightTurn = 0;
			}
		}

		if (mLightTurnUp != 0) {
			if (mObject != null) {
				mObject.rotateX(mLightTurnUp);
				mLightTurnUp = 0;
			}
		}

		//mWorld.getCamera().setFOV(mScaleFactor);
		mFrameBuffer.clear(mBackColor);
		mWorld.renderScene(mFrameBuffer);
		mWorld.draw(mFrameBuffer);
		mFrameBuffer.display();

		if (System.currentTimeMillis() - time >= 1000) {
			Logger.log(fps + "fps");
			fps = 0;
			time = System.currentTimeMillis();
		}
		fps++;
	}

	private Object3D loadModel(String filename, float scale) throws UnsupportedEncodingException {
		InputStream stream = null;
		try {
			stream = mContext.getResources().getAssets().open(filename);
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

	public void setLightTurn(float turn) {
		mLightTurn = turn;
	}

	public void setLightTurnUp(float turnUp) {
		mLightTurnUp = turnUp;
	}

	public void setCameraFOV(float fov) {
		if (fov < mScaleFactor)
			mWorld.getCamera().moveCamera(Camera.CAMERA_MOVEOUT, 1);
		else if (fov > mScaleFactor)
			mWorld.getCamera().moveCamera(Camera.CAMERA_MOVEIN, 1);

		mScaleFactor = fov;
	}
}
