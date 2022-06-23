package io.tenseventyseven.fresh.ota;

import android.annotation.SuppressLint;
import android.text.format.DateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.tenseventyseven.fresh.utils.Tools;

public class SoftwareUpdate {
    private long dateTime;
    private long versionCode;
    private String versionName;
    private String spl;
    private String md5Hash;
    private String releaseType;
    private String fileUrl;
    private long fileSize;
    private String changelog;
    private String updatedApps;
    private int response = 0;

    public int getResponse() {
        return response;
    }

    public void setResponse(int response) {
        this.response = response;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public long getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(long versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getSpl() {
        return spl;
    }

    public void setSpl(String spl) {
        this.spl = spl;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public String getReleaseType() {
        return releaseType;
    }

    public void setReleaseType(String releaseType) {
        this.releaseType = releaseType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    public String getUpdatedApps() {
        return updatedApps;
    }

    public void setUpdatedApps(String updatedApps) {
        this.updatedApps = updatedApps;
    }

    public String getFullVersion() {
        return String.format("%s/%s/%s", versionCode, dateTime, releaseType);
    }

    public String getFormattedVersion() {
        return String.format("%s %s", versionName, Tools.capitalizeString(releaseType));
    }

    @SuppressLint("SimpleDateFormat")
    public String getSplString() {
        if (!"".equals(spl)) {
            try {
                SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd");
                Date patchDate = template.parse(spl);
                String format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy");
                spl = DateFormat.format(format, patchDate).toString();
            } catch (ParseException e) {
                // broken parse; fall through and use the raw string
            }
            return spl;
        } else {
            return null;
        }
    }

    public String getFileSizeFormat() {
        return UpdateUtils.getFormattedFileSize(fileSize);
    }
}
