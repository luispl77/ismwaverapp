package com.emwaver.ismwaver.ui.overview;

import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
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
import java.util.Locale;

public class AccordionAdapter extends RecyclerView.Adapter<AccordionAdapter.ViewHolder> {

    private final SparseBooleanArray expandState = new SparseBooleanArray();

    private ViewHolder viewHolder;

    // Titles and descriptions are hardcoded
    private final String[] titles = {"Frequency", "Modulation", "Power", "Bandwidth", "Gain", "Packet Settings", "GPIO"};
    private final String[] descriptions = {
            "The frequency configuration determines the operating frequency of the device.",
            "Modulation can be either Amplitude modulation (ASK) or Frequency modulation (FSK).",
            "Power level can be a maximum of +12 dBm.",
            "Bandwidth configuration affects noise, range and reception.",
            "Gain affects noise , range and reception.",
            "Packet Settings for Packet Mode.",
            "The CC1101 has 2 general purpose pins that can have many different configurations."
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
        String title = titles[position];
        holder.titleTextView.setText(title);
        holder.detailTextView.setText(descriptions[position]);
        viewHolder = holder;

        // Clear existing views in the container
        holder.editTextContainer.removeAllViews();

        // Based on the title, add labeled EditTexts dynamically
        if ("Frequency".equals(title)) {
            addLabeledEditText(holder.editTextContainer, "Center Frequency (MHz)", "Enter frequency in MHz");
        } else if ("Modulation".equals(title)) {
            addLabeledEditText(holder.editTextContainer, "Modulation (ASK/FSK)", "Enter ASK/FSK");
        } else if ("Power".equals(title)) {
            addLabeledEditText(holder.editTextContainer, "Power (dBm)", "Enter power in dBm");
        } else if ("Bandwidth".equals(title)) {
            addLabeledEditText(holder.editTextContainer, "Bandwidth (kHz)", "Enter bandwidth in KHz");
            addLabeledEditText(holder.editTextContainer, "Deviation (kHz)", "Enter deviation in kHz");
        }else if ("Gain".equals(title)) {
            addLabeledEditText(holder.editTextContainer, "LNA Gain (dB)", "Enter gain in dB");
            addLabeledEditText(holder.editTextContainer, "Filter Length (#samples)", "Enter number of samples");
            addLabeledEditText(holder.editTextContainer, "Decision Boundary (dB)", "Enter decision boundary in dB");
        } else if ("Packet Settings".equals(title)) {
            addLabeledEditText(holder.editTextContainer, "Packet Format", "Enter gain in dB");
            addLabeledEditText(holder.editTextContainer, "Packet Length (#bytes)", "Enter number of bytes in payload");
            addLabeledEditText(holder.editTextContainer, "Preamble Length (#bytes)", "Enter number of bytes in preamble");
            addLabeledEditText(holder.editTextContainer, "Sync Word (eg.8cab)", "Enter 2-byte sync word");
            addLabeledEditText(holder.editTextContainer, "Sync Mode (precision)", "Select sync word detection");
            addLabeledEditText(holder.editTextContainer, "Data Rate (kbit/s)", "Enter data rate in kbit/s");
        } else if ("GPIO".equals(title)) {
            addLabeledEditText(holder.editTextContainer, "GPIO 0", "Select function");
            addLabeledEditText(holder.editTextContainer, "GPIO 2", "Select function");
            addLabeledEditText(holder.editTextContainer, "FIFO Threshold (#bytes)", "Enter threshold in bytes");
        }

        // Adjust visibility based on expansion state
        boolean isExpanded = expandState.get(position);
        holder.detailTextView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.editTextContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Expand/collapse toggle
        holder.titleTextView.setOnClickListener(v -> {
            boolean newExpandState = !expandState.get(position);
            expandState.put(position, newExpandState);
            notifyItemChanged(position);
        });
    }


    // Helper method to dynamically add EditText fields
    // Helper method to dynamically add EditText fields with labels
    private void addLabeledEditText(LinearLayout container, String label, String hintText) {
        Context context = container.getContext();

        // Simplify the label for tag creation by removing details in parentheses and spaces
        String simplifiedLabel = label.replaceAll("\\s+\\(.*?\\)$", "").replace(" ", "");
        String editTextTag = simplifiedLabel + "EditText";
        Log.i("editTextTag", editTextTag);

        // Create and add the label TextView
        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setText(label);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // Example text size, adjust as needed
        container.addView(textView);

        // Check if the EditText already exists
        View existingView = container.findViewWithTag(editTextTag);
        if (existingView != null) {
            // The EditText already exists, so we don't need to add a new one
            Log.i("addLabeledEditText", "EditText with tag " + editTextTag + " already exists.");
            return;
        }

        // Create and add the EditText
        EditText editText = new EditText(context);
        editText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        editText.setHint(hintText);
        // Generate an ID and set a tag for the EditText
        editText.setId(View.generateViewId());
        editText.setTag(simplifiedLabel.replace(" ", "") + "EditText");

        container.addView(editText);
    }



    @Override
    public int getItemCount() {
        return titles.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, detailTextView;
        LinearLayout editTextContainer; // Container for dynamically adding labeled EditTexts

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            detailTextView = itemView.findViewById(R.id.detailTextView);
            editTextContainer = itemView.findViewById(R.id.editTextContainer);
        }
    }

    public void updateAccordionSettings(byte [] registerPacket){
        int freq2 = registerPacket[7] & 0xFF;
        int freq1 = registerPacket[8] & 0xFF;
        int freq0 = registerPacket[9] & 0xFF;

        // Convert the frequency bytes to a single integer
        long frequency = ((freq2 << 16) | (freq1 << 8) | freq0);
        // Assuming the oscillator frequency is 26 MHz
        double fOsc = 26e6; // 26 MHz
        double frequencyMHz = frequency * (fOsc / Math.pow(2, 16)) / 1e6; // Convert to MHz
        Log.i("frequencyMHz", ""+frequencyMHz);

        EditText frequencyEditText = viewHolder.editTextContainer.findViewWithTag("CenterFrequencyEditText");
        if (frequencyEditText != null) {
            frequencyEditText.setText(String.format(Locale.getDefault(), "%.6f", frequencyMHz));
        }
    }




}

