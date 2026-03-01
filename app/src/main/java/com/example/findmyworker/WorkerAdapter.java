package com.example.findmyworker;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
import java.util.Locale;

public class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder> {

    private Context context;
    private List<Customer> workers;
    private Location clientLocation;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Customer worker);
    }

    public WorkerAdapter(Context context, List<Customer> workers, Location clientLocation, OnItemClickListener listener) {
        this.context = context;
        this.workers = workers;
        this.clientLocation = clientLocation;
        this.listener = listener;
    }

    public void setClientLocation(Location clientLocation) {
        this.clientLocation = clientLocation;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_worker, parent, false);
        return new WorkerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkerViewHolder holder, int position) {
        Customer worker = workers.get(position);

        holder.tvName.setText((position + 1) + ". " + worker.getName() + " (" + worker.getRanking() + "★)");
        holder.tvSpecialties.setText(worker.getSpecialties());

        if (clientLocation != null && worker.getLatitude() != 0.0) {
            float[] results = new float[1];
            Location.distanceBetween(clientLocation.getLatitude(), clientLocation.getLongitude(),
                    worker.getLatitude(), worker.getLongitude(), results);
            String distStr = String.format(Locale.getDefault(), "%.1f km away", results[0] / 1000.0);
            holder.tvDistance.setText(distStr);
            holder.tvDistance.setVisibility(View.VISIBLE);
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        if (worker.getProfilePictureUrl() != null && !worker.getProfilePictureUrl().isEmpty()) {
            Glide.with(context)
                    .load(worker.getProfilePictureUrl())
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(worker));
    }

    @Override
    public int getItemCount() {
        return workers != null ? workers.size() : 0;
    }

    public static class WorkerViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecialties, tvDistance;
        ShapeableImageView ivProfile;

        public WorkerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvWorkerName);
            tvSpecialties = itemView.findViewById(R.id.tvWorkerSpecialties);
            tvDistance = itemView.findViewById(R.id.tvWorkerDistance);
            ivProfile = itemView.findViewById(R.id.ivWorkerProfile);
        }
    }
}