package com.mimavault.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import com.mimavault.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 检查更新：读取本仓库最新发布，版本号更高时提示下载安装包。
 * 直连失败会再试一次国内镜像；下载交给浏览器，安装由用户确认。
 */
public final class UpdateChecker {

    private static final String API =
            "https://api.github.com/repos/startime-ltk/-MimaVault-app/releases/latest";
    private static final String MIRROR = "https://ghfast.top/";

    private static final int FAILED = 0;
    private static final int LATEST = 1;
    private static final int NEW_VERSION = 2;

    private UpdateChecker() {
    }

    /** silent 为 true 时只在发现新版本时弹窗，用于启动时的静默检查。 */
    public static void check(Activity activity, boolean silent) {
        new Thread(() -> {
            String json = fetch(API);
            if (json == null) {
                json = fetch(MIRROR + API);
            }
            if (json == null) {
                show(activity, silent, FAILED, null, null, null);
                return;
            }
            try {
                JSONObject release = new JSONObject(json);
                String tag = release.optString("tag_name", "");
                String notes = release.optString("body", "");
                String apkUrl = findApk(release.optJSONArray("assets"));
                String current = currentVersion(activity);
                if (apkUrl == null || compare(tag, current) <= 0) {
                    show(activity, silent, LATEST, null, null, null);
                } else {
                    show(activity, silent, NEW_VERSION, tag, notes, apkUrl);
                }
            } catch (Exception e) {
                show(activity, silent, FAILED, null, null, null);
            }
        }).start();
    }

    private static String findApk(JSONArray assets) throws Exception {
        if (assets == null) {
            return null;
        }
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if (asset.optString("name", "").toLowerCase().endsWith(".apk")) {
                String url = asset.optString("browser_download_url", "");
                return url.isEmpty() ? null : url;
            }
        }
        return null;
    }

    private static void show(Activity a, boolean silent, int status,
                             String tag, String notes, String apkUrl) {
        a.runOnUiThread(() -> {
            if (a.isFinishing() || a.isDestroyed()) {
                return;
            }
            if (status == FAILED) {
                if (!silent) {
                    Toast.makeText(a, R.string.update_failed, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if (status == LATEST) {
                if (!silent) {
                    Toast.makeText(a, R.string.update_latest, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            String summary = notes == null ? "" : notes.trim();
            if (summary.length() > 160) {
                summary = summary.substring(0, 160) + "…";
            }
            String message = "当前版本 " + currentVersion(a)
                    + (summary.isEmpty() ? "" : "\n\n" + summary);
            new AlertDialog.Builder(a)
                    .setTitle("发现新版本 " + tag)
                    .setMessage(message)
                    .setPositiveButton("下载安装", (d, w) -> {
                        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                        a.startActivity(Intent.createChooser(view, "下载安装包"));
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private static String currentVersion(Activity a) {
        try {
            return a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    /** 把 vA8.31.3 之类的版本号转成数字逐段比较，兼容前缀字母。 */
    private static int compare(String remote, String local) {
        int[] a = numbers(remote);
        int[] b = numbers(local);
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return x > y ? 1 : -1;
            }
        }
        return 0;
    }

    private static int[] numbers(String s) {
        String[] parts = s.replaceAll("[^0-9.]", "").split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    private static String fetch(String address) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(address).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "MimaVault-Android");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            if (conn.getResponseCode() != 200) {
                return null;
            }
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
