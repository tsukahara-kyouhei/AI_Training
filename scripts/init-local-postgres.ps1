param(
    [string]$Host = "localhost",
    [int]$Port = 5432,
    [string]$Database = "office_order",
    [string]$Username = "office_order",
    [string]$Password = "office_order",
    [switch]$SkipSeed
)

$ErrorActionPreference = "Stop"

function Invoke-PsqlFile {
    param(
        [string]$SqlFilePath
    )

    if (-not (Test-Path $SqlFilePath)) {
        throw "SQLファイルが見つかりません: $SqlFilePath"
    }

    & psql `
        -v ON_ERROR_STOP=1 `
        -h $Host `
        -p $Port `
        -U $Username `
        -d $Database `
        -f $SqlFilePath

    if ($LASTEXITCODE -ne 0) {
        throw "psql実行に失敗しました: $SqlFilePath"
    }
}

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw "psql コマンドが見つかりません。PostgreSQLクライアントをインストールし、PATHを設定してください。"
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$schemaDir = Join-Path $repoRoot "sql/schema"
$seedDir = Join-Path $repoRoot "sql/seed"

$schemaFiles = @(
    "masters.sql",
    "products.sql",
    "members.sql",
    "orders.sql",
    "spring-batch-metadata.sql",
    "content.sql",
    "batch.sql",
    "search-functions.sql"
)

$seedFiles = @(
    "masters.sql",
    "products.sql",
    "members.sql",
    "orders.sql",
    "content.sql"
)

$env:PGPASSWORD = $Password
try {
    foreach ($file in $schemaFiles) {
        Invoke-PsqlFile -SqlFilePath (Join-Path $schemaDir $file)
    }

    if (-not $SkipSeed) {
        foreach ($file in $seedFiles) {
            Invoke-PsqlFile -SqlFilePath (Join-Path $seedDir $file)
        }
    }

    Write-Host "ローカルPostgreSQLへの初期化が完了しました。"
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
