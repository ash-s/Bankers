# Installs Lombok into Eclipse/STS so builder() / getXxx() are not "undefined"
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
Write-Host "Launching Lombok installer. Select your Eclipse/STS folder, then restart the IDE."
java -jar $lombokJar
