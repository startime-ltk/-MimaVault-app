package com.mimavault.model;

import java.util.Date;

/**
 * 密码条目实体，与 PC 端 Entry 1:1 对齐
 * 对应数据库 entries 表
 */
public class Entry {

    public static final String CATEGORY_WEBSITE = "网站";
    public static final String CATEGORY_APP = "应用";
    public static final String CATEGORY_OTHER = "其他";
    public static final String[] CATEGORIES = {CATEGORY_WEBSITE, CATEGORY_APP, CATEGORY_OTHER};

    private long id;
    private String category;
    private String platform;
    private String account;
    private String passwordEnc;
    private String phone;
    private String email;
    private String note;
    private String imagePath;
    private String gestureSeq;
    private String syncStatus;
    private Date createdAt;
    private Date updatedAt;

    public Entry() {
        this.category = CATEGORY_WEBSITE;
        this.syncStatus = "local";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getPasswordEnc() { return passwordEnc; }
    public void setPasswordEnc(String passwordEnc) { this.passwordEnc = passwordEnc; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getGestureSeq() { return gestureSeq; }
    public void setGestureSeq(String gestureSeq) { this.gestureSeq = gestureSeq; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    /** 用于合并导入去重的键：platform|account（与 PC 端 keyOf 一致） */
    public String backupKey() {
        return (platform == null ? "" : platform) + "|" + (account == null ? "" : account);
    }
}
