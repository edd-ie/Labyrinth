package edd_ie.com.github.labyrinth.logic;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import edd_ie.com.github.labyrinth.R;

public class SearchFunction {
    private class SearchRow extends RecyclerView.ViewHolder{
        public TextView room;
        public TextView building;

        public SearchRow(@NonNull View itemView) {
            super(itemView);
            room = itemView.findViewById(R.id.room_number);
            building = itemView.findViewById(R.id.building_name);
        }
    }
}
