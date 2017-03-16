package com.spacelobster.esfun;


import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Logger;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.MemoryHelper;

import java.io.UnsupportedEncodingException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ImageRenderer implements GLSurfaceView.Renderer {
	private Context mContext;
	private FrameBuffer mFrameBuffer = null;
	private World mWorld;
	private RGBColor mBackColor = new RGBColor(50, 50, 100);
	private Object3D mObject = null;

	public ImageRenderer(Context mContext) {
		this.mContext = mContext;

		mWorld = new World();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		if (mFrameBuffer != null) {
			mFrameBuffer.dispose();
		}

		mFrameBuffer = new FrameBuffer(gl, width, height);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		mFrameBuffer.clear(mBackColor);
		mWorld.renderScene(mFrameBuffer);
		mWorld.draw(mFrameBuffer);
		mFrameBuffer.display();
	}

	public void addObject(Bitmap textureBitmap) {
		TextureManager.getInstance().addTexture("textureTatoo", new Texture(textureBitmap, true));

		mObject = Primitives.getPlane(30, 0.5f);
		mObject.setBillboarding(true);
		mObject.setTexture("textureTatoo");
		mObject.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);
		mObject.strip();

		mObject.build();

		mWorld.addObject(mObject);

		mWorld.getCamera().moveCamera(Camera.CAMERA_MOVEOUT, 40);
		mWorld.getCamera().lookAt(mObject.getTransformedCenter());

		MemoryHelper.compact();
	}
}
