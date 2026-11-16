# Build an installable IntelliJ plugin zip that bundles the Agent Core.
# 构建可直接安装的 IntelliJ 插件 zip（内置 Agent Core 可执行文件，免 Python 运行）。
#
# Usage: powershell -File scripts/build_plugin.ps1 [-SkipNative] [-SkipCore] [-Python python]
#
# 运行时输出保持 ASCII：Windows PowerShell 5.1 会按 ANSI 读取无 BOM 的脚本。

param(
    [switch]$SkipNative,
    [switch]$SkipCore,
    [string]$Python = "python"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $cargo = Get-Command cargo -ErrorAction SilentlyContinue
    if (-not $cargo) {
        $fallback = Join-Path $env:USERPROFILE ".cargo\bin\cargo.exe"
        if (Test-Path $fallback) { $cargo = Get-Item $fallback }
    }

    if (-not $SkipNative) {
        if ($cargo) {
            Write-Host "== build native =="
            & powershell -NoProfile -File (Join-Path $root "scripts/build_native.ps1")
            if ($LASTEXITCODE -ne 0) { throw "build_native.ps1 failed" }
        }
        else {
            Write-Host "cargo not found: skipping the Rust accelerator (pure Python fallback stays active)"
        }
    }

    if (-not $SkipCore) {
        Write-Host "== build core executable =="
        & powershell -NoProfile -File (Join-Path $root "scripts/build_core.ps1") -Python $Python
        if ($LASTEXITCODE -ne 0) { throw "build_core.ps1 failed" }
    }

    $coreExe = Get-ChildItem (Join-Path $root "core/dist") -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -eq "agent-core" -or $_.Name -eq "agent-core.exe" } |
        Select-Object -First 1
    if (-not $coreExe) {
        throw "core/dist/agent-core* is missing: the plugin zip would ship without a Core (run without -SkipCore)"
    }

    Write-Host "== gradlew buildPlugin =="
    # The plugin targets Java 21 bytecode, so Gradle must run on a JDK 21+ VM.
    # 插件目标是 Java 21 字节码，Gradle 必须跑在 JDK 21+ 上。
    $jdkMajor = 0
    if ($env:JAVA_HOME) {
        $releaseFile = Join-Path $env:JAVA_HOME "release"
        if (Test-Path $releaseFile) {
            $line = Select-String -Path $releaseFile -Pattern '^JAVA_VERSION=' | Select-Object -First 1
            if ($line -and ($line.Line -match 'JAVA_VERSION="?(\d+)')) { $jdkMajor = [int]$Matches[1] }
        }
    }
    if ($jdkMajor -lt 21) {
        throw "JAVA_HOME must point to a JDK 21+ (current: '$env:JAVA_HOME'). Install JDK 21, or on IDEA installs set JAVA_HOME to '<IDE>\jbr' which is 25."
    }

    # Reuse the local IDE install when IDEA_HOME is unset: no ~1.5 GB platform download.
    # 未设置 IDEA_HOME 时自动找本机 IDEA，避免 Gradle 再下 1.5 GB 平台包。
    if (-not $env:IDEA_HOME) {
        $ideRoots = @(
            (Join-Path $env:LOCALAPPDATA "Programs"),
            "C:\Program Files\JetBrains",
            "D:\"
        )
        $installs = foreach ($r in $ideRoots) {
            if (Test-Path $r) {
                Get-ChildItem $r -Directory -Filter "IntelliJ IDEA*" -ErrorAction SilentlyContinue
            }
        }
        $pick = $installs |
            Where-Object { Test-Path (Join-Path $_.FullName "build.txt") } |
            Sort-Object Name -Descending |
            Select-Object -First 1
        if ($pick) {
            $env:IDEA_HOME = $pick.FullName
            Write-Host "IDEA_HOME: $env:IDEA_HOME"
        }
    }

    & .\gradlew.bat --console=plain buildPlugin
    if ($LASTEXITCODE -ne 0) { throw "gradlew buildPlugin failed" }

    $zip = Get-ChildItem (Join-Path $root "build/distributions") -Filter "*.zip" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $zip) { throw "no plugin zip produced under build/distributions" }

    $sizeMb = [math]::Round($zip.Length / 1MB, 1)
    Write-Host ""
    Write-Host "plugin zip: $($zip.FullName) ($sizeMb MB)"
    Write-Host "install: Settings > Plugins > gear > Install Plugin from Disk..."
    Write-Host "the Core executable is bundled at agent-core/ inside the zip, no Python needed."
}
finally {
    Pop-Location
}
