package edd_ie.com.github.labyrinth.logic;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import edd_ie.com.github.labyrinth.R;
import edd_ie.com.github.labyrinth.SearchStartLocation;

public class SearchFunction extends FirestoreRecyclerAdapter<LocationModel, SearchFunction.SearchViewModal> {
    Context context;


    public SearchFunction(@NonNull FirestoreRecyclerOptions<LocationModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull SearchViewModal holder, int position, @NonNull LocationModel model) {
        holder.room.setText(model.getRoom());
        holder.building.setText(model.getBuilding());

        holder.itemView.setOnClickListener(v ->{
            //navigate to chat activity
            Intent intent = new Intent(context, SearchStartLocation.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public SearchViewModal onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_location,parent,false);
        return new SearchViewModal(view);
    }

    public class SearchViewModal extends RecyclerView.ViewHolder{
        public TextView room;
        public TextView building;

        public SearchViewModal(@NonNull View itemView) {
            super(itemView);
            room = itemView.findViewById(R.id.room_number);
            building = itemView.findViewById(R.id.building_name);
        }
    }
}
