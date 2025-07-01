package org.rti.ttfinder.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.rti.ttfinder.R;
import org.rti.ttfinder.data.entity.Assessment;
import org.rti.ttfinder.listeners.ItemClickListener;
import org.rti.ttfinder.utils.AppUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AssessmentAdapter extends RecyclerView.Adapter<AssessmentAdapter.MyViewHolder> {

    private ArrayList<Assessment> dataList;
    private Context mContext;
    private ItemClickListener mListenr;

    public AssessmentAdapter(ArrayList<Assessment> dataList, Context mContext) {
        this.dataList = dataList;
        this.mContext = mContext;
    }

    @NonNull
    @NotNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assessment_new, parent, false);

        return new MyViewHolder(itemView);

    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull MyViewHolder holder, int position) {
        Assessment  assessment = dataList.get(position);

        holder.tvIdentifier.setText("TTID: "+assessment.getTt_tracker_id());
        holder.tvDateTime.setText(assessment.getAssessment_ended());

    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tvIdentifier, tvDateTime;
        public MyViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            tvIdentifier = itemView.findViewById(R.id.tvId);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
        }
    }

    public void setOnItemClickListenr(ItemClickListener mListenr){
        if(mListenr != null)
            this.mListenr = mListenr;
    }
}
