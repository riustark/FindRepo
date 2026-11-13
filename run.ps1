# Helper script to load .env and start the application
if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $name, $value = $line.Split("=", 2)
            [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), "Process")
        }
    }
    Write-Host "Loaded environment variables from .env"
}

.\mvn.cmd spring-boot:run

