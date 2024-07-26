package edd_ie.com.github.labyrinth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SearchDetails extends AppCompatActivity {
    private EditText startLocation;
    private EditText endLocation;
    private ImageView myLocationBtn;
    private Button startNavigation;
    private ImageView backBtn;

    private Float currentGpsLocation;
    private Float endGpsLocation;
    private String inputLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Elements
        startNavigation = findViewById(R.id.start_navigation);
        backBtn = findViewById(R.id.return_btn);

        //Event listeners
        handleBackBtn();
        handleNavigationBtn();
    }

    public void handleBackBtn(){
        backBtn.setOnClickListener((e)->{
            Intent intent = new Intent(SearchDetails.this, MainActivity.class);
            intent.putExtra("endLocationName", inputLocation);
            startActivity(intent);
        });
    }

    public void handleNavigationBtn(){
        startNavigation.setOnClickListener((e)->{
            Intent intent = new Intent(SearchDetails.this, MainActivity.class);
            intent.putExtra("startGps", currentGpsLocation);
            intent.putExtra("endGps", endGpsLocation);
            intent.putExtra("endLocationName", inputLocation);
            startActivity(intent);
        });

    }
}