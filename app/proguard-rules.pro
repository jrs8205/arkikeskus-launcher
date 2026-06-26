# Project-specific R8/ProGuard rules for the release build.
#
# The libraries we use (Hilt, Room, Jetpack Compose, Coil, DataStore, Kotlin coroutines) ship their
# own "consumer" R8 rules, and manifest-declared components (Activity, the notification-listener
# service) are kept automatically. So no app-specific keeps are needed yet. Add targeted -keep rules
# below if R8 ever strips or renames something used via reflection.
