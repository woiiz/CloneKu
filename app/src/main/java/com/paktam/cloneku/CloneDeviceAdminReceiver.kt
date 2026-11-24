package com.paktam.cloneku

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class CloneDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "CloneKu: Device Admin aktif — Work Profile boleh dibuat", Toast.LENGTH_LONG).show()
    }
    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "CloneKu: Device Admin dimatikan", Toast.LENGTH_LONG).show()
    }
}
