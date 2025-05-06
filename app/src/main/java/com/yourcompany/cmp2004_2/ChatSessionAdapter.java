package com.yourcompany.cmp2004_2; // Adjust package

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yourcompany.cmp2004_2.db.ChatSessionEntity; // Import your entity
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.SessionViewHolder> {

    private List<ChatSessionEntity> sessions = new ArrayList<>();
    private OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onSessionClick(ChatSessionEntity session);
    }

    public ChatSessionAdapter(OnSessionClickListener listener) {
        this.listener = listener;
    }

    public void setSessions(List<ChatSessionEntity> newSessions) {
        this.sessions.clear();
        if (newSessions != null) {
            this.sessions.addAll(newSessions);
        }
        notifyDataSetChanged(); // Or use DiffUtil for better performance
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false); // Or your custom layout
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ChatSessionEntity session = sessions.get(position);
        holder.bind(session, listener);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView text1; // For session ID or snippet
        TextView text2; // For timestamp

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }

        void bind(final ChatSessionEntity session, final OnSessionClickListener listener) {
            String title = "Chat: " + session.sessionId.substring(0, Math.min(session.sessionId.length(), 8)) + "...";
            if (session.lastMessageSnippet != null && !session.lastMessageSnippet.isEmpty()) {
                title = session.lastMessageSnippet.substring(0, Math.min(session.lastMessageSnippet.length(), 30)) + "...";
            }
            text1.setText(title);

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            text2.setText("Last active: " + sdf.format(new Date(session.lastMessageTimestamp)));

            itemView.setOnClickListener(v -> listener.onSessionClick(session));
        }
    }
}