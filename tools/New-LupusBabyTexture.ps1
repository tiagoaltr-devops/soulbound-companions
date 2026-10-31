# Original pixel art authored for Soulbound Companions. No external images are read.
# Regenerate from the repository root with: powershell -File tools/New-LupusBabyTexture.ps1
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$bitmap = [Drawing.Bitmap]::new(64, 64, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
$palette = @{
    Ice = [Drawing.ColorTranslator]::FromHtml('#A7E8FF')
    Light = [Drawing.ColorTranslator]::FromHtml('#D9F8FF')
    Blue = [Drawing.ColorTranslator]::FromHtml('#65BEDF')
    Shade = [Drawing.ColorTranslator]::FromHtml('#3886B5')
    Navy = [Drawing.ColorTranslator]::FromHtml('#203B70')
    Magic = [Drawing.ColorTranslator]::FromHtml('#82FFF0')
    White = [Drawing.ColorTranslator]::FromHtml('#F2FFFF')
}
function Paint-Pixel([int]$X, [int]$Y, [string]$Color) { $bitmap.SetPixel($X, $Y, $palette[$Color]) }
function Paint-Rect([int]$U, [int]$V, [int]$Width, [int]$Height, [string]$Color) {
    for ($y = $V; $y -lt $V + $Height; $y++) {
        for ($x = $U; $x -lt $U + $Width; $x++) { Paint-Pixel $x $y $Color }
    }
}
function Paint-Box([int]$U, [int]$V, [int]$Width, [int]$Height, [int]$Depth) {
    Paint-Rect ($U + $Depth) $V $Width $Depth 'Light'
    Paint-Rect ($U + $Depth + $Width) $V $Width $Depth 'Shade'
    Paint-Rect $U ($V + $Depth) $Depth $Height 'Blue'
    Paint-Rect ($U + $Depth) ($V + $Depth) $Width $Height 'Ice'
    Paint-Rect ($U + $Depth + $Width) ($V + $Depth) $Depth $Height 'Blue'
    Paint-Rect ($U + 2 * $Depth + $Width) ($V + $Depth) $Width $Height 'Shade'
}
function Paint-Wing([int]$U) {
    Paint-Box $U 32 1 6 7
    foreach ($side in @($U, ($U + 8))) {
        for ($y = 0; $y -lt 6; $y++) {
            $width = @(3, 5, 7, 7, 7, 5)[$y]
            for ($x = 0; $x -lt 7; $x++) {
                if ($x -ge $width) { $bitmap.SetPixel($side + $x, 39 + $y, [Drawing.Color]::Transparent) }
                elseif ($y -eq 0 -or $x -eq 0 -or $x -eq $width - 1 -or $y -eq 5) { Paint-Pixel ($side + $x) (39 + $y) 'Navy' }
                elseif (($x + $y) % 3 -eq 0) { Paint-Pixel ($side + $x) (39 + $y) 'Magic' }
                else { Paint-Pixel ($side + $x) (39 + $y) 'Blue' }
            }
        }
    }
}
try {
    Paint-Box 0 0 7 6 6
    Paint-Box 0 16 4 5 3
    Paint-Box 16 16 2 4 2
    Paint-Box 24 16 2 4 2
    Paint-Box 40 0 2 3 2
    Paint-Box 48 0 2 3 2
    Paint-Wing 0
    Paint-Wing 20
    # Head front: U6..12, V6..11. Two magical eyes, a small nose and brow marks.
    foreach ($eye in @(7, 10)) {
        Paint-Rect $eye 7 2 3 'Navy'
        Paint-Pixel $eye 8 'Magic'
        Paint-Pixel ($eye + 1) 8 'White'
        Paint-Pixel ($eye + 1) 9 'Magic'
    }
    Paint-Pixel 8 6 'Shade'
    Paint-Pixel 10 6 'Shade'
    Paint-Pixel 9 10 'Navy'
    Paint-Pixel 9 11 'Light'
    # Lupus crescent markings on the temples and chest.
    foreach ($point in @(@(1,8), @(1,9), @(2,7), @(2,10), @(3,7), @(3,10), @(4,20), @(5,20), @(3,21), @(3,22), @(4,23), @(5,23))) {
        Paint-Pixel $point[0] $point[1] 'White'
    }
    foreach ($u in @(42, 50)) { Paint-Rect $u 0 2 2 'Navy'; Paint-Rect $u 2 2 1 'Shade' }
    foreach ($u in @(18, 26)) { Paint-Rect $u 20 2 2 'Navy'; Paint-Pixel $u 20 'Magic' }
    $output = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../src/main/resources/assets/soulbound/textures/entity/lupus_baby.png'))
    $null = [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($output))
    $bitmap.Save($output, [Drawing.Imaging.ImageFormat]::Png)
    Write-Output "Original 64x64 RGBA texture: $output"
} finally { $bitmap.Dispose() }
