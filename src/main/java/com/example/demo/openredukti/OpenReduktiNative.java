package com.example.demo.openredukti;

/**
 * JNI declarations for the native OpenRedukti bridge library.
 *
 * The native library (libopenredukti_jni.so / openredukti_jni.dll) must be
 * present on java.library.path at runtime.  The Dockerfile copies it to
 * /usr/local/lib and the JVM is started with -Djava.library.path=/usr/local/lib.
 *
 * All methods exchange data as UTF-8 JSON strings to avoid a protobuf
 * runtime dependency on the Java side.
 */
public final class OpenReduktiNative {

    private OpenReduktiNative() {}

    private static final boolean NATIVE_AVAILABLE;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("openredukti_jni");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[OpenRedukti] Native library not found – running in stub mode. " + e.getMessage());
        }
        NATIVE_AVAILABLE = loaded;
    }

    /** Returns true when libopenredukti_jni is loaded and functional. */
    public static boolean isAvailable() { return NATIVE_AVAILABLE; }

    /**
     * Returns a JSON object with the library version information.
     * {"status":"OK","library":"OpenRedukti","version":"...","bridge":"JNI 1.0"}
     */
    public static native String getVersion();

    /**
     * Bootstrap zero curves from par rates.
     *
     * @param jsonRequest JSON string – see openredukti_jni.cpp for schema
     * @return JSON string with bootstrapped discount factors, or an error object
     */
    public static native String bootstrapCurves(String jsonRequest);

    /**
     * Compute the present value (and optional sensitivities) of a set of cashflows.
     *
     * @param jsonRequest JSON string – see openredukti_jni.cpp for schema
     * @return JSON string with PV and sensitivities, or an error object
     */
    public static native String valueCashflows(String jsonRequest);
}
