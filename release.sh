set -eo pipefail
V="$1"; N="${V#v}"
[ -z "$N" ] && { echo "usage: bash release.sh v6"; exit 1; }
sed -i "s/^mod_version=.*/mod_version=$N.0.0/; s/^archives_base_name=.*/archives_base_name=fhasix-client/" gradle.properties
./gradlew build --no-daemon 2>&1 | tee build-output.log
rm -rf dist && mkdir dist
cp build/libs/fhasix-client-$N.0.0.jar dist/fhasix-client-$V.jar
S=$(sha256sum dist/fhasix-client-$V.jar | cut -d' ' -f1)
Z=$(stat -c%s dist/fhasix-client-$V.jar)
printf '{\n  "version": "%s",\n  "jar": "dist/fhasix-client-%s.jar",\n  "sha256": "%s",\n  "size": %s\n}\n' "$V" "$V" "$S" "$Z" > version.json
git add -A && git commit -m "Release $V" && git push
