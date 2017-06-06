package com.example.neomatrix.ia;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.R.attr.src;
import static org.opencv.android.Utils.bitmapToMat;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private  static  final String TAG = "MainActivity";
    JavaCameraView javaCameraView;
    Mat RoiRgb,mRgb, imgSalida;   

    TextView mensaje;
    int anchoinicial, altoinicial,ancho, alto, margen=30;

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS:{
                    javaCameraView.enableView();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }

        }
    };

    static {


    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        javaCameraView = (JavaCameraView)findViewById(R.id.java_camara_view);

        mensaje = (TextView)findViewById(R.id.mensaje);

        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause(){
        super.onPause();
        if (javaCameraView!=null)
            javaCameraView.disableView();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (javaCameraView!=null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (OpenCVLoader.initDebug()){
            Log.d(TAG, "corriendo");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }else{
            Log.d(TAG, "ERROR");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9,this,mLoaderCallback);
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {

        anchoinicial= width;
        altoinicial = height;

        mRgb = new Mat(width, height, CvType.CV_32SC4); //c4 = cuatro canales de color
        imgSalida = new Mat(height, width, CvType.CV_8UC1);
        //imgCanny = new Mat(height, width, CvType.CV_8UC1);

    }

    @Override
    public void onCameraViewStopped() {
        mRgb.release();
    }

    public String clasificar(double valor_compacidad){

        String etiqueta = "";

        double[] valores_clases = {0.002,2.0,0.001};

        double[] nuevos_valores = {Math.abs(valores_clases[0]-valor_compacidad),Math.abs(valores_clases[1]-valor_compacidad),Math.abs(valores_clases[2]-valor_compacidad)};

        double iNumeroMenor = nuevos_valores[0];
        int iPosicion = 0;
        for (int x=1;x<nuevos_valores.length;x++){
            if (nuevos_valores[x]<iNumeroMenor){
                iNumeroMenor = nuevos_valores[x];
                iPosicion = x;
            }
        }

        if(iPosicion==0){
            etiqueta="triangulo";
        }
        if(iPosicion==1){
            etiqueta="cuadrado";
        }
        if(iPosicion==2){
            etiqueta="circulo";
        }


        return  etiqueta;

    }

    public int[] area_perimetro(int [][] matriz, int ancho, int alto){
        int[] areayperimetro = new int[2];

        
        int area = 0;
        int perimetro = 0;

        for (int x = 1; x < (ancho-1); x++) {
            for (int y = 1; y < (alto-1); y++) {
               area += matriz[y][x];

                if (matriz[y][x] == 1) {
                    if (((matriz[y-1][x]==0) || (matriz[y][x -1]==0))||(matriz[y-1][x-1]==0)) {
                        if (((matriz[y+1][x]==0) || (matriz[y][x+1]==0))||(matriz[y+1][x+1]==0)) {
                            perimetro=perimetro+1;
                        }
                    }
                }

            }
        }

        areayperimetro[0]=area;
        areayperimetro[1]=perimetro;

        return  areayperimetro;
    }

    public int[][] arraytomatriz(int[] vector,int ancho,int alto){
        int matriz[][] = new int[alto][ancho];
        int cont = 0;

        for (int x = 0; x < ancho; x++) {
            for (int y = 0; y < alto; y++) {
                matriz[y][x] = vector[cont];
                cont++;
            }
        }
        return matriz;
    }

    public int[] binariza(int[] gris ,int umbral) {

        int[] binario = new int[gris.length];

        for (int i =0; i < gris.length; i++) {
            if (gris[i]<=umbral) {
                binario[i] = 1;
            }else{
                binario[i]= 0;
            }
        }
        return binario;
    }

    public int[] escala_grises(int[] rojo, int[] verde, int[]azul ) {

        int[] gris = new int[rojo.length];

        for (int i =0; i < gris.length; i++) {
            gris[i] = (int)((rojo[i] + verde[i] + azul[i]) / 3);
        }

        return gris;
    }

    public Bitmap arraytobitmap(int[] vector){
        // You are using RGBA that's why Config is ARGB.8888
        Bitmap bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888);
        // vector is your int[] of ARGB
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(vector));
        return  bitmap;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {


        Rect r = new Rect(margen,margen,(anchoinicial)-(margen*2),(altoinicial)-(margen*2));
        // x inicio, y inicio, ancho y alto del rectangulo
        ancho= (anchoinicial)-(margen*2);
        alto=  (altoinicial)-(margen*2);

        mRgb = inputFrame.rgba();


        RoiRgb = new Mat(mRgb, r);

        Core.rectangle(mRgb, r.tl(), r.br(), new Scalar(153,255,0), 3);

        final MatOfInt rgb = new MatOfInt(CvType.CV_32S);
        RoiRgb.convertTo(rgb,CvType.CV_32S);

        final int[] rgba = new int[(int)(rgb.total()*rgb.channels())];
        rgb.get(0,0,rgba);
        //4 canales

        final int[] rojo = new int[(int) rgb.total()]; 
        final int[] verde = new int[(int) rgb.total()];
        final int[] azul = new int[(int) rgb.total()]; 

        for (int i=0; i<(int)rgb.total(); i++ ){
            rojo[i] = rgba[i];
            verde[i] = rgba[i+(int)(rgb.total())];;
            azul[i] = rgba[i+(int)(rgb.total()*2)];
        }

        final int[] escala_gris = escala_grises(rojo,verde, azul);

        final int[] img_binaria = binariza(escala_gris, 120 );

        int[][] matrizdatos = arraytomatriz(img_binaria,ancho,alto);

        int[] area_y_perimetro = area_perimetro(matrizdatos, ancho,alto );

        final int my_area = area_y_perimetro[0];
        final int my_perimetro = area_y_perimetro[1];

        double perimetro2 = my_perimetro*my_perimetro;
        final double compacidad1 = my_area/perimetro2;
        final double compacidad = (4*Math.PI)*(compacidad1);

       

        final String clasificacion = clasificar(compacidad);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mensaje.setText("area: "+my_area+" perimetro: "+my_perimetro+"\ncompacidad: "+ compacidad+"\n Clasificacion: "+ clasificacion+"\n\n\n\n\n\n\n\n");
            }
        });



        
        return mRgb; // se regresa la imagen a mostrar en la pantalla
    }
}
