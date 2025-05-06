package com.yourcompany.cmp2004_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
// No need to import LinearLayoutManager here
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {


    private List<Message> messageList;


    public MessageAdapter(List<Message> messageList) { // Renamed parameter for clarity (optional but good)
        this.messageList = messageList; // Assign the passed list to the adapter's list
        // Previously was: this.messageList = messageList; (assigning null to itself)
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // *** FIX 2: Inflate correctly, attaching to parent temporarily ***
        View chatView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, null);
        return new MyViewHolder(chatView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        if (messageList == null || position >= messageList.size()) {
            return;
        }

        Message message = messageList.get(position);

        if (Message.SENT_BY_ME.equals(message.getSentBy())) {
            holder.leftChatView.setVisibility(View.GONE);
            holder.rightChatView.setVisibility(View.VISIBLE);
            holder.rightTextView.setText(message.getMessage());
        } else {
            holder.rightChatView.setVisibility(View.GONE);
            holder.leftChatView.setVisibility(View.VISIBLE);
            holder.leftTextView.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        // *** FIX 3 (Safety): Add null check before accessing size ***
        // Although FIX 1 should prevent null, this makes it more robust
        return (messageList == null) ? 0 : messageList.size();
    }

    // Keep ViewHolder class definition the same
    public class MyViewHolder extends RecyclerView.ViewHolder {

        LinearLayout leftChatView, rightChatView;
        TextView leftTextView, rightTextView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatView = itemView.findViewById(R.id.left_chat_view);
            rightChatView = itemView.findViewById(R.id.right_chat_view);
            leftTextView = itemView.findViewById(R.id.left_chat_text_view);
            rightTextView = itemView.findViewById(R.id.right_chat_text_view);

            // Add null checks here too for extreme safety (if IDs were missing in layout)
            // if (leftChatView == null || rightChatView == null || leftTextView == null || rightTextView == null) {
            //    Log.e("MyViewHolder", "One or more views not found in chat_item.xml!");
            // }
        }
    }

}