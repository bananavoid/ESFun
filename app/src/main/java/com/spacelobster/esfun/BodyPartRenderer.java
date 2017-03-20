package com.spacelobster.esfun;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.GenericVertexController;
import com.threed.jpct.Interact2D;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;
import com.threed.jpct.Mesh;
import com.threed.jpct.Object3D;
import com.threed.jpct.PolygonManager;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureInfo;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class BodyPartRenderer implements GLSurfaceView.Renderer {
	private Context mContext;
	private FrameBuffer mFrameBuffer = null;
	private World mWorld;
	private RGBColor mBackColor = new RGBColor(50, 50, 100);
	private Light mLight = null;
	private Object3D mObject = null;
	private Object3D mImageObject = null;
	private String mThingName = null;

	private float mLightTurn = 0;
	private float mLightTurnUp = 0;

	private int fps = 0;

	private long time = System.currentTimeMillis();
	private float mScaleFactor;

	public BodyPartRenderer(Context context, String modelFileName) {
		mContext = context;
		mWorld = new World();
		mWorld.setAmbientLight(20, 20, 20);
		mLight = new Light(mWorld);
		mLight.setIntensity(250, 250, 250);

		TextureManager.getInstance().addTexture("textureSkin",
				new Texture(BitmapHelper.rescale(BitmapHelper.convert(mContext.getResources().getDrawable(R.drawable.skin)),
						64, 64), true));

		TextureManager.getInstance().addTexture("textureSkinDark",
				new Texture(BitmapHelper.rescale(BitmapHelper.convert(mContext.getResources().getDrawable(R.drawable.skin_dark)),
						64, 64), true));

		mThingName = modelFileName;

		createObject();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int w, int h) {
		if (mFrameBuffer != null) {
			mFrameBuffer.dispose();
		}

		mFrameBuffer = new FrameBuffer(gl, w, h);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
	}

	@Override
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
			//Logger.log(fps + "fps");
			fps = 0;
			time = System.currentTimeMillis();
		}
		fps++;
	}

	private void createObject() {
		try {
			mObject = loadModel(mThingName, 10);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		mObject.calcTextureWrapSpherical();
		//mObject.setTextureMatrix(new Matrix());
		mObject.setTexture("textureSkin");
		//mObject.strip();

//		mObject.getMesh().setVertexController(new GenericVertexController() {
//			@Override
//			public void apply() {
//				if (mImageObject != null) {
//					Mesh imgMesh = mImageObject.getMesh();
//				}
//			}
//		}, true);

		mObject.build();
		mWorld.addObject(mObject);

		mWorld.getCamera().moveCamera(Camera.CAMERA_MOVEOUT, 40);
		mWorld.getCamera().lookAt(mObject.getTransformedCenter());
		mScaleFactor = mWorld.getCamera().getFOV();

		SimpleVector sv = new SimpleVector();
		sv.set(mObject.getTransformedCenter());
		sv.y -= 100;
		sv.z -= 100;
		mLight.setPosition(sv);
		MemoryHelper.compact();
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

	public void moveCamera(float fov) {
		if (fov < mScaleFactor)
			mWorld.getCamera().moveCamera(Camera.CAMERA_MOVEOUT, 0.5f);
		else if (fov > mScaleFactor)
			mWorld.getCamera().moveCamera(Camera.CAMERA_MOVEIN, 0.5f);

		mScaleFactor = fov;
	}

	public void moveImageObject(float fov) {
		if (fov < mScaleFactor)
			mImageObject.translate(0, 0, 0.2f);
		else if (fov > mScaleFactor)
			mImageObject.translate(0, 0, -0.2f);

		mScaleFactor = fov;
	}

	public void addObjectToThisWorld(Object3D object) {
		mImageObject = object;
		mWorld.addObject(mImageObject);
	}

	public void mergeObjects() {
		//tileTexture(mObject);
		//mergeImageToTexture();
		setPoligonTextureOnPlace();
	}

	private void tileTexture(Object3D obj) {
		PolygonManager pmObj = obj.getPolygonManager();
		PolygonManager pmTatoo = mImageObject.getPolygonManager();

		int end = pmTatoo.getMaxPolygonID();

		for (int i = 0; i < end; i++) {
			SimpleVector uv0 = pmTatoo.getTextureUV(i, 0);
			SimpleVector uv1 = pmTatoo.getTextureUV(i, 1);
			SimpleVector uv2 = pmTatoo.getTextureUV(i, 2);


			if (uv0 == null || uv1 == null || uv2 == null)
				return;

			//int t = pmTatoo.getPolygonTexture(i);
//			TextureInfo ti = new TextureInfo(
//					t,
//					uv0.x, uv0.y,
//					uv1.x, uv1.y,
//					uv2.x, uv2.y
//			);
			//pmObj.setPolygonTexture(i, ti);
		}

		obj.build();
		obj.touch();
	}

	private ArrayList<Integer> setPoligonTextureOnPlace() {
		float[] imgBounds = mImageObject.getMesh().getBoundingBox();
		SimpleVector translation = new SimpleVector();
		mImageObject.getTranslation(translation);

		Log.d("MRG", "image translation: " + translation.x + " " + translation.y + " " + translation.z);

		ArrayList<Integer> chosenIds = new ArrayList<>();

		SimpleVector minVector = Interact2D.reproject2D3DWS(
				mWorld.getCamera(),
				mFrameBuffer,
				(int)imgBounds[0],
				(int)imgBounds[2],
				imgBounds[4]
		);
		SimpleVector maxVector = Interact2D.reproject2D3DWS(
				mWorld.getCamera(),
				mFrameBuffer,
				(int)imgBounds[1],
				(int)imgBounds[3],
				imgBounds[5]
		);

		Log.d("MRG", "bounds min: " + imgBounds[0] + " " + imgBounds[2] + " " + imgBounds[4]);
		Log.d("MRG", "bounds max: " + imgBounds[1] + " " + imgBounds[3] + " " + imgBounds[5]);


		PolygonManager pmObj = mObject.getPolygonManager();
		int end = pmObj.getMaxPolygonID();

		for (int i = 0; i < end; ++i) {
			SimpleVector v0 = pmObj.getTransformedVertex(i, 0);
			SimpleVector v1 = pmObj.getTransformedVertex(i, 1);
			SimpleVector v2 = pmObj.getTransformedVertex(i, 2);

			if (
				v0.x < imgBounds[1] &&
				v0.x > imgBounds[0] &&
				v0.y < imgBounds[3] &&
				v0.y > imgBounds[2] &&
				v0.z <= 0 &&
				v1.x < imgBounds[1] &&
				v1.x > imgBounds[0] &&
				v1.y < imgBounds[3] &&
				v1.y > imgBounds[2] &&
				v1.z <= 0 &&
						v2.x < imgBounds[1] &&
						v2.x > imgBounds[0] &&
						v2.y < imgBounds[3] &&
						v2.y > imgBounds[2] &&
						v2.z <= 0
					)
			{
				chosenIds.add(i);
				pmObj.setPolygonTexture(i, TextureManager.getInstance().getTextureID("textureSkinDark"));
			}
		}

		Log.d("MRG", "chosen ones: " + chosenIds.size());

		return chosenIds;
	}
}
