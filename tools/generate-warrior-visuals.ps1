# Original pixel-art UV atlases. No source images or third-party assets are read.
# Run: powershell -NoProfile -ExecutionPolicy Bypass -File tools/generate-warrior-visuals.ps1
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot=Join-Path $PSScriptRoot '../src/main/resources/assets/soulbound/textures/entity'
$script:dark=@(29,40,54);$script:gold=@(171,132,65);$script:cyan=@(56,218,231)
function Pixel($x,$y,$rgb,$shade=0){
    $r=[Math]::Max(0,[Math]::Min(255,[int]($rgb[0]+$shade)));$g=[Math]::Max(0,[Math]::Min(255,[int]($rgb[1]+$shade)));$b=[Math]::Max(0,[Math]::Min(255,[int]($rgb[2]+$shade)))
    $script:image.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,$r,$g,$b))
}
function Face($x,$y,$w,$h,$kind,$light,$front){
    $base=switch($kind){'face'{@(69,104,122)} 'shaft'{@(87,62,44)} 'edge'{@(116,197,204)} 'blade'{@(59,98,117)} 'gold'{$script:gold} 'rune'{$script:dark} default{$script:dark}}
    for($iy=0;$iy -lt $h;$iy++){for($ix=0;$ix -lt $w;$ix++){
        $shade=$light + (($ix*7+$iy*11)%3)-1
        if($ix -eq 0 -or $iy -eq 0){$shade+=12};if($ix -eq $w-1 -or $iy -eq $h-1){$shade-=14}
        Pixel ($x+$ix) ($y+$iy) $base $shade
    }}
    if(!$front){return}
    if($kind -eq 'face'){
        for($ix=1;$ix -lt $w-1;$ix++){Pixel ($x+$ix) ($y+1) $script:dark -5}
        foreach($ix in @(1,($w-2))){Pixel ($x+$ix) ($y+2) @(177,255,255);if($h -ge 7){Pixel ($x+$ix) ($y+3) $script:cyan}}
        for($ix=2;$ix -lt $w-2;$ix++){Pixel ($x+$ix) ($y+$h-2) $script:dark}
    } elseif($kind -eq 'shaft'){
        for($iy=0;$iy -lt $h;$iy++){if($iy%4 -eq 0){for($ix=0;$ix -lt $w;$ix++){Pixel ($x+$ix) ($y+$iy) $script:gold -30}}}
    } elseif($kind -in @('armor','rune','blade','shoulder','boot','helmet')){
        if($w -ge 4){for($ix=1;$ix -lt $w-1;$ix++){Pixel ($x+$ix) $y $script:gold -8}}
        if($h -ge 4 -and $w -ge 3){
            $cx=[int][Math]::Floor($w/2);$cy=[int][Math]::Floor($h/2)
            for($iy=[Math]::Max(1,$cy-1);$iy -le [Math]::Min($h-2,$cy+1);$iy++){Pixel ($x+$cx) ($y+$iy) $script:cyan}
            if($w -ge 5){Pixel ($x+$cx-1) ($y+$cy) $script:cyan;Pixel ($x+$cx+1) ($y+$cy-1) $script:cyan}
        }
        if($kind -eq 'armor' -and $w -ge 5 -and $h -ge 3){for($iy=1;$iy -lt $h-1;$iy++){Pixel ($x+1) ($y+$iy) $script:gold -35;Pixel ($x+$w-2) ($y+$iy) $script:dark -12}}
        if($kind -eq 'boot'){for($ix=0;$ix -lt $w;$ix++){Pixel ($x+$ix) ($y+$h-1) @(13,23,31)}}
    }
}
function Region($u,$v,$w,$h,$d,$kind){
    $endX=$u+2*($w+$d);$endY=$v+$h+$d
    if($endX -gt $script:image.Width -or $endY -gt $script:image.Height){throw "UV outside atlas: $u,$v"}
    # Reserve complete UV rectangles, including unused corners, to catch accidental overlaps.
    for($y=$v;$y -lt $endY;$y++){for($x=$u;$x -lt $endX;$x++){$key="$x,$y";if(!$script:used.Add($key)){throw "UV overlap at $key"}}}
    Face ($u+$d) $v $w $d $kind 12 $false
    Face ($u+$d+$w) $v $w $d $kind -12 $false
    Face $u ($v+$d) $d $h $kind -5 $false
    Face ($u+$d) ($v+$d) $w $h $kind 3 $true
    Face ($u+$d+$w) ($v+$d) $d $h $kind -10 $false
    Face ($u+2*$d+$w) ($v+$d) $w $h $kind -4 $false
}
function Atlas($size,$name,$regions){
    $script:image=New-Object Drawing.Bitmap $size,$size;$script:used=New-Object 'System.Collections.Generic.HashSet[string]'
    foreach($r in $regions){Region $r[0] $r[1] $r[2] $r[3] $r[4] $r[5]}
    $script:image.Save((Join-Path $assetRoot "$name.png"),[Drawing.Imaging.ImageFormat]::Png);$script:image.Dispose()
    Write-Output "$name : $size x $size, $($regions.Count) UV regions, no overlaps"
}
Atlas 128 'lupus_junior' @(
    @(0,0,6,6,6,'face'),@(0,14,6,7,3,'armor'),@(0,26,6,3,3,'armor'),
    @(28,0,4,3,5,'shoulder'),@(28,10,3,5,3,'armor'),@(42,10,3,5,4,'armor'),
    @(28,20,3,2,3,'gold'),@(0,34,3,5,3,'armor'),@(14,34,3,5,3,'armor'),
    @(28,28,4,3,5,'boot'),@(48,26,2,12,1,'edge'),@(46,42,5,1,2,'gold'),
    @(10,48,2,3,1,'rune'),@(0,52,7,1,7,'helmet'),@(64,0,7,3,4,'armor'),@(64,10,5,2,1,'armor'),@(80,10,3,2,4,'armor'),@(64,16,3,3,1,'armor'))
Atlas 128 'lupus_pleno' @(
    @(0,0,7,7,7,'face'),@(32,0,8,9,5,'armor'),@(64,0,7,4,4,'armor'),@(96,0,8,3,5,'armor'),
    @(0,20,6,4,7,'shoulder'),@(32,20,4,7,4,'armor'),@(64,20,4,7,5,'armor'),@(96,20,4,3,4,'gold'),
    @(0,40,4,7,4,'armor'),@(32,40,4,7,4,'armor'),@(64,40,5,3,7,'boot'),@(96,40,5,3,2,'armor'),
    @(0,60,8,2,8,'helmet'),@(48,60,3,4,1,'rune'),@(64,60,2,23,2,'shaft'),
    @(80,60,7,8,3,'blade'),@(104,60,3,12,2,'edge'),@(116,60,2,6,2,'edge'),
    @(0,84,3,3,3,'gold'),@(16,84,4,5,4,'rune'),@(36,84,5,2,5,'gold'),
    @(60,96,8,1,5,'gold'),@(88,96,5,2,3,'boot'),@(0,104,10,4,6,'armor'),@(36,104,7,2,1,'armor'),@(56,104,5,3,5,'armor'),@(80,104,4,5,2,'armor'))
