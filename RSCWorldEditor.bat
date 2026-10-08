@echo off
set "WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS=--use-gl=angle --use-angle=swiftshader --enable-unsafe-swiftshader"
start "" "%~dp0Open_RSCWorldEditor_bat.exe"
