# Fase 6.10 — Refinamento visual e preparação pré-release

Minecraft 1.21.11, NeoForge 21.11.48, Java 21. Dependências, wrapper e versão
do artefato (`1.0.0`) preservados. As homologações anteriores de God e
sobrevivência foram informadas pelo usuário; esta fase exige nova inspeção visual.

## 6.10.1 — Visual

O diagnóstico mostrou troncos dominados por uma única caixa, ombreiras pouco
articuladas e textura com painéis muito uniformes. Também havia um erro concreto:
a canela do Pleno tinha altura 7, mas a região da textura era gerada com altura 6.

- Junior: núcleo do tronco mais estreito, peitoral separado, abdômen recuado,
  proteção leve de quadril/cotovelo, ombreiras inclinadas e aro da cabeça menor.
  Preserva a arma espiritual integrada ao antebraço e a silhueta jovem.
- Pleno: peito largo sobre tronco mais estreito, cintura afunilada, proteções
  inclinadas de quadril e cotovelo. As lâminas do machado têm inclinação própria,
  com pivô junto à cabeça da arma; o cabo continua sendo a referência das mãos.
- Texturas originais regeneradas pelo script existente: painéis escuros, bordas
  douradas e runas ciano. Junior agora usa atlas 128×128; Pleno mantém 128×128.
  Novas peças têm regiões UV próprias e a canela do Pleno foi corrigida.
- Caminhada ganhou deslocamento vertical discreto. Junior apresenta ataque com
  recuperação; os golpes pesados do Pleno retornam à pose de repouso dentro da
  duração já recebida do servidor. Não foram alterados dano, cooldown ou duração.
- Baby e God, seus modelos, texturas e animações não foram redesenhados.
  Hitboxes, render states e origens das articulações existentes foram preservados.
  Equipamentos continuam invisíveis.

O pipeline permanece `EntityModel`/`LayerDefinition`, com estado extraído pelo
renderer existente. Não há bibliotecas novas, partículas extras ou novas
alocações no caminho de animação. As peças são cuboides articulados da linguagem
visual vanilla; não se trata de uma malha orgânica importada.

## 6.10.2 — Estabilização

Revisados inventário, projeção dos equipamentos, sincronização da HUD, alimentação,
regeneração, IA defensiva, controladores de combate e persistência canônica.

O caminho normal de clique já verificava proprietário e vínculo. Foi acrescentada
a mesma autorização à entrada pública `quickMoveStack`, impedindo chamadas diretas
por outro jogador. Trata-se de proteção adicional; não foi demonstrado um exploit
de rede no caminho normal. Cliques sobre uma sessão desatualizada continuam sendo
recusados e atualizam a visão antes de permitir nova transferência.

`CompanionEquipment.capture` copiava as cinco stacks em todos os ticks, mesmo sem
mudança. Agora só renova essas cópias quando os equipamentos realmente mudam.
Serialização, durabilidade e aplicação dos modificadores mantêm as mesmas regras.

Não houve alteração de fórmulas, talentos, HP-base, alimentação por saturação,
cooldown alimentar de 100 ticks, limiares defensivos 50%/65%, combate, rituais,
Recall ou regras de ressurreição. God permanece com duração 60 s e cooldown de
300 s para novas ativações, conforme o ajuste anterior solicitado pelo usuário.

### Validação executada

- Build: suíte existente preservada; 2.533 verificações de geometria, faces UV,
  poses e aderência das mãos ao machado, além das demais suítes de regressão.
- Recursos: bake real dos modelos sem GPU, faces UV dentro do atlas e centros
  das faces pintados; gerador verifica limites e sobreposição de regiões.
- Legado: registros Baby/Junior/Pleno sem `survival`, `forms` e `progression`
  carregam com defaults, preservando UUIDs e estágio. Outras suítes existentes
  continuam cobrindo God, talentos, identidade, Recall e serialização.
- Servidor dedicado isolado: 23 verificações nativas de itens e 54 de integração,
  incluindo atributos, encantamentos, desgaste, HP, alimentação, escudo, fuga,
  Ficar, dois proprietários simulados e rejeição de transferência por terceiro.
- Sessão desatualizada: uma alteração canônica externa impede o primeiro clique;
  o clique posterior transfere exatamente a quantidade atual, sem duplicá-la.
- Reinício real do mesmo servidor: duas verificações comparam identidade, formas,
  progressão, inventário e cooldowns com um snapshot anterior. O timestamp de
  localização pode avançar legitimamente; o cooldown do Recall deve permanecer.

O mundo usado é apenas `runs/phase69-server/phase69-validation`, já destinado às
fixtures. As instâncias de teste são encerradas após as verificações. Não foram
modificados mundos de produção nem gerados mundos de worldgen nesta fase.

Limites: FakePlayers não simulam transporte de rede ou dois clientes gráficos.
Os testes não homologam clipping, estética, latência, desconexão real durante
cliques, corte abrupto de armazenamento ou integração com todos os outros mods.
Inventário do jogador e SavedData continuam sem transação física única contra
crash/rollback seletivo. Encantamentos que exigem exclusivamente um jogador e
habilidades específicas de outros mods não recebem suporte artificial.

## 6.10.3 — Pré-release

Classificação e destino dos comandos:

| Categoria | Comandos | Destino |
| --- | --- | --- |
| Debug de progressão | `/soulbound debug setlevel`, `addxp` | Somente desenvolvimento, continuam exigindo OP |
| Diagnóstico interno | `/soulbound altar inspect`, `candidate`, `ruins`, `ruin_scan`, `ruin_candidates`, `ruin_village_probe` | Somente desenvolvimento |
| Recuperação/suporte pessoal | `/soulbound recover` | Preservado no release; somente vínculo do executor |
| Funcionalidade de jogador | `/soulbound god` | Preservado com as validações existentes |
| Administração vanilla | Comandos do Minecraft | Sem alteração |

