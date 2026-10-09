# Installs Lombok into Eclipse/STS so builder() / getXxx() are not "undefined".
#
# Usage:
#   .\install-lombok-eclipse.ps1                       # GUI installer (picks Java automatically)
#   .\install-lombok-eclipse.ps1 "D:\path\to\eclipse"  # headless install, no GUI (IDE must be closed)
#
# Why: Maven/javac runs Lombok as an annotation processor (builds always work),
# but the Eclipse compiler (JDT/ECJ) needs Lombok attached to the IDE itself,
# otherwise every CALL SITE of generated code shows red errors.

param(
    [string]$EclipsePath
)

$lombokJar = Join-Path $env:USERPROFILE ".m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar"
if (-not (Test-Path $lombokJar)) {
    Write-Host "Downloading Lombok via Maven..."
    Set-Location $PSScriptRoot
    .\mvnw.cmd dependency:get "-Dartifact=org.projectlombok:lombok:1.18.46" -q
}
if (-not (Test-Path $lombokJar)) {
    Write-Error "Lombok jar not found at $lombokJar"
    exit 1
}

# 'java' is frequently NOT on PATH (this machine included), so fall back to the
# JRE bundled with Spring Tools / Eclipse before giving up.
$javaExe = (Get-Command java -ErrorAction SilentlyContinue).Source
if (-not $javaExe) {
    $searchRoots = @("D:\Development Tools", "C:\Program Files", "D:\") | Where-Object { Test-Path $_ }
    foreach ($root in $searchRoots) {
        $jreDir = Get-ChildItem $root -Directory -Recurse -Depth 4 -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -like "org.eclipse.justj.openjdk.hotspot.jre.full*" } |
            Select-Object -First 1
        if ($jreDir) {
            $candidate = Join-Path $jreDir.FullName "jre\bin\java.exe"
            if (Test-Path $candidate) { $javaExe = $candidate; break }
        }
    }
}
if (-not $javaExe -or -not (Test-Path $javaExe)) {
    Write-Error "No java.exe on PATH and no bundled JRE found. Install a JDK, then re-run."
    exit 1
}
Write-Host "Using Java: $javaExe"

# Headless mode: no GUI, exits when done (Eclipse/STS must be CLOSED first).
if ($EclipsePath) {
    & $javaExe -jar $lombokJar install $EclipsePath
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "Done. Start your IDE again - errors should be gone."
    exit 0
}

# GUI mode: installer window, user picks the IDE folder.
Write-Host "Launching the graphical installer."
Write-Host "Select your Eclipse/STS folder in the window, then restart the IDE."
& $javaExe -jar $lombokJar
