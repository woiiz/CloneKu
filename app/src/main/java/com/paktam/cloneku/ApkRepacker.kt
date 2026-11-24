package com.paktam.cloneku

import android.content.Context
import android.content.pm.PackageManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Repack APK — fallback bila Work Profile tak boleh.
 * Langkah sebenar yang lengkap perlukan apktool / binary AndroidManifest editor + apksigner.
 * Fail ni sediakan pipeline yang betul:
 *  1. Copy APK asal dari /data/app
 *  2. Salin ke folder CloneKu/clones/
 *  3. Tanda package baru: <asal>.clone1
 *
 * NOTA PENTING UNTUK BUILD PENUH:
 * Tukar package name dalam binary AndroidManifest.xml TIDAK boleh buat dengan Zip copy saja.
 * Untuk production, tambah library `apktool` (brut.androlib) atau guna tool `APKEditor` / `MT Manager`
 * di telefon. Versi ni copy APK asal dan sediakan fail untuk di-repack dengan APKEditor —
 * dan kalau app tu tak ada signature check, install APK copy ni sebagai langkah pertama
 * akan gagal sebab package sama — jadi user MESTI repack dulu dengan APKEditor (langkah ada dalam README).
 * Jangan tipu user: kami labelkan dengan jujur dalam UI.
 */
object ApkRepacker {

    sealed class Result {
        data class Ok(val outputFile: File, val outputPath: String) : Result()
        data class Fail(val reason: String) : Result()
    }

    fun cloneApk(context: Context, packageName: String): Result {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val src = File(appInfo.sourceDir)
            if (!src.exists()) return Result.Fail("APK asal tak dijumpai: ${appInfo.sourceDir}")

            val outDir = File(context.getExternalFilesDir(null), "clones")
            if (!outDir.exists()) outDir.mkdirs()

            val newPackage = "$packageName.clone1"
            val outFile = File(outDir, "${packageName}-clone1.apk")

            // Copy APK asal — ini langkah 1. Repack sebenar (tukar package) dibuat
            // dengan APKEditor / apktool, rujuk README bahagian "Repack manual 2 minit".
            FileInputStream(src).use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }

            // Simpan nota package baru supaya user tahu nama clone
            File(outDir, "${packageName}-clone1.txt").writeText(
                "App asal: $packageName\nPackage clone: $newPackage\n\n" +
                "CARA REPACK 2 MINIT (dalam telefon, percuma):\n" +
                "1. Install APKEditor (Play Store)\n" +
                "2. Buka APKEditor > Select APK > pilih fail ni: ${outFile.absolutePath}\n" +
                "3. Pilih 'Common Edit' > tukar Package Name ke $newPackage\n" +
                "4. Save > Install APK yang dah di-save tu.\n" +
                "Siap — app clone akan muncul sebagai app berasingan.\n"
            )

            Result.Ok(outFile, outFile.absolutePath)
        } catch (e: Exception) {
            Result.Fail(e.message ?: "Ralat tak diketahui")
        }
    }

    /** Helper zip — untuk kegunaan masa depan bila tambah binary manifest editor. */
    @Suppress("unused")
    fun copyZipWithoutSignature(src: File, dst: File) {
        ZipInputStream(FileInputStream(src)).use { zin ->
            ZipOutputStream(FileOutputStream(dst)).use { zout ->
                var entry: ZipEntry? = zin.nextEntry
                while (entry != null) {
                    if (entry.name.startsWith("META-INF/")) { entry = zin.nextEntry; continue }
                    zout.putNextEntry(ZipEntry(entry.name))
                    zin.copyTo(zout)
                    zout.closeEntry()
                    entry = zin.nextEntry
                }
            }
        }
    }
}
