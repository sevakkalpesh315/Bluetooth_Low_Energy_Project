package com.ublox.BLE.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.ublox.BLE.R;
import com.ublox.BLE.activities.MainActivity;

public class OverviewFragment extends Fragment {


    public interface IOverviewFragmentInteraction {
        void onGreenLight(boolean enabled);
        void onRedLight(boolean enabled);
        void setStartSession();
        void setStopSession();
        void setListSession();
        void setPauseSession();
        void setDetailsSession();
    }

    public static OverviewFragment newInstance() {
        OverviewFragment fragment = new OverviewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public OverviewFragment() {
        // Required empty public constructor
    }

    private IOverviewFragmentInteraction mInteractionListener;
Button startSession, stopSession, listSession, pauseSession, detailsSession;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_overview, container, false);

    }

    private Switch sGreen;
    private Switch sRed;
    private TextView mError;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sGreen = (Switch) view.findViewById(R.id.sGreenLight);
        sRed = (Switch) view.findViewById(R.id.sRedLight);
        startSession=(Button)view.findViewById(R.id.btnStartSession);
        stopSession=(Button)view.findViewById(R.id.btnStopSession);
        listSession=(Button)view.findViewById(R.id.btnStartList);
        detailsSession=(Button)view.findViewById(R.id.btnSessionDetails);

        pauseSession=(Button)view.findViewById(R.id.btnPauseSession);
        mError=(TextView) view.findViewById(R.id.tvError);

        sGreen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mInteractionListener != null && ((MainActivity) getActivity()).isConnected()) {
                    mInteractionListener.onGreenLight(isChecked);
                } else {
                    sGreen.setChecked(!isChecked);
                }
            }
        });

        sRed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mInteractionListener != null && ((MainActivity) getActivity()).isConnected()) {
                    mInteractionListener.onRedLight(isChecked);
                } else {
                    sRed.setChecked(!isChecked);
                }
            }
        });

        stopSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mInteractionListener != null && ((MainActivity) getActivity()).isConnected()) {
                    mInteractionListener.setStopSession();
                }

            }
        });
        listSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mInteractionListener != null && ((MainActivity) getActivity()).isConnected()) {
                    mInteractionListener.setListSession();
                }

            }
        });
        startSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mInteractionListener != null && ((MainActivity) getActivity()).isConnected()) {
                    mInteractionListener.setStartSession();
                }

            }
        });

        pauseSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mInteractionListener != null && ((MainActivity) getActivity()).isConnected()) {
                    mInteractionListener.setPauseSession();
                }

            }
        });

        detailsSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mInteractionListener != null && ((MainActivity) getActivity()).isConnected()) {
                    mInteractionListener.setDetailsSession();
                }

            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mInteractionListener = (IOverviewFragmentInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement IOverviewFragmentInteraction");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mInteractionListener = null;
    }
}
