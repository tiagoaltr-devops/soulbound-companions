$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classes = Join-Path $projectRoot 'research/external-mods/work/test-classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
& javac -encoding UTF-8 -d $classes (Join-Path $PSScriptRoot 'ModAnalyzer.java') (Join-Path $PSScriptRoot 'ModAnalyzerTest.java')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $classes ModAnalyzerTest
exit $LASTEXITCODE
