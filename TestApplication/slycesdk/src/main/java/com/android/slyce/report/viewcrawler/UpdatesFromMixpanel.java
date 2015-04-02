package com.android.slyce.report.viewcrawler;

import com.android.slyce.report.mpmetrics.Tweaks;

import org.json.JSONArray;

/* This interface is for internal use in the Mixpanel library, and should not be
   implemented in client code. */
public interface UpdatesFromMixpanel {
    public void setEventBindings(JSONArray bindings);
    public Tweaks getTweaks();
}
