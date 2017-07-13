package com.sensibility_testbed;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.snakei.MiscInfoService;
import com.snakei.SensorService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Privacy Config Fragment Layout : customizing sensor settings mode
 */

public class ConfigFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // grab the container and layout
        View configContainer = inflater.inflate(R.layout.config, null);
        LinearLayout layout = (LinearLayout) configContainer.findViewById(R.id.sensorList);

        // make the ui labels for different categories
        TextView networkTitles = new TextView(getActivity());
        networkTitles.setText("Networks");
        TextView sensorTitles = new TextView(getActivity());
        sensorTitles.setText("Sensors");

        //set label style
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.setMargins(20, 40, 0, 0);

        networkTitles.setTypeface(networkTitles.getTypeface(), Typeface.BOLD);
        networkTitles.setPaintFlags(networkTitles.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);
        networkTitles.setLayoutParams(labelParams);

        sensorTitles.setTypeface(sensorTitles.getTypeface(), Typeface.BOLD);
        sensorTitles.setPaintFlags(sensorTitles.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);
        sensorTitles.setLayoutParams(labelParams);

        // instantiate and initialize misc and sensor service
        SensorService sensorService = new SensorService();
        MiscInfoService miscService = new MiscInfoService();
        sensorService.init(getContext());
        miscService.init(getContext());

        // first add the networks
        layout.addView(networkTitles);

        try {
            // get the networks list
            String networks = miscService.getNetworkInfo();
            // System.out.println(sensors);

            //parse the json array and go through each sensor
            JSONArray networkArray = new JSONArray(networks);
            for (int i = 0; i < networkArray.length(); i++) {
                JSONObject networkObject = networkArray.getJSONObject(i);

                // get the sensor name and power
                String name = networkObject.get("show_name").toString();
                String powerString = "50";
                Double powerDouble = Double.parseDouble(powerString);
                String type = "network";
                int power = powerDouble.intValue();

                // make the ui components
                TextView networkLabel = new TextView(getActivity());
                TextView accuracy = new TextView(getActivity());
                TextView count = new TextView(getActivity());

                SeekBar networkBar = new SeekBar(getActivity());
                SeekBar networkBar2 = new SeekBar(getActivity());

                // add listener for seekbar
                SeekBar.OnSeekBarChangeListener networkBarListener = new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        //Executed when progress is changed
                        // System.out.println(progress);
                    }
                };


                // add listener for seekbar2
                SeekBar.OnSeekBarChangeListener networkBarListener2 = new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        //Executed when progress is changed
                        // System.out.println(progress);
                    }
                };
                networkBar.setOnSeekBarChangeListener(networkBarListener);
                networkBar2.setOnSeekBarChangeListener(networkBarListener2);

                // update info for ui
                networkLabel.setText(name);
                networkLabel.setGravity(Gravity.RIGHT);
                networkLabel.setTypeface(networkLabel.getTypeface(), Typeface.BOLD);
                networkLabel.setLayoutParams(labelParams);

                accuracy.setText("Accuracy:");
                LinearLayout.LayoutParams labelParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                labelParams2.setMargins(30, 40, 0, 0);
                accuracy.setLayoutParams(labelParams2);

                count.setText("Count:");
                LinearLayout.LayoutParams labelParams3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                labelParams3.setMargins(40, 40, 0, 0);
                count.setLayoutParams(labelParams3);

                // add the network label
                layout.addView(networkLabel);
                layout.addView(accuracy);
                // add the network bar
                networkBar.setProgress(power);
                layout.addView(networkBar);
                layout.addView(count);
                networkBar2.setProgress(power);
                layout.addView(networkBar2);

            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

        //now add the sensor title
        layout.addView(sensorTitles);

        try {
            // get the sensor list
            String sensors = sensorService.getSensorList();
            // System.out.println(sensors);

            //parse the json array and go through each sensor
            JSONArray sensorArray = new JSONArray(sensors);
            for (int i = 0; i < sensorArray.length(); i++) {
                JSONObject sensorObject = sensorArray.getJSONObject(i);

                // get the sensor name and power
                String name = sensorObject.get("show_name").toString();
                String powerString = sensorObject.get("power").toString();
                Double powerDouble = Double.parseDouble(powerString);
                String type = sensorObject.get("string_type").toString();
                int power = powerDouble.intValue();

                // make the ui components
                TextView sensorLabel = new TextView(getActivity());
                TextView accuracy = new TextView(getActivity());
                TextView count = new TextView(getActivity());

                SeekBar sensorBar = new SeekBar(getActivity());
                SeekBar sensorBar2 = new SeekBar(getActivity());

                // add listener for seekbar
                SeekBar.OnSeekBarChangeListener sensorBarListener = new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        //Executed when progress is changed
                        // System.out.println(progress);
                    }
                };


                // add listener for seekbar2
                SeekBar.OnSeekBarChangeListener sensorBarListener2 = new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        //Executed when progress is changed
                        // System.out.println(progress);
                    }
                };
                sensorBar.setOnSeekBarChangeListener(sensorBarListener);
                sensorBar2.setOnSeekBarChangeListener(sensorBarListener2);

                // update info for ui
                sensorLabel.setText(name);
                sensorLabel.setGravity(Gravity.RIGHT);
                sensorLabel.setTypeface(sensorLabel.getTypeface(), Typeface.BOLD);
                sensorLabel.setLayoutParams(labelParams);

                accuracy.setText("Accuracy:");
                LinearLayout.LayoutParams labelParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                labelParams2.setMargins(30, 40, 0, 0);
                accuracy.setLayoutParams(labelParams2);

                count.setText("Count:");
                LinearLayout.LayoutParams labelParams3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                labelParams3.setMargins(40, 40, 0, 0);
                count.setLayoutParams(labelParams3);

                // add the sensor label
                layout.addView(sensorLabel);
                layout.addView(accuracy);
                // add the sensor bar
                sensorBar.setProgress(power);
                layout.addView(sensorBar);
                layout.addView(count);
                sensorBar2.setProgress(power);
                layout.addView(sensorBar2);

            }


        } catch (JSONException e) {
            e.printStackTrace();
        }


        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.config, container, false);
        return configContainer;
    }
}
