# 旧版GUI贴图生成脚本

Add-Type -AssemblyName System.Drawing

$outPath = "src/client/resources/assets/items-retrieval/textures/gui/search_gui.png"

$bitmap = [System.Drawing.Bitmap]::new(256, 194)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::None

function New-Brush([string]$hex) {
    return [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($hex))
}

function New-Pen([string]$hex) {
    return [System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml($hex), 1)
}

function Fill-Rect([System.Drawing.Graphics]$g, [string]$hex, [int]$x, [int]$y, [int]$w, [int]$h) {
    $brush = New-Brush $hex
    $g.FillRectangle($brush, $x, $y, $w, $h)
    $brush.Dispose()
}

function Stroke-Rect([System.Drawing.Graphics]$g, [string]$hex, [int]$x, [int]$y, [int]$w, [int]$h) {
    $pen = New-Pen $hex
    $g.DrawRectangle($pen, $x, $y, $w - 1, $h - 1)
    $pen.Dispose()
}

function Add-StoneNoise([System.Drawing.Graphics]$g, [int]$x, [int]$y, [int]$w, [int]$h, [string[]]$palette, [int]$step, [int]$seed) {
    for ($py = $y; $py -lt ($y + $h); $py += $step) {
        for ($px = $x; $px -lt ($x + $w); $px += $step) {
            $index = [Math]::Abs((($px * 37) + ($py * 17) + ($seed * 23)) % $palette.Length)
            Fill-Rect $g $palette[$index] $px $py 1 1
        }
    }
}

function Draw-Inset([System.Drawing.Graphics]$g, [int]$x, [int]$y, [int]$w, [int]$h, [string]$outerDark, [string]$innerLight, [string]$fill) {
    Fill-Rect $g $outerDark $x $y $w $h
    Stroke-Rect $g $outerDark $x $y $w $h
    Stroke-Rect $g $innerLight ($x + 1) ($y + 1) ($w - 2) ($h - 2)
    Fill-Rect $g $fill ($x + 2) ($y + 2) ($w - 4) ($h - 4)
}

function Draw-BevelPanel([System.Drawing.Graphics]$g, [int]$x, [int]$y, [int]$w, [int]$h, [string]$base, [string]$shadow, [string]$light, [string]$core, [int]$noiseSeed, [string[]]$noisePalette) {
    Fill-Rect $g $base $x $y $w $h
    Fill-Rect $g $light $x $y $w 1
    Fill-Rect $g $light $x $y 1 $h
    Fill-Rect $g $shadow $x ($y + $h - 1) $w 1
    Fill-Rect $g $shadow ($x + $w - 1) $y 1 $h
    Fill-Rect $g $shadow ($x + 1) ($y + 1) ($w - 2) ($h - 2)
    Fill-Rect $g $core ($x + 2) ($y + 2) ($w - 4) ($h - 4)
    Add-StoneNoise $g ($x + 3) ($y + 3) ($w - 6) ($h - 6) $noisePalette 4 $noiseSeed
}

#立体感面板配色参考
# 灰色：#c6c6c6，面板主色调
# 黑色：#000000，面板最外描边，一般只有一个像素宽
# 白色：#ffffff，面板边框亮部，在面板的上边和左边
# 深灰：#555555，面板边框暗部，在面板的下边和右边

$PanelBase = "#C6C6C6"
$PanelLight = "#FFFFFF"
$PanelDark = "#555555"
$PanelOutline = "#000000"
$PanelCore = "#AFAFAF"
$PanelInsetFill = "#9A9A9A"

$SectionBase = "#B4B4B4"
$SectionCore = "#959595"

$ButtonBase = "#BDBDBD"
$ButtonCore = "#9D9D9D"

$PanelNoise = @("#BCBCBC", "#B2B2B2", "#C9C9C9", "#A7A7A7")
$InsetNoise = @("#A4A4A4", "#9A9A9A", "#B2B2B2", "#8F8F8F")
$ButtonNoise = @("#A8A8A8", "#9E9E9E", "#B8B8B8", "#939393")
$SlotNoise = @("#9E9E9E", "#8D8D8D", "#AFAFAF")

