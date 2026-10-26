param([string]$BaseUrl = 'http://localhost:8095')
$ErrorActionPreference = 'Stop'
$pair = 'student:practice-only'
$merchantHeaders = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair)) }
$callbackHeaders = @{ 'X-Callback-Token' = 'local-callback-token' }
$reference = 'ORDER-' + [guid]::NewGuid().ToString('N')
$request = @{ reference = $reference; phone = '251700000001'; amount = 250.00; currency = 'ETB' } | ConvertTo-Json

Write-Host '1. Create a payment (202, PENDING).'
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/payments" -Headers $merchantHeaders -ContentType 'application/json' -Body $request | Format-List
Write-Host '2. Replay the same request (same payment, no second initiation).'
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/payments" -Headers $merchantHeaders -ContentType 'application/json' -Body $request | Format-List
Write-Host '3. Simulate the provider callback (SUCCESS).'
$callback = @{ reference = $reference; status = 'SUCCESS' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/payments/callback" -Headers $callbackHeaders -ContentType 'application/json' -Body $callback | Format-List
Write-Host '4. Retrieve the completed payment.'
Invoke-RestMethod -Uri "$BaseUrl/api/payments/$reference" -Headers $merchantHeaders | Format-List
Write-Host '5. Create a timeout scenario (UNKNOWN after about two seconds).'
$request = @{ reference = "SLOW-$reference"; phone = '251700000001'; amount = 10; currency = 'ETB' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/payments" -Headers $merchantHeaders -ContentType 'application/json' -Body $request | Format-List
