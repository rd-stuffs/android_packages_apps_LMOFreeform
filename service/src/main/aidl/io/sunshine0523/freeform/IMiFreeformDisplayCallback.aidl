// IMiFreeformDisplayCallback.aidl
package io.sunshine0523.freeform;

/** {@hide} */
@PermissionManuallyEnforced
oneway interface IMiFreeformDisplayCallback {
    void onDisplayPaused();
    void onDisplayResumed();
    void onDisplayStopped();
    void onDisplayAdd(int displayId);
}
