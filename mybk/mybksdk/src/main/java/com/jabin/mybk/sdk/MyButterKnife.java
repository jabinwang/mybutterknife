package com.jabin.mybk.sdk;

import android.app.Activity;

public class MyButterKnife {

    public static void bind(Activity activity){
        String className = activity.getClass().getCanonicalName() + "$ViewBinder";

        try {
            Class<?> viewBinderClass = Class.forName(className);
            ViewBinder<Activity> viewBinder = (ViewBinder<Activity>) viewBinderClass.newInstance();
            viewBinder.bind(activity);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }
}
