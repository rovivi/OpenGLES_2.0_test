package com.example.myapplication;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer, View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {


    // region Constants
    public static final String MVP_MATRIX = "uMVPMatrix";
    public static final String POSITION = "vPosition";
    public static final String TEXTURE_COORDINATE = "vTextureCoordinate";
    // endregion


    private static final float[] POSITION_MATRIX = {//This is the cube
            -1, -1, 1,  // X1,Y1,Z1
            1, -1, 1,  // X2,Y2,Z2
            -1, 1, 1,  // X3,Y3,Z3
            1, 1, 1,  // X4,Y4,Z4
    };


    private FloatBuffer positionBuffer = ByteBuffer.allocateDirect(POSITION_MATRIX.length * 4)//this float buffet it help us lately
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(POSITION_MATRIX);
    //Son las cordenadas de la textura en este caso la imagen del gato // como podemos ver solamente son las cordenadas de la cara pricipal
    private static final float TEXTURE_COORDS[] = {
            0, 1, // X1,Y1
            1, 1, // X2,Y2
            0, 0, // X3,Y3
            1, 0, // X4,Y4
    };

    private FloatBuffer textureCoordsBuffer = ByteBuffer.allocateDirect(TEXTURE_COORDS.length * 4)//se asignan las cordenadas de la textura en un float buffer
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(TEXTURE_COORDS);
    // endregion Buffers

    // region Shaders











    private static final String VERTEX_SHADER = "" +
            "precision mediump float;" +
            "uniform mat4 " + MVP_MATRIX + ";" +
            "attribute vec4 " + POSITION + ";" +
            "attribute vec4 " + TEXTURE_COORDINATE + ";" +
            "varying vec2 position;" +
            "void main(){" +
            " gl_Position = " + MVP_MATRIX + " * " + POSITION + ";" +
            " position = " + TEXTURE_COORDINATE + ".xy;" +
            "}";

    private static final String FRAGMENT_SHADER = "" +
            "precision mediump float;" +
            "uniform sampler2D uTexture;" +
            "varying vec2 position;" +
            "void main() {" +
            "    gl_FragColor = texture2D(uTexture, position);" +
            "}";
    // endregion Shaders

    // region Variables
    private GLSurfaceView view;
    private int vPosition;
    private int vTexturePosition;
    private int uMVPMatrix;
    private ScaleGestureDetector detector;
    private float scale = 1;
    private float[] mvpMatrix = new float[16];
    private float[] projectionMatrix = new float[16];//la matriz de proyecccion
    private float[] viewMatrix = new float[16];//la camara?
    private float[] rotationMatrix = new float[16];
    //private MotionEvent motionEvent;

    ScaleGestureDetector scaleGestureDetector;

    public MainActivity() {
    }

    // endregion Variables


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        detector = new ScaleGestureDetector(this, this);
        view = findViewById(R.id.surface);
        view.setOnTouchListener(this);//se le asigna el touch listener nomas para mover el cubo jejejeje
        view.setPreserveEGLContextOnPause(true);//que no se pierda el contexto con un unfocus
        view.setEGLContextClientVersion(2);//se le dice que es opengl 2 y asi
        view.setRenderer(this);//como se implementa gl rederer entonces la misma instancia del activiy puede hacer el renderisado
        view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);//Cuando se pinta se limpia oseas no frame rate limit
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {


        // A little bit of initialization
        GLES20.glClearColor(1f, 40f, 0f, 0.5f); // El color de la limpieza en este caso un negro
        Matrix.setRotateM(rotationMatrix, 0, 0, 0, 0, 1.0f);  //Se le asigna la  matris de rotacion (ya que el cubo estar--a girando)


        // First, we load the picture into a texture that OpenGL will be able to use
        Bitmap bitmap = loadBitmapFromAssets(); ///se cargar el bitmap para usarlo como una textura posteriormente
        int texture = createFBOTexture(bitmap.getWidth(), bitmap.getHeight());   //se llama el metodo que convertira el bitmap en una textura para despues pasarla a opengl


        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture); //Se carga la texutra y se asocia con el id (que en este caso es un int)
        GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap);


        // Then, we load the shaders into a program
        int iVShader, iFShader, iProgId;
        int[] link = new int[1];
        iVShader = loadShader(VERTEX_SHADER, GLES20.GL_VERTEX_SHADER);
        iFShader = loadShader(ShadderRawinBow, GLES20.GL_FRAGMENT_SHADER);

        iProgId = GLES20.glCreateProgram();
        GLES20.glAttachShader(iProgId, iVShader);
        GLES20.glAttachShader(iProgId, iFShader);
        GLES20.glLinkProgram(iProgId);

        GLES20.glGetProgramiv(iProgId, GLES20.GL_LINK_STATUS, link, 0);
        if (link[0] <= 0) {
            throw new RuntimeException("Program couldn't be loaded");
        }
        GLES20.glDeleteShader(iVShader);
        GLES20.glDeleteShader(iFShader);
        GLES20.glUseProgram(iProgId);

        // Now that our program is loaded and in use, we'll retrieve the handles of the parameters
        // we pass to our shaders
        vPosition = GLES20.glGetAttribLocation(iProgId, POSITION);
        vTexturePosition = GLES20.glGetAttribLocation(iProgId, TEXTURE_COORDINATE);
        uMVPMatrix = GLES20.glGetUniformLocation(iProgId, MVP_MATRIX);


    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        // OpenGL will stretch what we give it into a square. To avoid this, we have to send the ratio
        // information to the VERTEX_SHADER. In our case, we pass this information (with other) in the
        // MVP Matrix as can be seen in the onDrawFrame method.
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 2, 7);
        //Matrix.frustumM(projectionMatrix, 0,-1, ratio, -1, 1, 3, 7);

        // Since we requested our OpenGL thread to only render when dirty, we have to tell it to.
        view.requestRender();


    }

    @Override
    public void onDrawFrame(GL10 gl) {
// We have setup that the background color will be black with GLES20.glClearColor in
        // onSurfaceCreated, now is the time to ask OpenGL to clear the screen with this color
//        GLES20.glClear(GLES20.GL_COLOR_CLEAR_VALUE | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(2, 1, 0, 1);


        // Using matrices, we set the camera at the center, advanced of 7 looking to the center back
        // of -1
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 7, 0, 0, -1, 0, 1, 0);
        // We combine the scene setup we have done in onSurfaceChanged with the camera setup
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        // We combile that with the applied rotation
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, rotationMatrix, 0);
        // Finally, we apply the scale to our Matrix
        Matrix.scaleM(mvpMatrix, 0, scale, scale, scale);
        // We attach the float array containing our Matrix to the correct handle
        GLES20.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0);

        // We pass the buffer for the position
        positionBuffer.position(0);
        GLES20.glVertexAttribPointer(vPosition, 3, GLES20.GL_FLOAT, false, 0, positionBuffer);
        GLES20.glEnableVertexAttribArray(vPosition);

        // We pass the buffer for the texture position
        textureCoordsBuffer.position(0);
        GLES20.glVertexAttribPointer(vTexturePosition, 2, GLES20.GL_FLOAT, false, 0, textureCoordsBuffer);
        GLES20.glEnableVertexAttribArray(vTexturePosition);

        // We draw our square which will represent our logo
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(vPosition);
        GLES20.glDisableVertexAttribArray(vTexturePosition);
    }

    private float previousX, previousY;

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (scaleGestureDetector.getScaleFactor() != 0) {
            scale *= scaleGestureDetector.getScaleFactor();
            view.requestRender();
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        detector.onTouchEvent(motionEvent);
        if (motionEvent.getPointerCount() == 1) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    previousX = motionEvent.getX();
                    previousY = motionEvent.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (previousX != motionEvent.getX()) {
                        Matrix.rotateM(rotationMatrix, 0, motionEvent.getX() - previousX, 0, 1, 0);
                    }
                    if (previousY != motionEvent.getY()) {
                        Matrix.rotateM(rotationMatrix, 0, motionEvent.getY() - previousY, 1, 0, 0);
                    }
                    this.view.requestRender();
                    previousX = motionEvent.getX();
                    previousY = motionEvent.getY();
                    break;
            }
        }

        return true;
    }


    //Methods
    private Bitmap loadBitmapFromAssets() {
        try (InputStream is = getAssets().open("owo.png")) {
            return BitmapFactory.decodeStream(is);
            //return  BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher_background);
        } catch (Exception ex) {
            throw new RuntimeException();
        }
        //
    }

    /**
     * This method generate a FBO texture
     *
     * @param width  ancho de la imagenn
     * @param height alto de la iman
     * @return returnla el id del FBO Texture
     */
    private int createFBOTexture(int width, int height) {
        int[] temp = new int[1];
        GLES20.glGenFramebuffers(1, temp, 0);
        int handleID = temp[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, handleID);

        int fboTex = createTexture(width, height);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTex, 0);

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("GL_FRAMEBUFFER status incomplete");
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return handleID;
    }

    private int createTexture(int width, int height) {
        int[] mTextureHandles = new int[1];
        GLES20.glGenTextures(1, mTextureHandles, 0);
        int textureID = mTextureHandles[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        return textureID;
    }

    private int loadShader(final String strSource, final int iType) {
        int[] compiled = new int[1];
        int iShader = GLES20.glCreateShader(iType);
        GLES20.glShaderSource(iShader, strSource);
        GLES20.glCompileShader(iShader);
        GLES20.glGetShaderiv(iShader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            throw new RuntimeException("Compilation failed : " + GLES20.glGetShaderInfoLog(iShader));
        }
        return iShader;
    }
    // endregion Utils





    String ShadderRawinBow = "#version 120\n" +
            "\n" +
            "uniform sampler2D texture;\n" +
            "uniform float rnd;\n" +
            "uniform float intensity;\n" +
            "uniform float colorswap;\n" +
            "\n" +
            "float rand(vec2 co){\n" +
            "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);\n" +
            "}\n" +
            "\n" +
            "void main (void)\n" +
            "{\n" +
            "\t//intensity = 1.0f;\n" +
            "\tfloat f = max(0, rand(vec2(gl_FragCoord.y, gl_FragCoord.x+rnd)) - rand(vec2(0, gl_FragCoord.x+rnd))*intensity);\n" +
            "\n" +
            "\tvec4 color = vec4(0, f, 0, texture2D(texture, gl_TexCoord[0].st).w * gl_Color.w * f);\n" +
            "\t\n" +
            "\tvec4 c = texture2D(texture, gl_TexCoord[0].st);\n" +
            "\tfloat r = c.r; float b = c.b; float g = c.g;\n" +
            "\n" +
            "\tfloat swap2 = (0.5f - abs(0.5f - colorswap))*2.0f;\n" +
            "\tc.r = r*(1.0f-swap2) + g*swap2;\n" +
            "\tc.b = b*(1.0f-swap2) + g*swap2;\n" +
            "\tc.g = g*(1.0f-swap2) + b*swap2;\n" +
            "\t\n" +
            "\tfloat swap1 = colorswap * (1.0f - swap2);\n" +
            "\tr = c.r; b = c.b;\n" +
            "\t\n" +
            "\tc.r = r*(1.0f-swap1) + b*swap1;\n" +
            "\tc.b = b*(1.0f-swap1) + r*swap1;\n" +
            "\t\n" +
            "\tvec4 c1 = c * gl_Color;\n" +
            "\t\n" +
            "\t//c1.w *= 1.0f-intensity;\n" +
            "\t//c1.w -= gl_FragCoord.y*0.1f;\n" +
            "\t\n" +
            "\tgl_FragColor = c1*(1-intensity*2) + color*intensity*2;\n" +
            "}";






}







