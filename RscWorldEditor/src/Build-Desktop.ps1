$ErrorActionPreference='Stop'
$ed=Split-Path $PSScriptRoot
$out=Join-Path (Split-Path $ed) 'Open_RSCWorldEditor_bat.exe'
& "$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\csc.exe" /nologo /target:winexe /platform:x64 "/out:$out" "/win32manifest:$PSScriptRoot\DesktopApp.manifest" /reference:System.Windows.Forms.dll /reference:System.Drawing.dll "/reference:$ed\desktop\Microsoft.Web.WebView2.Core.dll" "/reference:$ed\desktop\Microsoft.Web.WebView2.WinForms.dll" "$PSScriptRoot\DesktopApp.cs"
if($LASTEXITCODE -ne 0){throw 'Desktop build failed.'}
