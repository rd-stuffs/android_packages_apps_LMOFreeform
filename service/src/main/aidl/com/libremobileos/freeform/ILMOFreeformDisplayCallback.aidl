// ILMOFreeformDisplayCallback.aidl
package com.libremobileos.freeform;

/** {@hide} */
@PermissionManuallyEnforced
oneway interface ILMOFreeformDisplayCallback {
    void onDisplayPaused();
    void onDisplayResumed();
    void onDisplayStopped();
    void onDisplayAdd(int displayId);
}
