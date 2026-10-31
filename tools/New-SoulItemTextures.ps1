# Original Soulbound item pixel art. No third-party files are read.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$output = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../src/main/resources/assets/soulbound/textures/item'))
$null = [IO.Directory]::CreateDirectory($output)
function Pixel([Drawing.Bitmap]$Image, [int]$X, [int]$Y, [string]$Color) {
    $Image.SetPixel($X, $Y, [Drawing.ColorTranslator]::FromHtml($Color))
}
function Egg([bool]$Broken) {
    $image = [Drawing.Bitmap]::new(16, 16, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $widths = @(0, 0, 2, 4, 6, 8, 10, 10, 12, 12, 12, 10, 8, 6, 0, 0)
    for ($y = 2; $y -le 13; $y++) {
        $w = $widths[$y]; $start = (16 - $w) / 2
        for ($x = $start; $x -lt $start + $w; $x++) {
            $color = if ($x -eq $start -or $x -eq $start + $w - 1 -or $y -eq 13) { '#203B70' }
                elseif ($x -le 6 -and $y -lt 9) { '#D9F8FF' } else { '#65BEDF' }
            Pixel $image $x $y $color
        }
    }
    foreach ($p in @(@(7,5),@(8,5),@(6,6),@(6,7),@(6,8),@(7,9),@(8,9))) { Pixel $image $p[0] $p[1] '#82FFF0' }
    Pixel $image 8 7 '#F2FFFF'
    if ($Broken) {
        foreach ($p in @(@(8,3),@(7,4),@(8,5),@(8,6),@(9,7),@(8,8),@(7,9),@(8,10),@(8,11),@(9,12))) {
            Pixel $image $p[0] $p[1] '#5C284F'
        }
        Pixel $image 9 8 '#D47F9B'
        $image.SetPixel(7, 4, [Drawing.Color]::Transparent)
        $image.SetPixel(8, 5, [Drawing.Color]::Transparent)
        $image.SetPixel(8, 6, [Drawing.Color]::Transparent)
        $image.SetPixel(9, 7, [Drawing.Color]::Transparent)
    }
    return ,$image
}
function Staff([bool]$Inactive) {
    $image = [Drawing.Bitmap]::new(16, 16, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $dark = if ($Inactive) { '#353B4A' } else { '#203B70' }
    $light = if ($Inactive) { '#747C8C' } else { '#65BEDF' }
    $magic = if ($Inactive) { '#9399A3' } else { '#82FFF0' }
    for ($i = 0; $i -le 8; $i++) {
        Pixel $image (2 + $i) (14 - $i) $dark
        Pixel $image (3 + $i) (14 - $i) $light
        Pixel $image (3 + $i) (15 - $i) $dark
    }
    foreach ($p in @(@(11,0),@(10,1),@(12,1),@(9,2),@(13,2),@(9,3),@(13,3),@(9,4),@(13,4),@(10,5),@(12,5),@(11,6))) {
        Pixel $image $p[0] $p[1] $dark
    }
    foreach ($p in @(@(11,1),@(10,2),@(11,2),@(12,2),@(10,3),@(11,3),@(12,3),@(10,4),@(11,4),@(12,4),@(11,5))) {
        Pixel $image $p[0] $p[1] $magic
    }
    Pixel $image 11 2 $(if ($Inactive) { '#BBC2C9' } else { '#F2FFFF' })
    Pixel $image 11 4 $light
    Pixel $image 8 6 $magic
    Pixel $image 13 6 $magic
    return ,$image
}
foreach ($entry in @(@('soul_egg', $false), @('shattered_egg', $true))) {
    $image = Egg $entry[1]
    try { $image.Save((Join-Path $output ($entry[0] + '.png')), [Drawing.Imaging.ImageFormat]::Png) } finally { $image.Dispose() }
}
foreach ($entry in @(@('soul_staff', $false), @('inactive_staff', $true))) {
    $image = Staff $entry[1]
    try { $image.Save((Join-Path $output ($entry[0] + '.png')), [Drawing.Imaging.ImageFormat]::Png) } finally { $image.Dispose() }
}
Write-Output "Four original 16x16 item textures written to $output"
