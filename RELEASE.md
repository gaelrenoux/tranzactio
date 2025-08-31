# Quick How-to for release
```bash
git tag vX.Y.Z
sbt
+clean
+compile
+publishSigned
project core
sonatypeBundleRelease
```
