$ErrorActionPreference='Stop'
$rcon=Join-Path $PSScriptRoot 'Invoke-SoulboundTestRcon.ps1'
$candidates=@()
foreach($x in -1..1){foreach($z in -1..1){
    $reply=& $rcon -Commands @("soulbound altar candidate $x $z")
    if($reply.Result -match 'x=(-?\d+), z=(-?\d+)'){$candidates+=,@([int]$Matches[1],[int]$Matches[2])}
}}
$results=@()
foreach($candidate in $candidates){
    $x=$candidate[0];$z=$candidate[1]
    & $rcon -Commands @("forceload add $x $z") | Out-Null
    try {
        $queries=@();foreach($y in 48..160){$queries+="execute if block $x $y $z soulbound:ancestral_pedestal"}
        $results+=& $rcon -Commands $queries | Where-Object {$_.Result -match 'passed'}
    }finally{& $rcon -Commands @("forceload remove $x $z") | Out-Null}
}
if($results.Count -eq 0){throw 'No natural pedestal found in this bounded nine-candidate sample; this does not prove generation is broken.'}
$reportDirectory=Join-Path $PSScriptRoot '../build/phase6-validation'
[IO.Directory]::CreateDirectory($reportDirectory) | Out-Null
$results | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $reportDirectory 'worldgen-test.json')
$results