function Draw-Rivet([System.Drawing.Graphics]$g, [int]$x, [int]$y) {
    Fill-Rect $g "#8F8F8F" $x $y 3 3
    Fill-Rect $g "#E6E6E6" $x $y 2 1
    Fill-Rect $g "#E6E6E6" $x $y 1 2
    Fill-Rect $g "#4C4C4C" ($x + 2) ($y + 1) 1 2
    Fill-Rect $g "#4C4C4C" ($x + 1) ($y + 2) 2 1
}

function Draw-Slot([System.Drawing.Graphics]$g, [int]$x, [int]$y) {
    Fill-Rect $g "#000000" $x $y 18 18
    Fill-Rect $g "#555555" $x $y 18 1
    Fill-Rect $g "#555555" $x $y 1 18
    Fill-Rect $g "#FFFFFF" ($x + 17) $y 1 18
    Fill-Rect $g "#FFFFFF" $x ($y + 17) 18 1
    Fill-Rect $g "#8C8C8C" ($x + 1) ($y + 1) 16 16
    Add-StoneNoise $g ($x + 2) ($y + 2) 14 14 $SlotNoise 3 ($x + $y)
}

Fill-Rect $graphics $PanelOutline 0 0 256 194
Draw-BevelPanel $graphics 0 0 256 194 $PanelBase $PanelDark $PanelLight $PanelCore 11 $PanelNoise
Draw-Inset $graphics 3 3 250 188 $PanelDark $PanelLight $PanelInsetFill
Add-StoneNoise $graphics 6 6 244 182 $InsetNoise 5 3

Draw-BevelPanel $graphics 4 14 28 164 $SectionBase $PanelDark $PanelLight $SectionCore 19 $InsetNoise
Draw-BevelPanel $graphics 36 14 168 74 $SectionBase $PanelDark $PanelLight $SectionCore 29 $InsetNoise
Draw-BevelPanel $graphics 4 108 198 82 $SectionBase $PanelDark $PanelLight $SectionCore 37 $InsetNoise
Draw-BevelPanel $graphics 206 14 46 176 $SectionBase $PanelDark $PanelLight $SectionCore 43 $InsetNoise

Draw-Inset $graphics 40 4 160 16 "#4A4A4A" $PanelLight "#8A8A8A"
Fill-Rect $graphics "#EAEAEA" 41 5 157 1
Fill-Rect $graphics "#444444" 41 18 157 1

foreach ($buttonY in @(18, 41, 64, 87, 110)) {
    Draw-BevelPanel $graphics 209 $buttonY 42 20 $ButtonBase $PanelDark $PanelLight $ButtonCore ($buttonY + 7) $ButtonNoise
}

Draw-Inset $graphics 209 174 42 16 "#4C4C4C" $PanelLight "#8D8D8D"
Draw-Rivet $graphics 4 4
Draw-Rivet $graphics 249 4
Draw-Rivet $graphics 4 187
Draw-Rivet $graphics 249 187

for ($i = 0; $i -lt 8; $i++) {
    Draw-Slot $graphics 8 (18 + 20 * $i)
}

for ($row = 0; $row -lt 3; $row++) {
    for ($col = 0; $col -lt 9; $col++) {
        Draw-Slot $graphics (40 + 18 * $col) (18 + 18 * $row)
    }
}

for ($row = 0; $row -lt 3; $row++) {
    for ($col = 0; $col -lt 9; $col++) {
        Draw-Slot $graphics (8 + 18 * $col) (112 + 18 * $row)
    }
}

for ($col = 0; $col -lt 9; $col++) {
    Draw-Slot $graphics (8 + 18 * $col) 170
}

$bitmap.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)

$graphics.Dispose()
$bitmap.Dispose()

Write-Output "Generated texture: $outPath"
