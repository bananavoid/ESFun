package com.spacelobster.esfun;

import android.opengl.GLSurfaceView;
import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    /** The OpenGL View */


    /**
     * Initiate the OpenGL View and set our own
     * Renderer (@see Lesson02.java)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Debug.startMethodTracing("calc");
        super.onCreate(savedInstanceState);
        setContentView(new MyRenderer(this));
    }

    /**
     * Remember to resume the glSurface
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Also pause the glSurface
     */
    @Override
    protected void onPause() {
        Debug.stopMethodTracing();
        super.onPause();
    }
    @Override
    protected void onDestroy(){
        Debug.stopMethodTracing();
        super.onDestroy();
    }
}
