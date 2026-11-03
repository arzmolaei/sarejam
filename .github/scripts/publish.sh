#!/usr/bin/env bash
# Usage: publish.sh <tag> <apk> <commit>. Refuses any APK not signed with the pinned release certificate.
set -euo pipefail
tag="$1"; apk="$2"; commit="$3"
signer="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)/apksigner"
"$signer" verify --print-certs "$apk" | tee certs.txt
actual="$(grep -m1 'certificate SHA-256 digest' certs.txt | awk '{print $NF}')"
expected="$(tr -d '[:space:]' < docs/signing-cert.sha256)"
if [ "$actual" != "$expected" ]; then
  echo "::error::APK certificate $actual does not match pinned $expected"; exit 1
fi
sha256sum "$apk" > "$apk.sha256"
notes="docs/releases/${tag}.md"
[ -f "$notes" ] || { echo "نسخه ${tag#v}" > /tmp/notes.md; notes=/tmp/notes.md; }
gh release create "$tag" "$apk" "$apk.sha256" --target "$commit" --title "سرجمع ${tag#v}" --notes-file "$notes"
