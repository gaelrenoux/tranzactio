# Quick How-to for release
```bash
git tag vX.Y.Z
git push origin vX.Y.Z
sbt
+clean
+compile
+publishSigned
project core
sonatypeBundleRelease
```
Wait for the deployment to happen.
Check on https://central.sonatype.com/publishing/deployments
