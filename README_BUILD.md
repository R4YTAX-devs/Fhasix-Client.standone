# FHASIX Client 1.21.11 (Fabric, Yarn) - full source, version 5.4

- `src/`    the client mod (Java 21, Fabric Loom, Fabric API 0.141.5+1.21.11)
- `loader/` the FHASIX loader (checks GitHub for updates and installs them)
- `release.sh` builds both jars and publishes them to the GitHub repo (your own repo + token needed)
- `termux-setup.sh` one-time Termux setup (Java 21 + a working Java certificate store); release.sh runs it by itself

Build the jars on any computer with JDK 21:
    ./gradlew build                 -> build/libs/fhasix-client-*.jar
    ./gradlew -p loader build       -> loader/build/libs/fhasix-loader-*.jar
A `-sources.jar` next to each jar is produced automatically.

5.4: Title-screen side banners (Fhasix Points check-in while unclaimed + news), Extra Boost (always on),
and the hidden FPS modes that never helped (Lazy Chunk Loading, Effects, HUD, Background, Menu, Fog Optimizer).
