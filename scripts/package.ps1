# ============================================================
# Packaging VetPharma — génère une application Windows installable
# avec JVM embarquée (l'utilisateur final n'a PAS besoin de Java).
#
# Usage :   .\scripts\package.ps1            → app-image (dossier portable)
#           .\scripts\package.ps1 -Type msi  → installateur MSI (requiert WiX Toolset 3.x)
#
# Résultat : dist\VetPharma\VetPharma.exe
# ============================================================
param(
    [ValidateSet("app-image", "msi", "exe")]
    [string]$Type = "app-image"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$appVersion = "1.0.0"

Write-Host "[1/4] Compilation et packaging Maven (tests inclus)..." -ForegroundColor Cyan
& .\mvnw.cmd -q clean package
if ($LASTEXITCODE -ne 0) { throw "Échec du build Maven." }

Write-Host "[2/4] Copie des dépendances vers target\libs..." -ForegroundColor Cyan
& .\mvnw.cmd -q dependency:copy-dependencies "-DoutputDirectory=target\libs" "-DincludeScope=runtime"
if ($LASTEXITCODE -ne 0) { throw "Échec de la copie des dépendances." }

Write-Host "[3/4] Préparation du dossier d'entrée jpackage..." -ForegroundColor Cyan
$inputDir = "target\jpackage-input"
if (Test-Path $inputDir) { Remove-Item -Recurse -Force $inputDir }
New-Item -ItemType Directory -Force $inputDir | Out-Null
Copy-Item "target\vet-pharmacy-1.0-SNAPSHOT.jar" $inputDir
Copy-Item "target\libs\*" $inputDir

Write-Host "[4/4] Génération de l'application ($Type) avec jpackage..." -ForegroundColor Cyan
if (Test-Path "dist") { Remove-Item -Recurse -Force "dist" }

$jpackageArgs = @(
    "--type", $Type,
    "--name", "VetPharma",
    "--app-version", $appVersion,
    "--vendor", "VetPharma",
    "--description", "Gestion de Pharmacie Veterinaire",
    "--input", $inputDir,
    "--main-jar", "vet-pharmacy-1.0-SNAPSHOT.jar",
    "--main-class", "com.pharmacie.Launcher",
    "--dest", "dist",
    "--java-options", "-Dfile.encoding=UTF-8"
)
if ($Type -ne "app-image") {
    # Options installateur : menu Démarrer, raccourci bureau, désinstallation propre
    $jpackageArgs += @("--win-menu", "--win-shortcut", "--win-dir-chooser")
}

& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "Échec de jpackage." }

Write-Host ""
Write-Host "Packaging terminé !" -ForegroundColor Green
if ($Type -eq "app-image") {
    Write-Host "Application portable : dist\VetPharma\VetPharma.exe"
    Write-Host "IMPORTANT : placez un config.properties (copie de config.properties.example)"
    Write-Host "dans le répertoire de travail de l'application avant le premier lancement."
} else {
    Write-Host "Installateur : dist\VetPharma-$appVersion.$Type"
}
