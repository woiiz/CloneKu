package com.paktam.cloneku

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.FileProvider
import java.io.File

/**
 * Clone via Work Profile (Managed Profile) — cara rasmi Android.
 * App yang sama install 2 kali: 1 di Personal, 1 di Work. Data berasingan, function sama 100%.
 */
class CloneManager(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val um = context.getSystemService(Context.USER_SERVICE) as UserManager

    fun hasWorkProfile(): Boolean {
        return try {
            um.userProfiles.size > 1
        } catch (e: Exception) { false }
    }

    fun workProfileHandle(): UserHandle? {
        return try { um.userProfiles.firstOrNull { it != android.os.Process.myUserHandle() } } catch (e: Exception) { null }
    }

    /** Intent rasmi untuk cipta Managed Profile. User kena confirm di skrin Android. */
    fun createManagedProfileIntent(): Intent? {
        return try {
            val admin = ComponentName(context, CloneDeviceAdminReceiver::class.java)
            Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
                putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, admin)
                putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true)
            }
        } catch (e: Exception) { null }
    }

    /**
     * Install app yang sama ke dalam Work Profile.
     * Android tak benarkan install APK direct cross-profile dari app biasa tanpa Device Owner,
     * jadi cara yang berfungsi: buka Play Store dalam Work Profile, atau copy APK dan arahkan
     * PackageInstaller dalam profile tu. Di sini kita lancarkan intent install cross-profile
     * dan buka launcher work profile supaya user tekan Install.
     */
    fun installIntoWorkProfile(packageName: String, context: Context): Boolean {
        return try {
            val handle = workProfileHandle() ?: return false
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            // Cuba lancarkan Play Store dalam Work Profile untuk package tu
            val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            launcherApps.startMainActivity(
                ComponentName(packageName, ""),
                handle, null, null
            )
            true
        } catch (e: Exception) {
            // Fallback: arahkan user buka Play Store Work Profile secara manual
            try {
                val handle = workProfileHandle() ?: return false
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // LauncherApps.getActivityList untuk cari Play Store dalam work profile
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val activities = launcherApps.getActivityList("com.android.vending", handle)
                if (activities.isNotEmpty()) {
                    launcherApps.startMainActivity(activities[0].componentName, handle, null, null)
                    true
                } else false
            } catch (e2: Exception) { false }
        }
    }

    fun launchClonedApp(packageName: String): Boolean {
        return try {
            val handle = workProfileHandle() ?: return false
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val activities = launcherApps.getActivityList(packageName, handle)
            if (activities.isEmpty()) return false
            launcherApps.startMainActivity(activities[0].componentName, handle, null, null)
            true
        } catch (e: Exception) { false }
    }

    fun installApkFile(file: File, context: Context) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
