package com.vinsol.androidcurl;

public class cpuArchHelper {
	
	/* this is used to load the 'CPU_ARCH' library on application
     * startup. 
     */
    static {
        System.loadLibrary("CPU_ARCH");
    }
    
	/* A native method that is implemented by the
     * 'cpuArch' native library, which is packaged
     * with this application.
     */
    public native String cpuArchFromJNI();

    public boolean isARM_CPU(String cpuInfoString) {
        return cpuInfoString.contains("ARM");
    }

    public boolean isARM_v7_CPU(String cpuInfoString) {
        return cpuInfoString.contains("v7");
	}
}