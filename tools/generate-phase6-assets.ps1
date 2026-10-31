$ErrorActionPreference = 'Stop'
$resourceRoot = Join-Path $PSScriptRoot '../src/main/resources'
function Write-Resource([string]$relative, $data) {
    $target = Join-Path $resourceRoot $relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target)) | Out-Null
    [IO.File]::WriteAllText($target, ($data | ConvertTo-Json -Depth 30) + "`n", [Text.UTF8Encoding]::new($false))
}
function Recipe([string]$id, [string[]]$ingredients, [int]$count = 1) {
    Write-Resource "data/soulbound/recipe/$id.json" @{type='minecraft:crafting_shapeless';category='misc';ingredients=$ingredients;result=@{id="soulbound:$id";count=$count}}
}
Recipe 'offering_altar' @('minecraft:obsidian','minecraft:obsidian','minecraft:gold_ingot','minecraft:amethyst_shard') 1
Recipe 'xp_platform' @('minecraft:obsidian','minecraft:obsidian','minecraft:lapis_block','minecraft:sculk') 1
Recipe 'ritual_stone' @('minecraft:stone_bricks','minecraft:stone_bricks','minecraft:amethyst_shard','minecraft:lapis_lazuli') 4
Recipe 'reactivation_essence' @('minecraft:echo_shard','minecraft:ghast_tear','minecraft:amethyst_shard','minecraft:netherite_scrap')
Recipe 'awakening_core' @('minecraft:obsidian','minecraft:obsidian','minecraft:obsidian','minecraft:obsidian','minecraft:diamond')
Recipe 'ascension_core' @('minecraft:netherite_block','minecraft:echo_shard','minecraft:diamond')
Recipe 'divine_core' @('soulbound:draconic_essence','minecraft:nether_star','minecraft:amethyst_shard')
Recipe 'restoration_core' @('minecraft:lily_of_the_valley','minecraft:diamond','minecraft:diamond','minecraft:diamond','minecraft:diamond')
$offerings = @{
    activation=@(@{item='minecraft:diamond';count=2},@{item='minecraft:amethyst_shard';count=8},@{item='minecraft:echo_shard';count=1})
    awakening=@(@{item='minecraft:diamond';count=2},@{item='minecraft:amethyst_shard';count=8},@{item='minecraft:lapis_lazuli';count=16})
    ascension=@(@{item='minecraft:netherite_ingot';count=1},@{item='minecraft:echo_shard';count=2},@{item='minecraft:diamond_block';count=1})
    divine=@(@{item='soulbound:draconic_essence';count=1},@{item='minecraft:nether_star';count=1},@{item='minecraft:end_crystal';count=4})
    restoration=@(@{item='minecraft:diamond';count=4},@{item='minecraft:lily_of_the_valley';count=1},@{item='minecraft:ghast_tear';count=1})
}
foreach ($entry in $offerings.GetEnumerator()) { Write-Resource "data/soulbound/soul_ritual/$($entry.Key).json" @{offerings=$entry.Value} }
Add-Type -AssemblyName System.Drawing
$blocks=@('ancestral_pedestal','soul_forge','offering_altar','xp_platform','ritual_stone')
$items=@('ancestral_spark','reactivation_essence','draconic_essence','awakening_core','ascension_core','divine_core','restoration_core','charged_core')
foreach ($id in ($items + $blocks)) {
    $block = $blocks -contains $id
    $bitmap=[Drawing.Bitmap]::new(16,16)
    for($y=0;$y -lt 16;$y++){for($x=0;$x -lt 16;$x++){
        $color=[Drawing.Color]::Transparent
        $distance=[Math]::Abs($x-7.5)+[Math]::Abs($y-7.5)
        if($block){
            $shade=30+(($x*17+$y*31)%24)
            $color=[Drawing.Color]::FromArgb(255,$shade,$shade+13,$shade+20)
            if(($x -eq 3 -or $x -eq 12) -and $y -ge 3 -and $y -le 12 -or ($y -eq 3 -or $y -eq 12) -and $x -ge 3 -and $x -le 12){$color=[Drawing.Color]::FromArgb(255,62,166,203)}
            if($distance -lt 3){$color=[Drawing.Color]::FromArgb(255,160,237,251)}
        }elseif($distance -le 6){
            $color=if($distance -gt 4.5){[Drawing.Color]::FromArgb(255,25,61,92)}else{[Drawing.Color]::FromArgb(255,70,169,220)}
            if($id -eq 'restoration_core'){$color=[Drawing.Color]::FromArgb(255,121,195,154)}
            if($id -eq 'divine_core' -or $id -eq 'draconic_essence'){$color=[Drawing.Color]::FromArgb(255,168,92,222)}
            if($id -eq 'ascension_core'){$color=[Drawing.Color]::FromArgb(255,113,103,152)}
            if($x -le 7 -and $y -lt 7 -and $distance -lt 4){$color=[Drawing.Color]::FromArgb(255,203,246,255)}
        }
        if($id -eq 'ancestral_spark' -and ($x -eq 7 -or $y -eq 7) -and $distance -le 7){$color=[Drawing.Color]::FromArgb(255,209,250,255)}
        $bitmap.SetPixel($x,$y,$color)
    }}
    $folder=if($block){'block'}else{'item'}
    $png=Join-Path $resourceRoot "assets/soulbound/textures/$folder/$id.png"
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($png)) | Out-Null
    $bitmap.Save($png,[Drawing.Imaging.ImageFormat]::Png);$bitmap.Dispose()
    if($block){
        Write-Resource "assets/soulbound/blockstates/$id.json" @{variants=@{''=@{model="soulbound:block/$id"}}}
        Write-Resource "assets/soulbound/models/block/$id.json" @{parent='minecraft:block/cube_all';textures=@{all="soulbound:block/$id"}}
        if($id -in @('offering_altar','xp_platform','ritual_stone')){
            Write-Resource "data/soulbound/loot_table/blocks/$id.json" @{type='minecraft:block';pools=@(@{rolls=1;entries=@(@{type='minecraft:item';name="soulbound:$id"});conditions=@(@{condition='minecraft:survives_explosion'})})}
        }
    }
    if($id -ne 'ancestral_pedestal'){
        Write-Resource "assets/soulbound/items/$id.json" @{model=@{type='minecraft:model';model="soulbound:item/$id"}}
        if($block){Write-Resource "assets/soulbound/models/item/$id.json" @{parent="soulbound:block/$id"}}
        else{Write-Resource "assets/soulbound/models/item/$id.json" @{parent='minecraft:item/generated';textures=@{layer0="soulbound:item/$id"}}}
    }
}
$translations=@{
    'menu.soulbound.trial'=@('Provação das Três Almas','Trial of Three Souls')
    'menu.soulbound.trial_start'=@('Iniciar provação individual','Begin solo trial')
    'menu.soulbound.trial_level'=@('Nível da ruína: %s','Ruin tier: %s')
    'menu.soulbound.trial_phase.-1'=@('Aguardando desafiante','Awaiting challenger')
    'menu.soulbound.trial_phase.0'=@('Sabedoria: escolha o símbolo da pista','Wisdom: choose the symbol in the clue')
    'menu.soulbound.trial_phase.1'=@('Coragem: vença os guardiões','Courage: defeat the guardians')
    'menu.soulbound.trial_phase.2'=@('Vínculo: companheiro a até 3 blocos','Bond: companion within 3 blocks')
    'menu.soulbound.bond_progress'=@('Canalização: %s segundos','Channeling: %s seconds')
    'entity.soulbound.spirit_guardian'=@('Guardião espiritual (provisório)','Spirit guardian (placeholder)')
    'message.soulbound.trial_complete'=@('As três almas concederam uma Centelha Ancestral.','The three souls granted an Ancestral Spark.')
    'message.soulbound.trial_cancelled'=@('Provação cancelada. Nova tentativa em %s segundos.','Trial cancelled. Retry in %s seconds.')
    'message.soulbound.trial_denied'=@('Entrada negada: verifique companheiro ativo, cooldown, conclusão anterior e perímetro livre.','Entry denied: check active companion, cooldown, previous completion and empty perimeter.')
    'menu.soulbound.forge'=@('Forja da Alma — %s','Soul Forge — %s')
    'menu.soulbound.forge_confirm'=@('Confirmar ativação / risco de reativação','Confirm activation / reactivation risk')
    'menu.soulbound.core_exchange'=@('Inserir da mão / retirar núcleo','Insert held item / retrieve core')
    'menu.soulbound.ritual_confirm'=@('Confirmar ritual e sacrificar níveis','Confirm ritual and sacrifice levels')
    'menu.soulbound.forge_state.0'=@('Sem proprietário — ativação inicial','Unbound — first activation')
    'menu.soulbound.forge_state.1'=@('Inativa — reativação necessária','Inactive — reactivation required')
    'menu.soulbound.forge_state.2'=@('Ativa','Active')
    'menu.soulbound.forge_state.3'=@('Destruída definitivamente','Permanently destroyed')
    'menu.soulbound.forge_attempts'=@('Tentativas de reativação: %s','Reactivation attempts: %s')
    'menu.soulbound.forge_risk'=@('Falha: %s%% | Destruição: %s%%','Failure: %s%% | Destruction: %s%%')
    'menu.soulbound.forge_materials'=@('Altares: oposto / esquerda / direita','Altars: opposite / left / right')
    'menu.soulbound.empty'=@('Espaço do núcleo vazio','Core storage empty')
    'menu.soulbound.forge_essence'=@('Reativação: essência no espaço do núcleo','Reactivation: essence in the core storage')
    'menu.soulbound.core_present'=@('Núcleo armazenado: %s (0 = vazio)','Stored core: %s (0 = empty)')
    'message.soulbound.forge.active'=@('A Forja da Alma foi ativada.','The Soul Forge is active.')
    'message.soulbound.forge.inactive'=@('Falha comum. Materiais consumidos; desgaste mantido.','Ordinary failure. Materials consumed; wear retained.')
    'message.soulbound.forge.destroyed'=@('Falha crítica. Esta Forja foi destruída permanentemente.','Critical failure. This Forge is permanently destroyed.')
    'message.soulbound.ritual_complete'=@('Core Carregado pronto. Combine com seu Bastão na bancada.','Charged Core ready. Combine with your staff at a crafting table.')
    'stage.soulbound.junior'=@('Junior','Junior')
    'stage.soulbound.pleno'=@('Pleno','Pleno')
    'menu.soulbound.god'=@('Transformação God (se desbloqueada)','God transformation (if unlocked)')
    'message.soulbound.god_active'=@('Forma God: 60 segundos. Recarga: 15 minutos.','God form: 60 seconds. Cooldown: 15 minutes.')
    'message.soulbound.god_denied'=@('Transformação indisponível: verifique estágio, desbloqueio e recarga.','Transformation unavailable: check stage, unlock and cooldown.')
    'menu.soulbound.ritual_cost'=@('Custo: %s níveis | Duração: %s s','Cost: %s levels | Duration: %s s')
    'tooltip.soulbound.altar_token'=@('Identidade: %s','Identity: %s')
    'tooltip.soulbound.spark'=@('Uma autorização ancestral de fabricação.','One ancestral forge crafting authorization.')
    'tooltip.soulbound.core_tradeable'=@('Carregado • negociável • uso único no servidor','Charged • tradeable • single server-authorized use')
}
$symbols=@(@('Sol','Sun','A luz que desperta ao amanhecer.','The light that rises at dawn.'),@('Lua','Moon','Guardiã prateada da noite.','Silver guardian of the night.'),@('Chama','Flame','Devora madeira e aquece a pedra.','Devours wood and warms stone.'),@('Onda','Wave','Corre sem pernas e molda as margens.','Runs without legs and shapes the banks.'),@('Raiz','Root','Segura a árvore sob nossos pés.','Holds the tree beneath our feet.'),@('Asa','Wing','Carrega o viajante pelos céus.','Carries the traveler through the sky.'),@('Alma','Soul','O vínculo que permanece além da vida.','The bond that remains beyond life.'))
for($i=0;$i -lt 7;$i++){$translations["symbol.soulbound.$i"]=@($symbols[$i][0],$symbols[$i][1]);$translations["clue.soulbound.$i"]=@($symbols[$i][2],$symbols[$i][3])}
$names=@{ancestral_pedestal=@('Pedestal Ancestral','Ancestral Pedestal');soul_forge=@('Forja da Alma','Soul Forge');offering_altar=@('Altar de Oferenda','Offering Altar');xp_platform=@('Plataforma de Sacrifício','XP Sacrifice Platform');ritual_stone=@('Pedra Ritualística','Ritual Stone');ancestral_spark=@('Centelha Ancestral','Ancestral Spark');reactivation_essence=@('Essência de Reativação','Reactivation Essence');draconic_essence=@('Essência Dracônica','Draconic Essence');awakening_core=@('Core do Despertar','Awakening Core');ascension_core=@('Core da Ascensão','Ascension Core');divine_core=@('Core Divino','Divine Core');restoration_core=@('Core da Restauração','Restoration Core');charged_core=@('Core Carregado','Charged Core')}
foreach($id in $names.Keys){$prefix=if($blocks -contains $id){'block'}else{'item'};$translations["$prefix.soulbound.$id"]=$names[$id]}
foreach($kind in @('awakening','ascension','divine','restoration')){$translations["item.soulbound.charged_core.$kind"]=@("$($names["${kind}_core"][0]) Carregado","Charged $($names["${kind}_core"][1])")}
foreach($lang in @('pt_br','en_us')){
    $file=Join-Path $resourceRoot "assets/soulbound/lang/$lang.json"
    $old=Get-Content -LiteralPath $file -Raw -Encoding UTF8 | ConvertFrom-Json
    $merged=[ordered]@{};foreach($property in $old.PSObject.Properties){$merged[$property.Name]=$property.Value}
    $index=if($lang -eq 'pt_br'){0}else{1};foreach($key in $translations.Keys){$merged[$key]=$translations[$key][$index]}
    Write-Resource "assets/soulbound/lang/$lang.json" $merged
}
