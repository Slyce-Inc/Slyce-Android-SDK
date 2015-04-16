package com.android.slyce.report.mpmetrics;

public interface ResourceIds {
    public boolean knownIdName(String name);
    public int idFromName(String name);
    public String nameForId(int id);
}
