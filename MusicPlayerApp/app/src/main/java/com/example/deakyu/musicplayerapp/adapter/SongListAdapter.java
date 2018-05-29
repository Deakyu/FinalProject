package com.example.deakyu.musicplayerapp.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.deakyu.musicplayerapp.R;
import com.example.deakyu.musicplayerapp.model.Song;

import java.util.List;

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.ViewHolder>{

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public CardView row;
        public TextView title;
        public TextView artist;

        public ViewHolder(View iv) {
            super(iv);

            row = iv.findViewById(R.id.song_row);
            title = iv.findViewById(R.id.title);
            artist = iv.findViewById(R.id.artist);
            row.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(clickListener != null) clickListener.onClick(v, getAdapterPosition());
        }
    }

    private final LayoutInflater inflater;
    private List<Song> songs;
    private Context c;
    private ItemClickListener clickListener;

    public void setClickListener(ItemClickListener itemClickListener) {
        this.clickListener = itemClickListener;
    }

    public SongListAdapter(Context c) {
        this.inflater = LayoutInflater.from(c);
        this.c = c;
    }

    public SongListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.song_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder vh, int pos) {
        Song curSong = songs.get(pos);
        String title = isNull(curSong.getTitle()) ? "Not Available" : curSong.getTitle();
        String artist = isNull(curSong.getArtist()) ? "Not Available" : curSong.getArtist();

        vh.row.setUseCompatPadding(true);
        vh.title.setText(title);
        vh.artist.setText(artist);
    }

    private boolean isNull(String str) {
        return str == null || str.equals("");
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }
}
