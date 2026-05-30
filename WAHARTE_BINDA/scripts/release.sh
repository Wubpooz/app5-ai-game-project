#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 vX.Y.Z"
  exit 1
fi

TAG="$1"

git tag -a "$TAG" -m "Release $TAG"
git push origin "$TAG"

echo "Pushed tag $TAG. GitHub Actions will build and publish the release." 
