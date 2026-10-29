param()
$ErrorActionPreference = 'Stop'
$dir = 'target\surefire-reports'
$out = New-Object System.Text.StringBuilder
Get-ChildItem -Path $dir -Filter 'TEST-*.xml' | ForEach-Object {
  $text = [System.IO.File]::ReadAllText($_.FullName, [System.Text.Encoding]::UTF8)
  $xml = [xml]$text
  $suite = $xml.testsuite
  [void]$out.AppendLine("SUITE`t$($suite.name)`trun=$($suite.tests) fail=$($suite.failures) err=$($suite.errors)")
  foreach ($tc in $suite.testcase) {
    if ($tc.failure) {
      $msg = "$($tc.failure.message)"
      if ($msg.Length -gt 160) { $msg = $msg.Substring(0,160) }
      [void]$out.AppendLine("FAIL`t$($tc.name)`t$msg")
    }
    if ($tc.error) {
      $msg = "$($tc.error.message)"
      if ($msg.Length -gt 160) { $msg = $msg.Substring(0,160) }
      [void]$out.AppendLine("ERR`t$($tc.name)`t$msg")
    }
  }
}
[System.IO.File]::WriteAllText((Join-Path (Get-Location) 'fail-compact.txt'), $out.ToString(), [System.Text.Encoding]::UTF8)
Write-Output 'done'
