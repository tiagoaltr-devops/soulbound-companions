$ErrorActionPreference='Stop'
$translations=@{
 'menu.soulbound.god_short'=@('Forma God','God form')
 'menu.soulbound.locate_ruin'=@('Localizar Ruína','Locate Ruin')
 'menu.soulbound.resonance_searching'=@('Ressonando...','Resonating...')
 'menu.soulbound.resonance_cooldown'=@('Ruína: %s s','Ruin: %s s')
 'message.soulbound.resonance_unavailable'=@('Ressonância Ancestral indisponível: seu Lupus precisa estar ativo, próximo e no Overworld.','Ancestral Resonance unavailable: your Lupus must be active, nearby and in the Overworld.')
 'message.soulbound.resonance_cooldown'=@('A Ressonância Ancestral estará pronta em %s segundos.','Ancestral Resonance will be ready in %s seconds.')
 'message.soulbound.resonance_searching'=@('Seu companheiro procura uma ressonância ancestral...','Your companion searches for an ancestral resonance...')
 'message.soulbound.resonance_found'=@('Seu Lupus sentiu uma Ruína Ancestral. Observe a direção indicada.','Your Lupus sensed an Ancestral Ruin. Watch the direction it indicates.')
 'message.soulbound.resonance_none'=@('Seu companheiro não consegue sentir nenhuma Ruína Ancestral próxima.','Your companion cannot sense any nearby Ancestral Ruin.')
 'message.soulbound.resonance_technical'=@('A ressonância foi interrompida durante a localização. Tente novamente; a recarga não foi consumida.','The resonance was interrupted during discovery. Try again; the cooldown was not consumed.')
 'message.soulbound.resonance_blocked'=@('Seu Lupus não encontrou um caminho aéreo seguro para indicar a ruína.','Your Lupus could not find a safe aerial path to indicate the ruin.')
 'message.soulbound.ruin_not_natural'=@('Este pedestal não possui uma Ruína Ancestral natural comprovada.','This pedestal has no verified natural Ancestral Ruin.')
}
for($i=0;$i -lt 2;$i++){
 $locale=@('pt_br','en_us')[$i];$path=Join-Path $PSScriptRoot "../src/main/resources/assets/soulbound/lang/$locale.json"
 $original=Get-Content -LiteralPath $path -Raw -Encoding UTF8 | ConvertFrom-Json
 $merged=[ordered]@{};foreach($p in $original.PSObject.Properties){$merged[$p.Name]=$p.Value}
 foreach($key in $translations.Keys){$merged[$key]=$translations[$key][$i]}
 [IO.File]::WriteAllText($path,($merged | ConvertTo-Json -Depth 20)+"`n",[Text.UTF8Encoding]::new($false))
}
