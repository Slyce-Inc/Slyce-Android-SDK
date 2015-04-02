package com.android.slyce.report.mpmetrics;

/**
 * For use with {@link com.android.slyce.report.mpmetrics.MixpanelAPI.People#addOnMixpanelUpdatesReceivedListener(com.android.slyce.report.mpmetrics.OnMixpanelUpdatesReceivedListener)}
 */
/* package */ interface OnMixpanelUpdatesReceivedListener {
    /**
     * Called when the Mixpanel library has updates, for example, Surveys or Notifications.
     * This method will not be called once per update, but rather any time a batch of updates
     * becomes available. The related updates can be checked with
     * {@link com.android.slyce.report.mpmetrics.MixpanelAPI.People#getSurveyIfAvailable()} or {@link com.android.slyce.report.mpmetrics.MixpanelAPI.People#getNotificationIfAvailable()}
     */
    public void onMixpanelUpdatesReceived();
}
