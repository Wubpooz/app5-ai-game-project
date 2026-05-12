param([string]$Tag)

if (-not $Tag) {
    Write-Host "Usage: .\release.ps1 vX.Y.Z"
    exit 1
}

git tag -a $Tag -m "Release $Tag"
git push origin $Tag

Write-Host "Pushed tag $Tag. GitHub Actions will build and publish the release."
