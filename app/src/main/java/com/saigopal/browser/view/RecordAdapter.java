package com.saigopal.browser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.saigopal.browser.R;
import com.saigopal.browser.database.Record;


import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RecordAdapter extends ArrayAdapter<Record> {
    private final Context context;
    private final int layoutResId;
    private final List<Record> list;
    private final boolean mainScreen;

    public RecordAdapter(Context context, List<Record> list,boolean mainScreen) {
        super(context, R.layout.list_item, list);
        this.context = context;
        this.layoutResId = R.layout.list_item;
        this.list = list;
        this.mainScreen = mainScreen;
    }

    private static class Holder {
        TextView title;
        TextView time;
        ImageView icon;
    }

    @SuppressLint("SetTextI18n")
    @SuppressWarnings("NullableProblems")
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Holder holder;
        View view = convertView;

            if (view == null) {
                if (mainScreen)
                {
                    view = LayoutInflater.from(context).inflate(R.layout.main_list_items, parent, false);
                    holder = new Holder();
                    holder.title = view.findViewById(R.id.record_item_title);
                    holder.time = view.findViewById(R.id.title_icon);
                }
                else {
                    view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
                    holder = new Holder();
                    holder.title = view.findViewById(R.id.record_item_title);
                    holder.time = view.findViewById(R.id.record_item_time);
                    holder.icon = view.findViewById(R.id.record_item_icon);
                }

                view.setTag(holder);

            } else {
                holder = (Holder) view.getTag();
            }

            Record record = list.get(position);
            if (mainScreen){
                holder.title.setText(record.getTitle());
                holder.time.setText(record.getTitle().charAt(0)+"");
            }
            else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                holder.title.setText(record.getTitle());
                holder.time.setText(sdf.format(record.getTime()));

                if (record.getTime() == 11) {
                    holder.icon.setImageResource(R.drawable.circle_red_big);
                } else if (list.get(position).getTime() == 10) {
                    holder.icon.setImageResource(R.drawable.circle_pink_big);
                } else if (record.getTime() == 9) {
                    holder.icon.setImageResource(R.drawable.circle_purple_big);
                } else if (record.getTime() == 8) {
                    holder.icon.setImageResource(R.drawable.circle_blue_big);
                } else if (record.getTime() == 7) {
                    holder.icon.setImageResource(R.drawable.circle_teal_big);
                } else if (record.getTime() == 6) {
                    holder.icon.setImageResource(R.drawable.circle_green_big);
                } else if (record.getTime() == 5) {
                    holder.icon.setImageResource(R.drawable.circle_lime_big);
                } else if (record.getTime() == 4) {
                    holder.icon.setImageResource(R.drawable.circle_yellow_big);
                } else if (record.getTime() == 3) {
                    holder.icon.setImageResource(R.drawable.circle_orange_big);
                } else if (record.getTime() == 2) {
                    holder.icon.setImageResource(R.drawable.circle_brown_big);
                } else if (record.getTime() == 1) {
                    holder.icon.setImageResource(R.drawable.circle_grey_big);
                } else {
                    holder.icon.setImageResource(R.drawable.circle_red_big);
                }

            }


        return view;
    }
}