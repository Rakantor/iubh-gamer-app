package com.example.iubhgamerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iubhgamerapp.ui.ChatFragment;

import java.util.List;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.MessageViewHolder> {
    private List<ChatFragment.ChatMessage> chatMessages;

    public RVAdapter(List<ChatFragment.ChatMessage> chatMessages){
        this.chatMessages = chatMessages;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private CardView cv;
        private TextView sender, time, text;

        MessageViewHolder(View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.cv);
            sender = itemView.findViewById(R.id.chat_sender);
            time = itemView.findViewById(R.id.chat_time);
            text = itemView.findViewById(R.id.chat_text);
        }
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview_item, viewGroup, false);
        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder messageViewHolder, int i) {
        messageViewHolder.sender.setText(chatMessages.get(i).senderName);
        messageViewHolder.time.setText(chatMessages.get(i).date);
        messageViewHolder.text.setText(chatMessages.get(i).text);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
}