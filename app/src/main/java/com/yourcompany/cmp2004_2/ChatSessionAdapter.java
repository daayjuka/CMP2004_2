package com.yourcompany.cmp2004_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // For delete button
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yourcompany.cmp2004_2.db.ChatSessionEntity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.SessionViewHolder> {

    private List<ChatSessionEntity> sessions = new ArrayList<>();
    private OnSessionActionsListener listener; // Renamed for clarity and new action

    // Updated interface to include delete action
    public interface OnSessionActionsListener {
        void onSessionClick(ChatSessionEntity session);
        void onDeleteSessionClick(ChatSessionEntity session); // New callback
    }

    public ChatSessionAdapter(OnSessionActionsListener listener) {
        this.listener = listener;
    }

    public void setSessions(List<ChatSessionEntity> newSessions) {
        this.sessions.clear();
        if (newSessions != null) {
            this.sessions.addAll(newSessions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // You should create a custom layout for your session item for better control
        // For now, I'll adapt the simple_list_item_2 concept but ideally, you'd inflate a custom XML.
        // Let's assume you create R.layout.item_chat_session.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_session, parent, false); // INFLATE YOUR CUSTOM LAYOUT
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
        TextView sessionTitleTextView;
        TextView sessionTimestampTextView;
        ImageButton deleteSessionButton; // Reference to delete button

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            // These IDs must match the ones in your item_chat_session.xml
            sessionTitleTextView = itemView.findViewById(R.id.session_title_text_view);
            sessionTimestampTextView = itemView.findViewById(R.id.session_timestamp_text_view);
            deleteSessionButton = itemView.findViewById(R.id.delete_session_button);
        }

        void bind(final ChatSessionEntity session, final OnSessionActionsListener listener) {
            String title = "Chat: " + session.sessionId.substring(0, Math.min(session.sessionId.length(), 8)) + "...";
            if (session.lastMessageSnippet != null && !session.lastMessageSnippet.isEmpty()) {
                title = session.lastMessageSnippet.substring(0, Math.min(session.lastMessageSnippet.length(), 30)) + "...";
            }
            sessionTitleTextView.setText(title);

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            sessionTimestampTextView.setText("Last active: " + sdf.format(new Date(session.lastMessageTimestamp)));

            itemView.setOnClickListener(v -> listener.onSessionClick(session));

            // Set listener for the delete button
            if (deleteSessionButton != null) { // Check if the button exists
                deleteSessionButton.setOnClickListener(v -> listener.onDeleteSessionClick(session));
            }
        }
    }
}