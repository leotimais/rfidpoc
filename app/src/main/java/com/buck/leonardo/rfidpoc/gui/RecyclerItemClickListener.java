package com.buck.leonardo.rfidpoc.gui;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerItemClickListener extends RecyclerView.SimpleOnItemTouchListener {
    private RecyclerView recyclerView;
    private OnItemClickListener onItemClickListener;
    private GestureDetector gestureDetector;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
        void onLongItemClick(View view, int position);
    }

    public RecyclerItemClickListener(Context context, RecyclerView recycler, OnItemClickListener listener) {
        recyclerView = recycler;
        onItemClickListener = listener;
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());

                if (child != null && onItemClickListener != null) {
                    onItemClickListener.onItemClick(child, recyclerView.getChildAdapterPosition(child));
                    return true;
                }
                return false;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());

                if (child != null && onItemClickListener != null) {
                    onItemClickListener.onLongItemClick(child, recyclerView.getChildAdapterPosition(child));
                }
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        return gestureDetector.onTouchEvent(e);
    }
}
