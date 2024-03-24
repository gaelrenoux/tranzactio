# Quick How-to for release
```bash
git tag vX.Y.Z
sbt
+clean
+compile
+publishSigned
# Check staging repositories on https://oss.sonatype.org/#stagingRepositories
project core
sonatypeRelease
# Check definitive repositories on https://oss.sonatype.org/#view-repositories;public~browsestorage~io.github
```
