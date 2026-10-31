param([Parameter(Mandatory=$true)][string[]]$Commands,[string]$RunDirectory='phase6-server',[int]$Port=25577)
$ErrorActionPreference = 'Stop'
$properties = Join-Path $PSScriptRoot "../runs/$RunDirectory/server.properties"
$password = ((Get-Content -LiteralPath $properties | Where-Object { $_ -like 'rcon.password=*' }) -split '=',2)[1]
if ([string]::IsNullOrEmpty($password)) { throw 'The isolated phase6 server has no RCON password.' }
$client = [Net.Sockets.TcpClient]::new('127.0.0.1',$Port)
try {
    $stream=$client.GetStream();$stream.ReadTimeout=60000
    $writer=[IO.BinaryWriter]::new($stream,[Text.Encoding]::UTF8,$true)
    $reader=[IO.BinaryReader]::new($stream,[Text.Encoding]::UTF8,$true)
    function Send-Packet([int]$request,[int]$type,[string]$body){
        $bytes=[Text.Encoding]::UTF8.GetBytes($body)
        $buffer=[IO.MemoryStream]::new();$packet=[IO.BinaryWriter]::new($buffer)
        $packet.Write([int]($bytes.Length+10));$packet.Write($request);$packet.Write($type);$packet.Write($bytes);$packet.Write([byte]0);$packet.Write([byte]0);$packet.Flush()
        $frame=$buffer.ToArray();$stream.Write($frame,0,$frame.Length);$stream.Flush();$packet.Dispose();$buffer.Dispose()
    }
    function Read-Packet {
        $length=$reader.ReadInt32();if($length -lt 10 -or $length -gt 1048576){throw 'Invalid RCON frame'}
        $request=$reader.ReadInt32();$type=$reader.ReadInt32();$body=$reader.ReadBytes($length-8)
        @{id=$request;type=$type;text=[Text.Encoding]::UTF8.GetString($body,0,$body.Length-2)}
    }
    Send-Packet 1 3 $password
    do {$auth=Read-Packet;if($auth.id -eq -1){throw 'RCON authentication failed'}}while($auth.type -ne 2)
    $request=2
    foreach($command in $Commands){
        Send-Packet $request 2 $command
        $reply=Read-Packet
        if($reply.id -ne $request){throw 'Unexpected RCON response'}
        [PSCustomObject]@{Command=$command;Result=$reply.text}
        $request++
    }
} finally {$client.Dispose()}
