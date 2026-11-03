# Original pixel-art UV atlases. No source images or third-party assets are read.
# Run: powershell -NoProfile -ExecutionPolicy Bypass -File tools/generate-god-prime.ps1
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot=Join-Path $PSScriptRoot '../src/main/resources/assets/soulbound/textures/entity'
$script:dark=@(24,34,48);$script:gold=@(212,178,96);$script:cyan=@(102,245,255)
function Pixel($x,$y,$rgb,$shade=0){
    $r=[Math]::Max(0,[Math]::Min(255,[int]($rgb[0]+$shade)));$g=[Math]::Max(0,[Math]::Min(255,[int]($rgb[1]+$shade)));$b=[Math]::Max(0,[Math]::Min(255,[int]($rgb[2]+$shade)))
    $script:image.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,$r,$g,$b))
}
function Face($x,$y,$w,$h,$kind,$light,$front){
    $base=switch($kind){'face'{@(92,128,141)} 'shaft'{@(87,62,44)} 'edge'{@(116,197,204)} 'blade'{@(59,98,117)} 'gold'{$script:gold} 'rune'{$script:dark} default{$script:dark}}
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
Atlas 128 'lupus_god_prime' @(
 @(0,0,7,7,7,'face'),@(32,0,8,8,5,'armor'),@(64,0,6,5,3,'armor'),@(96,0,7,3,4,'armor'),
 @(0,20,5,3,6,'shoulder'),@(32,20,3,7,3,'armor'),@(64,20,3,7,4,'armor'),@(96,20,3,3,3,'gold'),
 @(0,40,3,7,3,'armor'),@(32,40,3,6,3,'armor'),@(64,40,4,3,6,'boot'),@(96,40,4,3,1,'armor'),
 @(0,64,8,1,8,'helmet'),@(32,64,6,1,1,'gold'),@(48,64,2,4,1,'rune'),@(64,64,1,4,1,'gold'),
 @(64,84,2,23,2,'shaft'),@(0,84,3,5,3,'rune'),@(16,84,3,3,3,'gold'),
 @(80,84,5,7,2,'blade'),@(112,84,2,11,2,'edge'),@(48,84,2,5,2,'edge'),@(96,84,2,5,2,'armor'))