package org.rti.ttfinder.data.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.rti.ttfinder.models.ClassificationQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AppPreference {

    // declare context
    private static Context mContext;

    // singleton
    private static AppPreference appPreference = null;

    // common
    private SharedPreferences sharedPreferences, settingsPreferences;
    private SharedPreferences.Editor editor;

    public static AppPreference getInstance(Context context) {
        if(appPreference == null) {
            mContext = context;
            appPreference = new AppPreference();
        }
        return appPreference;
    }
    private AppPreference() {
        sharedPreferences = mContext.getSharedPreferences(PrefKey.APP_PREF_NAME, Context.MODE_PRIVATE);
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        editor = sharedPreferences.edit();
    }

    public void setString(String key, String value) {
        editor.putString(key , value);
        editor.commit();
    }
    public String getString(String key) {
        return sharedPreferences.getString(key, null);
    }

    public void setBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.commit();
    }
    public Boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void setInteger(String key, int value) {
        editor.putInt(key, value);
        editor.commit();
    }

    public int getInteger(String key) {
        return sharedPreferences.getInt(key, -1);
    }

    public void setStringArray(String key, ArrayList<String> values) {
        if (values != null && !values.isEmpty()) {
            String value = "";
            for (String str : values) {
                if(value.isEmpty()) {
                    value = str;
                } else {
                    value = value + "," + str;
                }
            }
            setString(key, value);
        } else if(values == null) {
            setString(key, null);
        }
    }

    public ArrayList<String> getStringArray(String key) {
        ArrayList<String> arrayList = new ArrayList<>();
        String value = getString(key);
        if (value != null) {
            arrayList = new ArrayList<>(Arrays.asList(value.split(",")));
        }
        return arrayList;
    }

    public void remove(String key){
        editor.remove(key);
        editor.commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean isExistTTID(String assessmentId){
        String queues = getString(PrefKey.GRADING_QUEUE);
        if(queues != null){
            ArrayList<ClassificationQueue> classificationQueues = new Gson().fromJson(queues, new TypeToken<ArrayList<ClassificationQueue>>(){}.getType());
            List<ClassificationQueue> updatedQueues = classificationQueues.stream()
                    .filter(q -> q.getAssessment().getTt_tracker_id().equals(assessmentId))
                    .collect(Collectors.toList());
            System.out.println("updatedQueues => "+updatedQueues.size());
            return !updatedQueues.isEmpty();
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void removeGradingFromQueue(String assessmentId){
        String queues = getString(PrefKey.GRADING_QUEUE);
        if(queues != null){
            ArrayList<ClassificationQueue> classificationQueues = new Gson().fromJson(queues, new TypeToken<ArrayList<ClassificationQueue>>(){}.getType());
            List<ClassificationQueue> updatedQueues = classificationQueues.stream()
                    .filter(q -> !q.getAssessment().getTt_tracker_id().equals(assessmentId))
                    .collect(Collectors.toList());
            String updatedQueue = new Gson().toJson(updatedQueues);
            //System.out.println(updatedQueue);
            setString(PrefKey.GRADING_QUEUE, updatedQueue);
        }
    }
}

