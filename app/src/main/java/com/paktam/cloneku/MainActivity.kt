package com.paktam.cloneku

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var cloneManager: CloneManager
    private lateinit var listView: ListView
    private lateinit var statusText: TextView
    private lateinit var btnSetupProfile: Button
    private lateinit var searchBox: EditText
    private var allApps: List<AppInfo> = emptyList()
    private var adapter: ArrayAdapter<String>? = null

    companion object { const val REQ_CREATE_PROFILE = 1001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cloneManager = CloneManager(this)
        listView = findViewById(R.id.listView)
        statusText = findViewById(R.id.statusText)
        btnSetupProfile = findViewById(R.id.btnSetupProfile)
        searchBox = findViewById(R.id.searchBox)

        btnSetupProfile.setOnClickListener { setupProfile() }
        findViewById<Button>(R.id.btnRefresh).setOnClickListener { loadApps() }

        searchBox.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { filter(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        listView.setOnItemClickListener { _, _, pos, _ ->
            val shown = adapter?.getItem(pos) ?: return@setOnItemClickListener
            val app = allApps.firstOrNull { shown.startsWith(it.label) } ?: return@setOnItemClickListener
            showCloneDialog(app)
        }

        updateStatus()
        loadApps()
    }

    private fun updateStatus() {
        val hasProfile = cloneManager.hasWorkProfile()
        statusText.text = if (hasProfile) {
            "✅ Work Profile AKTIF — kau boleh clone 1 copy setiap app.\nClone akan muncul dengan badge 💼"
        } else {
            "⚠️ Work Profile belum aktif. Tekan 'Aktifkan Work Profile' dulu untuk clone cara stabil. Kalau tak nak, masih boleh guna Repack APK."
        }
        btnSetupProfile.text = if (hasProfile) "Work Profile Aktif ✓" else "Aktifkan Work Profile"
        btnSetupProfile.isEnabled = !hasProfile
    }

    private fun setupProfile() {
        val intent = cloneManager.createManagedProfileIntent()
        if (intent != null) {
            startActivityForResult(intent, REQ_CREATE_PROFILE)
        } else {
            Toast.makeText(this, "Telefon ni tak support Work Profile / dah ada profile", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CREATE_PROFILE) {
            if (resultCode == Activity.RESULT_OK) Toast.makeText(this, "Work Profile berjaya dibuat!", Toast.LENGTH_LONG).show()
            updateStatus()
        }
    }

    private fun loadApps() {
        val pm = packageManager
        val pkgs = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }
        allApps = pkgs.map {
            AppInfo(pm.getApplicationLabel(it).toString(), it.packageName, it)
        }
        filter(searchBox.text.toString())
    }

    private fun filter(q: String) {
        val filtered = if (q.isBlank()) allApps else allApps.filter {
            it.label.contains(q, true) || it.packageName.contains(q, true)
        }
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
            filtered.map { "${it.label}\n${it.packageName}" })
        listView.adapter = adapter
        // simpan mapping untuk klik — guna label
        findViewById<TextView>(R.id.countText).text = "${filtered.size} app dijumpai"
    }

    private fun showCloneDialog(app: AppInfo) {
        val options = arrayOf(
            "Clone via Work Profile (disyorkan — function 100% sama)",
            "Clone via Repack APK (package baru .clone1 — untuk copy ke-2, ke-3)"
        )
        AlertDialog.Builder(this)
            .setTitle("Clone ${app.label}?")
            .setItems(options) { _, which ->
                if (which == 0) cloneViaProfile(app) else cloneViaRepack(app)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun cloneViaProfile(app: AppInfo) {
        if (!cloneManager.hasWorkProfile()) {
            Toast.makeText(this, "Aktifkan Work Profile dulu", Toast.LENGTH_LONG).show()
            setupProfile(); return
        }
        val ok = cloneManager.installIntoWorkProfile(app.packageName, this)
        Toast.makeText(this, if (ok) "Diarahkan install ${app.label} dalam Work Profile. Buka Play Store / CloneKu dalam Work Profile untuk siapkan." else "Gagal — cuba Repack APK", Toast.LENGTH_LONG).show()
    }

    private fun cloneViaRepack(app: AppInfo) {
        Toast.makeText(this, "Menyalin APK ${app.label}... tunggu", Toast.LENGTH_SHORT).show()
        Thread {
            val result = ApkRepacker.cloneApk(this, app.packageName)
            runOnUiThread {
                when (result) {
                    is ApkRepacker.Result.Ok -> {
                        Toast.makeText(this, "APK clone siap: ${result.outputPath}\nTekan Install untuk pasang.", Toast.LENGTH_LONG).show()
                        cloneManager.installApkFile(result.outputFile, this)
                    }
                    is ApkRepacker.Result.Fail -> {
                        AlertDialog.Builder(this)
                            .setTitle("Repack gagal")
                            .setMessage(result.reason + "\n\nApp ni mungkin ada signature check. Guna Work Profile.")
                            .setPositiveButton("OK", null).show()
                    }
                }
            }
        }.start()
    }
}

data class AppInfo(val label: String, val packageName: String, val info: ApplicationInfo)
