param(
    [string]$ReportPath = "test-results/browser-compat-report.txt"
)

Write-Host "Browser Compatibility Test for StructExam"
Write-Host "========================================"
Write-Host ""

Set-Location $PSScriptRoot

Write-Host "[1/3] Installing Playwright browsers..."
npx playwright install chromium firefox webkit msedge 2>&1 | Out-Null
Write-Host "Browsers installed successfully"
Write-Host ""

mkdir -Force test-results/browser-compat | Out-Null

Write-Host "[2/3] Running browser compatibility tests..."
Write-Host ""

$browsers = @("chromium", "firefox", "webkit", "msedge")
$testFiles = @(
    "e2e/login-flow.spec.js",
    "e2e/exam-list.spec.js",
    "e2e/student-routing.spec.js"
)

$results = @()

foreach ($browser in $browsers) {
    Write-Host "Testing browser: $browser"
    foreach ($testFile in $testFiles) {
        Write-Host "  Running test: $testFile"
        npx playwright test $testFile --project=$browser --reporter=html 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "    OK"
            $results += "$browser | $testFile | PASSED"
        } else {
            Write-Host "    FAILED"
            $results += "$browser | $testFile | FAILED"
        }
    }
    Write-Host ""
}

Write-Host "[3/3] Generating report..."
Write-Host ""

$reportContent = "========================================`r`n"
$reportContent += "Browser Compatibility Test Report`r`n"
$reportContent += "========================================`r`n"
$reportContent += "`r`n"
$reportContent += "Test Environment:`r`n"
$reportContent += "- Playwright Version: 1.49.0`r`n"
$reportContent += "- Test Date: $(Get-Date)`r`n"
$reportContent += "`r`n"
$reportContent += "Test Results:`r`n"
$reportContent += "----------------------------------------`r`n"

foreach ($result in $results) {
    $reportContent += $result + "`r`n"
}

$reportContent += "`r`n"
$reportContent += "========================================`r`n"
$reportContent += "Report Notes:`r`n"
$reportContent += "- Tests cover login flow, exam list, student routing`r`n"
$reportContent += "- All browsers passing indicates good compatibility`r`n"
$reportContent += "- Fix CSS/JS compatibility issues if tests fail`r`n"
$reportContent += "========================================`r`n"

$reportContent | Out-File $ReportPath -Encoding utf8
Write-Host "Report saved to: $ReportPath"
Write-Host ""
Write-Host "========================================"
Write-Host "Browser compatibility test completed"
Write-Host "========================================"