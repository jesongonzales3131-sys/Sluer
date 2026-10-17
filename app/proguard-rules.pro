# Shizuku user service: the RunnerService class is instantiated by reflection
# inside a process started by the Shizuku server, and it is reached over the
# IRunnerService binder interface. Keep both for release builds.
-keep class com.sluer.app.core.RunnerService { <init>(); *; }
-keep class com.sluer.app.core.IRunnerService { *; }