As três classes de registro exclusivamente diagnóstico não têm chamadores internos.
Foram excluídas do JAR e também recusam registro em ambiente de produção. Seus
fontes permanecem disponíveis para execução pela IDE. Fixtures continuam em
`src/test` e não entram no artefato público.

Logging dos runs mudou de DEBUG para INFO. Mensagens existentes de erro de
persistência/Recall e de rejeição de identidade foram mantidas; não foi encontrado
logging de combate/equipamentos a cada tick. Os logs de teste ainda mostram avisos
vanilla de ambiguidades de comandos e do modo offline do servidor isolado.

A descrição antiga dos metadados, que mencionava apenas Baby, foi atualizada.
A tarefa `releaseJarChecks` verifica metadados, quatro texturas, traduções e
ausência das classes de diagnóstico/fixtures e arquivos Blockbench no artefato.
Nenhuma publicação, commit ou push foi realizado.

Comandos de reprodução:

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat runPhase611Server -Pphase611LiveChecks -PsurvivalLiveChecks --console=plain
.\gradlew.bat runPhase611Server -Pphase611LiveChecks -PsurvivalLiveChecks -PrestartLiveChecks --console=plain
```

O segundo comando usa as propriedades já existentes do servidor isolado. O terceiro
exige o snapshot produzido pela execução anterior. Não use essas fixtures em mundos
de jogadores. Sucesso do Gradle ao iniciar um run não basta: confira também a mensagem
de aprovação da fixture e ausência de exceções no log.

Evidências: `build/phase610-build.log`, `build/phase610-native.log` e
`build/phase610-restart.log`. O build incremental passou; artefato local:
`build/libs/soulbound-1.0.0.jar`. Houve correção da tarefa de verificação para o cache
de configuração e atualização do teste que ainda exigia textura Junior 64×64.
O teste inicial de reinício foi ajustado para aceitar o timestamp legítimo do Recall.

## Arquivos desta fase

- `build.gradle` e `src/main/templates/META-INF/neoforge.mods.toml`.
- `client/JuniorCompanionModel.java`, `client/PlenoCompanionModel.java`.
- `companion/CompanionEquipment.java`, `menu/CompanionInventoryMenu.java`.
- `companion/progression/ProgressionCommands.java`.
- `ritual/AltarCommands.java`, `ritual/RuinDiagnostics.java`.
- `assets/soulbound/textures/entity/lupus_junior.png` e `lupus_pleno.png`.
- `tools/generate-warrior-visuals.ps1`.
- Testes: `client/WarriorVisualChecks.java`, `companion/JuniorChecks.java`,
  `companion/Phase69Checks.java`, `companion/Phase69ServerChecks.java`.
- Este documento. Caminhos Java relativos a `src/main/java/br/com/soulbound`
  ou `src/test/java/br/com/soulbound`; assets relativos a `src/main/resources`.

## Homologação manual pendente — 12 cenários

| Grupo / objetivo | Procedimento | Resultado esperado |
| --- | --- | --- |
| Visual: Junior | Inspecionar parado e caminhando, de frente/lado/costas, em GUI normal e ampliada | Silhueta leve, peito articulado, textura legível, sem faces ausentes |
| Visual: Pleno | Inspecionar armadura, quadril, botas e machado de vários ângulos | Silhueta mais larga e madura; lâminas e cabo reconhecíveis |
| Visual: combate | Filmar básicos, combo, investida e ultimate das duas formas | Mãos acompanham a arma; recuperação sem saltos ou clipping grave |
| Visual: regressão | Alternar Baby/Junior/Pleno/God, realizar ritual e sofrer dano | Baby/God preservados; poses e efeitos corretos; equipamentos invisíveis |
| Multiplayer: propriedade | Dois clientes, cada um com seu vínculo, tentam acessar itens e comandos do outro | Acesso cruzado recusado; HUD mostra somente o próprio vínculo |
| Multiplayer: inventário | Equipar, shift-click, usar cursor e reconectar durante movimentos normais | Quantidades e componentes preservados; sem duplicação/perda |
| Estabilidade: atributos | Equipar/tirar armadura e arma várias vezes, transformar em God e retornar | Sem acúmulo de modificadores, cura gratuita ou renovação do escudo |
| Estabilidade: sobrevivência | Receber dano abaixo de 50%, com e sem alimentos; testar Ficar/Passivo e recuperar 65% | Alimentação respeita 5 s; fuga só no modo permitido; retorno ofensivo correto |
| Estabilidade: vida e XP | Combater, curar, investir Vitalidade e alternar formas | HUD reflete HP/XP atuais e talentos; combate e fórmulas preservados |
| Persistência: ciclo completo | Salvar/reabrir, descarregar chunks, usar Recall, morrer e ressuscitar | Mesmos UUIDs; reserva única; inventário, progresso e cooldowns coerentes |
| Pré-release: JAR real | Instalar o JAR em cliente e servidor NeoForge compatíveis, sem fontes de desenvolvimento | Inicialização correta, `/soulbound debug` e diagnósticos de altar ausentes; recover/god disponíveis |
| Pré-release: mundo legado | Abrir cópia de mundo anterior à 6.9 e depois reiniciar o servidor | Vínculo e formas preservados; slots ausentes iniciam vazios; sem substituição destrutiva |

Esta fase está preparada para homologação. Inspeção visual em jogo, dois clientes
reais e instalação do JAR no ambiente final ainda não foram homologados.
