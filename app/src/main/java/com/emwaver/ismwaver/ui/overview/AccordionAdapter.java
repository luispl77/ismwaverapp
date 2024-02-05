package com.emwaver.ismwaver.ui.overview;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.emwaver.ismwaver.R;

import java.util.Arrays;
import java.util.List;

public class AccordionAdapter extends RecyclerView.Adapter<AccordionAdapter.ViewHolder> {

    private final SparseBooleanArray expandState = new SparseBooleanArray();

    // Titles and descriptions are hardcoded
    private final String[] titles = {"Frequency", "Power", "Bandwidth"};
    private final String[] descriptions = {
            "The frequency configuration determines the operating frequency of the device.",
            "This setting adjusts the output power level.",
            "Bandwidth configuration affects data rate and range."
    };

    public AccordionAdapter() {
        for (int i = 0; i < titles.length; i++) {
            expandState.append(i, false); // Initialize all items to be collapsed
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_accordion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        holder.titleTextView.setText(titles[position]);
        holder.detailTextView.setText(descriptions[position]);
        holder.detailTextView.setVisibility(expandState.get(position) ? View.VISIBLE : View.GONE);

        // Toggle visibility of the details
        holder.titleTextView.setOnClickListener(v -> {
            boolean isExpanded = !expandState.get(position);
            expandState.put(position, isExpanded);
            notifyItemChanged(position);
        });

        // Initially, EditTexts are empty
        holder.configurationEditText.setText("");
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, detailTextView;
        EditText configurationEditText;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            detailTextView = itemView.findViewById(R.id.detailTextView);
            configurationEditText = itemView.findViewById(R.id.configurationEditText);
        }
    }
}

