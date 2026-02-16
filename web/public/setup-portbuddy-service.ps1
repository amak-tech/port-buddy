<#
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
#>

# Check if running as Administrator
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Warning "Please run as Administrator to register a service (Scheduled Task)."
    exit 1
}

param(
    [Parameter(Position=0, Mandatory=$true)]
    [string]$Mode,
    
    [Parameter(Position=1, Mandatory=$true)]
    [string]$Port,
    
    [Parameter(Position=2)]
    [string]$HostName,
    
    [Parameter(Mandatory=$false)]
    [string]$Name
)

# Construct target argument
if ([string]::IsNullOrEmpty($HostName)) {
    $Target = "$Port"
} else {
    $Target = "$HostName`:$Port"
}

# Determine Service Name
if ([string]::IsNullOrEmpty($Name)) {
    $ServiceName = "portbuddy-$Mode-$Port"
} else {
    $ServiceName = $Name
}

# Locate portbuddy executable
$PortBuddyBin = Get-Command "portbuddy" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source
if (-not $PortBuddyBin) {
    # Try common location installed by install.ps1
    $CommonPath = Join-Path $env:USERPROFILE ".portbuddy\portbuddy.exe"
    if (Test-Path $CommonPath) {
        $PortBuddyBin = $CommonPath
    } else {
        Write-Error "Error: portbuddy executable not found in PATH or $CommonPath"
        Write-Error "Please make sure portbuddy is installed and available."
        exit 1
    }
}

Write-Host "Setting up Port Buddy service..." -ForegroundColor Cyan
Write-Host "Service Name: $ServiceName"
Write-Host "Binary: $PortBuddyBin"
Write-Host "Command: $PortBuddyBin $Mode $Target -n"
Write-Host "User Profile: $env:USERPROFILE"

# Create Scheduled Task Action
# We use cmd /c to ensure environment variables (specifically USERPROFILE) are set correctly for the context
# or rely on the fact that if we run as SYSTEM, we need to point it to the config.
# Port Buddy likely uses 'user.home' to find config.
# We set USERPROFILE to the current user's profile so it finds the config.
$UserHome = $env:USERPROFILE
$ActionCmd = "cmd.exe"
$ActionArg = "/c set USERPROFILE=$UserHome && `"$PortBuddyBin`" $Mode $Target"
$Action = New-ScheduledTaskAction -Execute $ActionCmd -Argument $ActionArg

# Create Trigger (At Startup)
$Trigger = New-ScheduledTaskTrigger -AtStartup

# Create Settings
# Restart if it fails, don't stop on battery, etc.
$Settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -RestartCount 3 `
    -RestartInterval (New-TimeSpan -Minutes 1) `
    -ExecutionTimeLimit (New-TimeSpan -Days 3650) # Effectively infinite

# Create Principal (SYSTEM account)
$Principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -LogonType ServiceAccount -RunLevel Highest

# Register Task
try {
    Register-ScheduledTask -TaskName $ServiceName -Action $Action -Trigger $Trigger -Principal $Principal -Settings $Settings -Force | Out-Null
    Write-Host "----------------------------------------"
    Write-Host "Service $ServiceName has been installed and started (scheduled)." -ForegroundColor Green
    Write-Host "To start immediately: Start-ScheduledTask -TaskName $ServiceName"
    Write-Host "Check status with: Get-ScheduledTask -TaskName $ServiceName"
    
    # Attempt to start it now
    Start-ScheduledTask -TaskName $ServiceName
} catch {
    Write-Error "Failed to register task: $_"
    exit 1
}
