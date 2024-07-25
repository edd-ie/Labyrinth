package edd_ie.com.github.labyrinth;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import edd_ie.com.github.labyrinth.helpers.FullScreenHelper;


public class MainActivity extends AppCompatActivity{
    GLSurfaceView arCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }


    private void searchFocus() {

//        if (searchBar != null) {
//            searchBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//                @Override
//                public void onFocusChange(View v, boolean hasFocus) {
//                    if (hasFocus) {
//                        canvas.setVisibility(View.INVISIBLE);
//                        surfaceView.setVisibility(View.INVISIBLE);
//                        searchPlane.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.white));
//                    } else {
//
//
//
//
//                        searchPlane.setBackgroundColor(Color.TRANSPARENT);
//                        canvas.setVisibility(View.VISIBLE);
//                        surfaceView.setVisibility(View.VISIBLE);
//                    }
//                }
//            });
//        }
    }



}
