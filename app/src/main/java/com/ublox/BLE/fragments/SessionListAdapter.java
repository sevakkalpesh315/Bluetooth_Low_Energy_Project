package com.ublox.BLE.fragments;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ublox.BLE.R;

import java.nio.BufferUnderflowException;
import java.util.Arrays;

/**
 * Created by Kalpesh on 22/09/2017.
 */

public class SessionListAdapter extends RecyclerView.Adapter<SessionListAdapter.MyViewHolder> {
    byte[][] list;

    public SessionListAdapter(byte[][] list) {
        this.list = list;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card,parent,false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
//        holder.tvJoke.setText(jokes.get(position).getMessage());
        String a = Arrays.toString(list[position]);
        Log.i("value",a);
        byte[] id = Arrays.copyOfRange(list[position],0,2);
        byte[] activity = Arrays.copyOfRange(list[position],2,4);
        byte[] startTime = Arrays.copyOfRange(list[position],4,8);
        byte[] duration = Arrays.copyOfRange(list[position],8,12);
        byte[] memory = Arrays.copyOfRange(list[position],12,16);

        Log.i("id",Arrays.toString(id));
        Log.i("activity",Arrays.toString(activity));
        Log.i("startTime",Arrays.toString(startTime));
        Log.i("duration",Arrays.toString(duration));
        Log.i("memory",Arrays.toString(memory));
        holder.tvSessionID.setText("SessionID: " + covertToString(id));
        holder.tvSessionAvitity.setText("Session Avitity: " + covertToString(activity));
        holder.tvStartTime.setText("Start Time:" + covertToString(startTime));
        holder.tvDuration.setText("Duration:" + covertToString(duration));
        holder.tvMemory.setText("Memory: " + covertToString(memory));
    }

    private String covertToString(byte[] input){
        long output = 0;
        try {
            for(byte b: input){
                output += b & 0xff;
            }
            return String.valueOf(toLong_STR(input));
        }   catch(BufferUnderflowException e){
            return "";
        } catch(NumberFormatException e){
            return "";
        }
    }

    public static long bytesToLong(byte[] bytes) {
        if (bytes.length > 8) {
//            throw new IllegalMethodParameterException("byte should not be more than 8 bytes");

        }
        long r = 0;
        for (int i = 0; i < bytes.length; i++) {
            r = r << 8;
            r += bytes[i];
        }

        return r;
    }

    static long toLong_STR(byte[] b)
    {
        long value = 0;
        for (int i = 0; i < b.length; i++)
        {
            value += ((long) b[i] & 0xffL) << (8 * i);
        }
        return value;
    }




    @Override
    public int getItemCount() {
        return list.length;
    }


    public class MyViewHolder extends RecyclerView.ViewHolder{
        TextView tvSessionID;
        TextView tvSessionAvitity;
        TextView tvStartTime;
        TextView tvDuration;
        TextView tvMemory;

        public MyViewHolder(View itemView) {
            super(itemView);
            tvSessionID = (TextView) itemView.findViewById(R.id.tvSessionID);
            tvSessionAvitity = (TextView) itemView.findViewById(R.id.tvSessionAvitity);
            tvStartTime = (TextView) itemView.findViewById(R.id.tvStartTime);
            tvDuration = (TextView) itemView.findViewById(R.id.tvDuration);
            tvMemory = (TextView) itemView.findViewById(R.id.tvMemory);
        }
    }


}