# Always run backend via Maven (handles Lombok + builds correctly)
# API listens on http://localhost:8082 — frontend on http://localhost:8081 proxies /api here
Set-Location $PSScriptRoot
.\mvnw.cmd spring-boot:run