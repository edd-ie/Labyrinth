package edd_ie.com.github.labyrinth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.Query;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import edd_ie.com.github.labyrinth.logic.LocationModel;
import edd_ie.com.github.labyrinth.logic.SearchFunction;
import edd_ie.com.github.labyrinth.utils.FirebaseUtil;

public class SearchDestination extends AppCompatActivity {
    private SearchView searchbar;
    private RecyclerView searchList;
    private ImageView backBtn;
    public SearchFunction searchLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_destination);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        searchbar = findViewById(R.id.search);
        searchList = findViewById(R.id.recyclerView);
        backBtn = findViewById(R.id.backBtn);

        //Event listeners
        searchbar.requestFocus();
        searchHandler();
        goToPreviousWindow();

    }

    private void goToPreviousWindow(){
        if(backBtn != null){
            backBtn.setOnClickListener((btn)->{
                startActivity(new Intent(SearchDestination.this, MainActivity.class));
            });
        }
    }

    private void searchHandler(){
        if(searchbar != null){
            searchbar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    // Handle query submission (if needed)
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Search your database using newText
                    performDatabaseSearch(newText);
                    return true;
                }
            });

        }
    }

    private void performDatabaseSearch(String searchTerm) {
        // Database search logic here
        Query query = FirebaseUtil.allLocations()
                .whereGreaterThanOrEqualTo("room",searchTerm);

        FirestoreRecyclerOptions<LocationModel> options = new FirestoreRecyclerOptions.Builder<LocationModel>()
                .setQuery(query,LocationModel.class).build();

        searchLocation = new SearchFunction(options, getApplicationContext());
        searchList.setLayoutManager(new LinearLayoutManager(this));
        searchList.setAdapter(searchLocation);
        searchLocation.startListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(searchLocation!=null)
            searchLocation.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(searchLocation!=null)
            searchLocation.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(searchLocation!=null)
            searchLocation.startListening();
    }
}