# Start React dev server — open http://localhost:8081 in your browser
Set-Location $PSScriptRoot
if (-not (Test-Path node_modules)) {
  npm install
}
npm run dev
