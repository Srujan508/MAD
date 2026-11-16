package com.example.miniproject;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying list of messages in Dashboard & Category views.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Message message);
    }

    private final Context context;
    private List<Message> messageList;
    private final OnItemClickListener listener;

    public MessageAdapter(Context context, List<Message> messageList, OnItemClickListener listener) {
        this.context = context;
        this.messageList = messageList != null ? messageList : new ArrayList<Message>();
        this.listener = listener;
    }

    public void updateList(List<Message> newList) {
        this.messageList = newList != null ? newList : new ArrayList<Message>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        final Message message = messageList.get(position);
        holder.tvSender.setText(message.getSender());
        holder.tvDate.setText(message.getFormattedDate());
        holder.tvBody.setText(message.getBody());

        String category = message.getCategory();
        holder.tvCategoryBadge.setText(category);

        int catColor = SMSCategorizer.getCategoryColor(context, category);
        int catBgColor = SMSCategorizer.getCategoryBgColor(context, category);

        // Customize Category Badge Chip
        holder.tvCategoryBadge.setTextColor(catColor);
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(catBgColor);
        chipBg.setCornerRadius(18f);
        chipBg.setStroke(2, catColor);
        holder.tvCategoryBadge.setBackground(chipBg);

        // Customize Icon & Avatar Circle
        holder.imgCategoryIcon.setImageResource(SMSCategorizer.getCategoryIconRes(category));
        holder.imgCategoryIcon.setColorFilter(catColor);

        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(catBgColor);
        holder.avatarFrame.setBackground(avatarBg);

        // Click listener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(message);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        FrameLayout avatarFrame;
        ImageView imgCategoryIcon;
        TextView tvSender;
        TextView tvCategoryBadge;
        TextView tvDate;
        TextView tvBody;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarFrame = itemView.findViewById(R.id.avatarFrame);
            imgCategoryIcon = itemView.findViewById(R.id.imgCategoryIcon);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvBody = itemView.findViewById(R.id.tvBody);
        }
    }
}
