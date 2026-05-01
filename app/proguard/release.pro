-dontobfuscate

-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int i(...);
    public static int e(...);
}

-assumenosideeffects class co.adityarajput.fileflow.utils.Logger {
    public final void d(...);
}

-dontwarn javax.annotation.processing.Processor
-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedOptions

#-printusage usage.txt
