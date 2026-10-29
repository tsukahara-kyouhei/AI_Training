$report = 'failure-details.txt'
Remove-Item $report -ErrorAction SilentlyContinue
Get-ChildItem 'target\surefire-reports' -Filter 'TEST-*.xml' | ForEach-Object {
    $doc = New-Object System.Xml.XmlDocument
    $doc.Load($_.FullName)
    $suite = $doc.testsuite
    Add-Content -Path $report -Value ('SUITE`t{0}`trun={1} fail={2} err={3}' -f $suite.name, $suite.tests, $suite.failures, $suite.errors) -Encoding utf8
    foreach ($tc in $suite.testcase) {
        $fail = $tc.SelectSingleNode('failure')
        $err = $tc.SelectSingleNode('error')
        if ($fail) {
            Add-Content -Path $report -Value ('FAIL`t{0}#{1}`t{2}' -f $tc.classname, $tc.name, ($fail.message -replace "[\r\n]+", ' | ')) -Encoding utf8
        }
        if ($err) {
            Add-Content -Path $report -Value ('ERR`t{0}#{1}`t{2}' -f $tc.classname, $tc.name, ($err.message -replace "[\r\n]+", ' | ')) -Encoding utf8
        }
    }
}
Get-Content $report
