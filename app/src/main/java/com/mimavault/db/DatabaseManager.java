package com.mimavault.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mimavault.config.AppConfig;
import com.mimavault.model.Entry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * SQLite 数据库管理，与 PC 端 DatabaseManager 逐字段对齐
 * 表结构与 PC 端密匣 MimaVault 数据库完全一致
 */
public class DatabaseManager {

    private SQLiteDatabase db;

    private static final String SQL_SETTINGS =
            "CREATE TABLE IF NOT EXISTS settings (" +
                    "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                    "master_hash TEXT NOT NULL," +
                    "created_at INTEGER)";

    private static final String SQL_ENTRIES =
            "CREATE TABLE IF NOT EXISTS entries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "category TEXT DEFAULT '网站'," +
                    "platform TEXT," +
                    "account TEXT," +
                    "password_enc TEXT," +
                    "phone TEXT," +
                    "email TEXT," +
                    "note TEXT," +
                    "image_path TEXT," +
                    "gesture_seq TEXT," +
                    "sync_status TEXT DEFAULT 'local'," +
                    "created_at INTEGER," +
                    "updated_at INTEGER)";

    /** 初始化：建表 + 兼容补列 */
    public void init() {
        db = SQLiteDatabase.openOrCreateDatabase(AppConfig.dbFile(), null);
        db.execSQL(SQL_SETTINGS);
        db.execSQL(SQL_ENTRIES);
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_entries_platform ON entries(platform)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_entries_category ON entries(category)");
        ensureColumn("entries", "category", "TEXT DEFAULT '网站'");
        ensureColumn("entries", "sync_status", "TEXT DEFAULT 'local'");
    }

    private void ensureColumn(String table, String column, String definition) {
        boolean found = false;
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (c.moveToNext()) {
                if (column.equalsIgnoreCase(c.getString(c.getColumnIndexOrThrow("name")))) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    public boolean isMasterSet() {
        try (Cursor c = db.rawQuery("SELECT master_hash FROM settings WHERE id = 1", null)) {
            return c.moveToFirst();
        }
    }

    public void saveMasterHash(String hash) {
        ContentValues cv = new ContentValues();
        cv.put("id", 1);
        cv.put("master_hash", hash);
        cv.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getMasterHash() {
        try (Cursor c = db.rawQuery("SELECT master_hash FROM settings WHERE id = 1", null)) {
            if (c.moveToFirst()) {
                return c.getString(0);
            }
        }
        return null;
    }

    public long insertEntry(Entry e) {
        ContentValues cv = toValues(e);
        long now = System.currentTimeMillis();
        cv.put("created_at", now);
        cv.put("updated_at", now);
        return db.insert("entries", null, cv);
    }

    public void updateEntry(Entry e) {
        ContentValues cv = toValues(e);
        cv.put("updated_at", System.currentTimeMillis());
        db.update("entries", cv, "id=?", new String[]{String.valueOf(e.getId())});
    }

    public void deleteEntry(long id) {
        db.delete("entries", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Entry> getAllEntries() {
        return query("SELECT * FROM entries ORDER BY updated_at DESC", null);
    }

    public List<Entry> searchEntries(String keyword, String category) {
        StringBuilder sql = new StringBuilder("SELECT * FROM entries WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (category != null && !category.trim().isEmpty() && !"全部".equals(category.trim())) {
            sql.append(" AND category=?");
            args.add(category.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String like = "%" + keyword.trim() + "%";
            sql.append(" AND (platform LIKE ? OR account LIKE ? OR phone LIKE ? OR email LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY updated_at DESC");
        return query(sql.toString(), args.toArray(new String[0]));
    }

    public Entry getEntryById(long id) {
        List<Entry> list = query("SELECT * FROM entries WHERE id=?", new String[]{String.valueOf(id)});
        return list.isEmpty() ? null : list.get(0);
    }

    public void clearEntries() {
        db.delete("entries", null, null);
    }

    /** 批量插入（导入恢复使用，事务） */
    public void insertAll(List<Entry> entries) {
        db.beginTransaction();
        try {
            for (Entry e : entries) {
                ContentValues cv = toValues(e);
                if (e.getCreatedAt() != null) {
                    cv.put("created_at", e.getCreatedAt().getTime());
                } else {
                    cv.put("created_at", System.currentTimeMillis());
                }
                cv.put("updated_at", e.getUpdatedAt() == null ? System.currentTimeMillis() : e.getUpdatedAt().getTime());
                db.insert("entries", null, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues toValues(Entry e) {
        ContentValues cv = new ContentValues();
        cv.put("category", e.getCategory() == null ? Entry.CATEGORY_WEBSITE : e.getCategory());
        cv.put("platform", e.getPlatform());
        cv.put("account", e.getAccount());
        cv.put("password_enc", e.getPasswordEnc());
        cv.put("phone", e.getPhone());
        cv.put("email", e.getEmail());
        cv.put("note", e.getNote());
        cv.put("image_path", e.getImagePath());
        cv.put("gesture_seq", e.getGestureSeq());
        cv.put("sync_status", e.getSyncStatus() == null ? "local" : e.getSyncStatus());
        return cv;
    }

    private List<Entry> query(String sql, String[] args) {
        List<Entry> list = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) {
                Entry e = new Entry();
                e.setId(c.getLong(c.getColumnIndexOrThrow("id")));
                e.setCategory(c.getString(c.getColumnIndexOrThrow("category")));
                e.setPlatform(c.getString(c.getColumnIndexOrThrow("platform")));
                e.setAccount(c.getString(c.getColumnIndexOrThrow("account")));
                e.setPasswordEnc(c.getString(c.getColumnIndexOrThrow("password_enc")));
                e.setPhone(c.getString(c.getColumnIndexOrThrow("phone")));
                e.setEmail(c.getString(c.getColumnIndexOrThrow("email")));
                e.setNote(c.getString(c.getColumnIndexOrThrow("note")));
                e.setImagePath(c.getString(c.getColumnIndexOrThrow("image_path")));
                e.setGestureSeq(c.getString(c.getColumnIndexOrThrow("gesture_seq")));
                e.setSyncStatus(c.getString(c.getColumnIndexOrThrow("sync_status")));
                long created = c.getLong(c.getColumnIndexOrThrow("created_at"));
                long updated = c.getLong(c.getColumnIndexOrThrow("updated_at"));
                if (created > 0) e.setCreatedAt(new Date(created));
                if (updated > 0) e.setUpdatedAt(new Date(updated));
                list.add(e);
            }
        }
        return list;
    }
}
