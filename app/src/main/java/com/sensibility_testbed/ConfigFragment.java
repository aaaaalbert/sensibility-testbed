package com.sensibility_testbed;

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

        // instantiate and initialize sensor service
        SensorService sensorService = new SensorService();
        sensorService.init(getContext());

        try {
            // get the sensor list
            String sensors = sensorService.getSensorList();

            //parse the json array and go through each sensor
            JSONArray sensorArray = new JSONArray(sensors);
            for (int i = 0; i < sensorArray.length(); i++) {
                JSONObject sensorObject = sensorArray.getJSONObject(i);

                // get the sensor name and power
                String name = sensorObject.get("name").toString();
                String powerString = sensorObject.get("power").toString();
                Double powerDouble = Double.parseDouble(powerString);
                int power = powerDouble.intValue();

                // make the ui components
                TextView sensorLabel = new TextView(getActivity());
                SeekBar sensorBar = new SeekBar(getActivity());

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
                sensorBar.setOnSeekBarChangeListener(sensorBarListener);

                // update info for ui
                sensorLabel.setText(name);
                sensorLabel.setGravity(Gravity.RIGHT);
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                labelParams.setMargins(20, 40, 0, 0);
                sensorLabel.setLayoutParams(labelParams);
                // add the sensor label
                layout.addView(sensorLabel);
                // add the sensor bar
                sensorBar.setProgress(power);
                layout.addView(sensorBar);
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }


        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.config, container, false);
        return configContainer;
    }
}
