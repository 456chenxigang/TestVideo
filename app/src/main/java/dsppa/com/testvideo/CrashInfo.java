package dsppa.com.testvideo;

import cn.bmob.v3.BmobObject;

/**
 * Created by cxg on 2017/9/5.
 */

public class CrashInfo extends BmobObject{

    private String exceptionInfo;

    private String packageInfo;

    private String sysVersion;

    public CrashInfo() {

    }

    public String getExceptionInfo() {
        return exceptionInfo;
    }

    public void setExceptionInfo(String exceptionInfo) {
        this.exceptionInfo = exceptionInfo;
    }

    public String getPackageInfo() {
        return packageInfo;
    }

    public void setPackageInfo(String packageInfo) {
        this.packageInfo = packageInfo;
    }

    public String getSysVersion() {
        return sysVersion;
    }

    public void setSysVersion(String sysVersion) {
        this.sysVersion = sysVersion;
    }
}
