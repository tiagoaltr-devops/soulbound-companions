# Fase 6 — Altar da Alma

> **Atualização 6.1.1:** a política de ruínas abaixo é histórica e foi substituída.
> Consulte [Ruínas e Ressonância Ancestral](FASE_6_1_1_RESSONANCIA.md) para a geração
> atual, índice de origem natural, comando Localizar Ruína, testes e limitações.
> As seções de Forja, rituais e evolução continuam aplicáveis.

## Resultado e escopo

Implementação das quatro subfases integrada a Minecraft **1.21.11**, NeoForge
**21.11.48** e Java **21**, sem mudar dependências ou wrapper. O nascimento inicial
continua gratuito e o Soul Recall mantém seu cooldown e custo zero de XP.

O build passou e houve inicialização real do cliente e do servidor dedicado.
Também foram encontradas três ruínas naturais por comandos no servidor de teste.
Isso **não homologa** provas, interfaces, crafting, rituais ou multiplayer com
jogadores. Esses testes permanecem no [checklist](FASE_6_CHECKLIST.md).

## Arquitetura e persistência

`soulbound_identity.dat` continua sendo a autoridade sobre proprietário, vínculo,
UUID reservado, espécie, estágio, modo, progresso, cooldown God e estado da entidade.
Os métodos de cópia preservam os campos anteriores, incluindo o estado do Recall.

`soulbound_altar.dat`, no armazenamento do Overworld, contém:

- Ruínas: UUID, dimensão, centro, chunk de vila associado, conclusões, participantes
  que já venceram, cooldowns e tentativa em andamento.
- Forjas: UUID, proprietário por UUID, posição/dimensão, presença do bloco,
  estado, desgaste e armazenamento do núcleo.
- Tokens únicos de Centelha/Core: tipo, destinatário de entrega, emissão/consumo e
  diário de aplicação no vínculo. O destinatário não restringe a negociação de Cores.
- Capacidade God permanente, prazo da forma temporária e histórico de ressurreições.
- Oferendas, reservas de rituais e transferências de inventário pendentes.

O codec preserva snapshots completos de `ItemStack`, incluindo componentes, através
do contexto de registros da versão instalada. O arquivo usa o envelope nativo de
`SavedDataType`; seu campo `data` contém o estado serializado em JSON. Dados inválidos
não provocam substituição silenciosa de um arquivo existente por um registro vazio.
Gravações críticas usam `saveAndJoin` e releitura para conferir o snapshot.

Os espaços dos altares e da Forja ficam no registro global, **não em BlockEntities**.
Essa decisão permite consumir oferendas, criar a reserva e registrar o resultado em
uma única gravação. Não há capabilities de automação, acesso por hopper ou renderização
do item sobre o bloco nesta implementação. Blocos em outra posição não recebem uma
identidade por copiar apenas sua aparência.

## 6.1 — Ruínas Ancestrais

**Histórico da entrega original da Fase 6.** Desde a revisão 6.1.1, a separação
padrão é de 1.000 blocos, a sala integra decorações de vila e o índice dispensa
interação inicial. A descrição desta seção documenta a estratégia substituída.

`RuinPlacement` seleciona um candidato determinístico do placement real
`minecraft:villages` por célula de `max(512, 8 × villageSpacing)` chunks. Não procura
vilas carregando chunks. `RuinStructure` e sua `StructurePiece` entram no pipeline
nativo de estruturas; a peça é processada na geração de novos chunks.

A construção acontece somente se o chunk de origem tiver um `StructureStart`
válido de vila e o centro estiver a pelo menos 2.000 blocos horizontais do spawn
vigente naquele momento. O chunk de origem fica dentro da dependência nativa de
`STRUCTURE_STARTS` da etapa `FEATURES`, conferida no código da versão instalada.

A sala fica 32 blocos a leste do centro do chunk de origem. São 13×13 blocos de
pedra musgosa/rachada, quatro colunas com lanternas de alma e um pedestal central.
Não existe Forja natural. O pedestal é inquebrável pelos meios comuns.

O deslocamento do candidato até o centro de sua célula é menor que `villageSpacing`.
Assim, células vizinhas têm separação mínima superior a 3.000 blocos entre centros
(limite conservador de 6.144 blocos com a política atual). Candidatos de células
distintas não podem representar a mesma vila. O UUID é salvo na peça; o registro
global da ruína é materializado na primeira interação válida com seu pedestal.

**Limites da estratégia:** associação por chunk de origem de vila, e não adição a
um pool jigsaw da vila. A sala pode ficar na borda da vila e sua integração visual
depende do terreno e das peças vizinhas. O placement próprio não oferece integração
com o algoritmo vanilla de `/locate`. Um start pode existir sem sala quando a vila,
o bioma ou a distância recusam a construção. O comando de candidatos é diagnóstico,
não confirmação de ruína. Alterações externas de worldgen/spawn podem mudar as
condições futuras; chunks já gerados não são reformados.

## 6.2 — Provação das Três Almas

Interaja com o pedestal central, preferencialmente com a mão vazia, e abra o menu.
O botão de início valida servidor, ruína, conclusão anterior, cooldown, tentativa
ativa e vínculo `ACTIVE`. Outro jogador não espectador a até 48 blocos horizontais
impede o início. A mesma verificação acontece a cada tick durante a tentativa.

1. **Sabedoria:** pistas de sete símbolos; sequências de 3, 4, 5, 6 e no máximo 7
   símbolos conforme as conclusões da ruína. Cada escolha correta toca uma nota.
   Uma escolha errada cancela a tentativa.
2. **Coragem:** entidade própria `soulbound:spirit_guardian`, com geometria/textura
   vanilla de zumbi **provisória**, brilho e nome explícito. De 2 a 12 guardiões no
   total; no máximo 4 simultâneos por padrão. Vida e dano têm limites. Não convertem
   na água, não transformam aldeões, não pegam itens e não concedem loot/XP. Só
   podem causar dano ao desafiante.
3. **Vínculo:** exige a entidade real canônica no mesmo mundo, a até 3 blocos do
   ponto central. Canalização de 5 a 25 segundos conforme o nível, com partículas.
   Afastar o companheiro zera a canalização; perder sua validade cancela a prova.

Morte, saída do perímetro, desconexão, intrusão ou timeout de dez minutos cancelam
a tentativa. Guardiões carregados são descartados; os descarregados têm sua entrada
recusada quando voltarem. Reiniciar cancela tentativas salvas e aplica o cooldown,
sem manter a ruína ocupada. Não há cobrança de materiais.

Somente o sucesso adiciona o UUID à lista de vencedores e incrementa a dificuldade.
A Centelha é um token único, emitido na mesma gravação da conclusão. Inventário
cheio deixa a entrega pendente. Cada UUID pode vencer uma vez em cada ruína.

## 6.3 — Forja e propriedade

Receita especial na bancada, em qualquer disposição: **1 Centelha válida,
6 obsidianas choronas, 1 lingote de netherita e 1 diamante**, ocupando nove espaços.
O resultado carrega o identificador da Centelha. Sua primeira colocação a reivindica
no registro e cria a Forja; cópias não permitem uma segunda colocação simultânea.
Uma Forja obtida com `/give` sem token não cria uma identidade independente.

Receitas dos três componentes e da Essência de Reativação estão nos datapacks.
O multibloco, no mesmo nível horizontal, aceita quatro rotações:

```text
..O..
..P..
OPFPO
..P..
..X..
```

`O`: altar de oferenda; `P`: pedra ritualística; `F`: Forja; `X`: plataforma de XP.
Os pontos não exigem componentes. A plataforma define a frente da estrutura.
Na orientação desenhada, as oferendas são verificadas na ordem norte/oeste/leste;
nas outras rotações, oposto à plataforma/esquerda/direita olhando para a Forja.

Clique nos altares para depositar a stack da mão principal; clique novamente para
retirar. A Forja abre o menu; o botão de núcleo deposita uma unidade da mão ou retira
o conteúdo. Os nomes dos materiais são enviados pelo servidor e respeitam os JSONs
de oferendas. Inserir/retirar um núcleo reabre o menu para atualizar seu conteúdo.

A ativação inicial consome **2 diamantes / 8 fragmentos de ametista / 1 fragmento
de eco**, nos três altares nessa ordem. Não falha e define como dono quem confirmou
a ativação. O dono não é definido ao fabricar ou colocar o bloco.

Somente o dono quebra e recupera uma Forja ativada; precisa de espaço no inventário,
núcleo retirado e nenhum ritual ativo. O item preserva UUID e desgaste e retorna
ao estado `INACTIVE`. Não há recuperação comum de uma Forja ainda sem dono: ative-a
antes de movê-la. Explosões, pistões e destruição pelos bosses vanilla são bloqueados.
Remoção administrativa ou de outros mods não recebe garantia absoluta.

Reativar exige as mesmas três oferendas mais uma Essência de Reativação no espaço
do núcleo. Os riscos por tentativa são 1%, 20%, 40%, 60%, 80% e permanecem em 80%.
Entre falhas, 75% deixam `INACTIVE` e 25% deixam `DESTROYED`, sem explosão. Tentativas
fracassadas também incrementam o contador. O sorteio, desgaste, consumo e resultado
ficam na mesma gravação do registro. Um identificador destruído nunca volta a ser
colocável. O menu mostra risco total de falha e risco total de destruição.

## 6.4 — Rituais e aplicação dos Cores

| Core bruto | Base de fabricação | Oferendas padrão, na ordem dos altares | XP dono/visitante |
| --- | --- | --- | --- |
| Despertar | 4 obsidianas + diamante | 2 diamantes / 8 ametistas / 16 lápis-lazúlis | 30/35 |
| Ascensão | bloco de netherita + eco + diamante | lingote de netherita / 2 ecos / bloco de diamante | 45/50 |
| Divino | essência dracônica + estrela do Nether + ametista | essência dracônica / estrela do Nether / 4 cristais do End | 60/65 |
| Restauração | lírio-do-vale + 4 diamantes | 4 diamantes / lírio-do-vale / lágrima de ghast | 25/30 |

Mortes efetivas de Ender Dragons concedem duas Essências Dracônicas. Novos dragões
invocados pelo ritual vanilla têm novos UUIDs e podem conceder novamente. Descarregar
um dragão não concede a essência. O UUID da morte é registrado antes do drop, que
fica deslocado do centro do dragão para evitar a saída do End.

Com a Forja ativa, insira o Core bruto e as oferendas, fique sobre a plataforma e
confirme o sacrifício. O servidor valida níveis, estado/estágio do vínculo, integridade,
posição e exclusividade da Forja. Deduz **níveis diretamente** e canaliza por 15 segundos.
Um visitante usa seu próprio vínculo e paga cinco níveis adicionais.

As oferendas e o Core passam para uma reserva persistente. Sair da plataforma,
morrer, desconectar, mudar as condições do vínculo ou invalidar o multibloco devolve
os materiais aos mesmos espaços canônicos. XP não é devolvido. Não há Core Carregado
nem mudança de estágio no cancelamento. Reiniciar recupera reservas interrompidas.

No sucesso, nasce um token de Core Carregado; ele pode ser negociado. Tipo e carga
são conferidos no registro, não no nome, textura ou lore. Combine o item de vínculo
e o Core na **Crafting Table**. O hook nativo `onCraftedBy`, chamado antes de resolver
os restos da receita na versão instalada, valida proprietário, vínculo, estado,
estágio e token. Um Core recusado retorna pelos restos da receita; um consumido
canonicamente não retorna. A grade pessoal da bancada é a exceção à proteção de
transferência do Bastão; baús e outros containers continuam protegidos.

Despertar aplica `JUNIOR`, Ascensão aplica `PLENO` e Divino desbloqueia God uma única
vez. Um diário persistente permite reaplicar uma evolução autorizada interrompida,
sem consumir novamente o token. Vida máxima: Baby 12, Junior 24, Pleno 40, God 80.
Velocidade aérea: 0,12/0,14/0,16/0,20. Escala: 1/1,2/1,45/1,7. Mudanças de vida
preservam a proporção atual. Todos permanecem passivos nesta implementação mínima.

Junior, Pleno e God reutilizam o modelo/textura Baby com escala vanilla sincronizada;
**não são visuais finais**. O botão God do Bastão ou `/soulbound god` ativa a capacidade
desbloqueada de um Pleno `ACTIVE`. Dura 1.200 ticks, com cooldown de 18.000 ticks a
partir da ativação. Ambos usam o relógio persistente do servidor, não horário civil.

Restauração só aceita `DEAD` com ressurreição necessária. Verifica que o UUID reservado
não está carregado em nenhuma dimensão, encontra uma posição segura, reserva
`SUMMONING` antes da inserção e reutiliza o mesmo owner/bond/companion UUID. Volta
a Baby, Follow, progresso zero e capacidade/cooldown God reiniciados; conserva o
histórico de ressurreições. `MISSING` nunca autoriza recriação automática.

## Transações e limites de recuperação

Transferências entre mão/inventário e armazenamento do altar usam intenções no
registro e recibos no `NeoForgeData` do jogador. A gravação do jogador é relida antes
de concluir a transferência. Reconectar resolve intenções pendentes. Retirada sem
espaço aguarda; não joga itens no chão. A emissão de recompensas usa o mesmo princípio
de recibo para não repetir uma entrega já salva no jogador.

Registros, playerdata, blocos e entidades **não constituem uma única transação física**.
Uma interrupção na estreita janela entre salvar o débito de XP e salvar a reserva
pode deixar o XP pago com os materiais ainda nos altares: é uma tentativa interrompida,
sem Core e sem devolução de XP. Falhas de armazenamento são recusadas, não ignoradas.

Como no nascimento/Recall anteriores, crash entre reserva de ressurreição e gravação
do chunk pode deixar `SUMMONING`/`ACTIVE` sem uma entidade recuperável automaticamente.
O sistema privilegia bloquear duplicação e exige revisão administrativa desse caso.
Uma Forja retirada durante crash também pode deixar um bloco antigo inerte até revisão.
Não há garantia contra rollback seletivo de arquivos, editores, comandos administrativos
ou mods que contornem os eventos. Cópias físicas de um token não concedem múltiplos
usos canônicos. Não foram executados testes de corte abrupto do processo/armazenamento.

## Configuração e recursos

Config SERVER: custos dos quatro rituais, sobretaxa, duração, cooldown das provas,
máximo simultâneo de guardiões, cinco patamares de falha e probabilidade crítica.
Na revisão 6.1.1 foram acrescentados raio/cooldown da Ressonância; as distâncias de
worldgen passaram a ser parâmetros de datapack. Os limites totais da prova continuam
fixos. Não há configuração de combate avançado.

`data/soulbound/recipe` contém receitas e `data/soulbound/soul_ritual` contém as três
oferendas de ativação e de cada ritual. `/reload` respeita substituições; valores
inválidos são recusados. A receita especial de Forja e a combinação Bastão/Core
preservam credenciais no serializer; seus padrões internos ainda são definidos em
Java. As demais receitas e as oferendas são substituíveis por datapacks.

Há 13 PNGs originais de 16×16, gerados por
`tools/generate-phase6-assets.ps1`, sem copiar arte de terceiros: cinco blocos e
oito materiais/Cores. Os componentes possuem modelos de item e bloco da versão
atual, loots para os componentes recuperáveis e traduções pt_BR/en_US.

## Arquivos principais

Criados em `src/main/java/br/com/soulbound/ritual/`:

- `AltarPolicy`, `AltarSavedData`, `AltarTokens`, `AltarInventory`, `InventoryTransfers`.
- `RuinPlacement`, `RuinStructure`, `AncestralBlock`, `TrialService`, `TrialEngine`, `TrialMenu`.
- `ForgeBlock`, `ForgeBlockItem`, `OfferingBlock`, `ForgeService`, `ForgeMenu`.
- `RitualDefinitions`, `RitualService`, `AltarCraftingRecipe`, `CompanionEvolution`, `AltarCommands`.

Também criados: `registry/ModAltar`, `entity/SpiritGuardianEntity`,
`client/SpiritGuardianRenderer`, `client/TrialScreen`, `client/ForgeScreen`,
`src/test/java/br/com/soulbound/ritual/AltarChecks.java`, recursos de worldgen/receitas/
oferendas/modelos/texturas/loot/tags e ferramentas de geração e teste em `tools/`.

Modificados nesta fase: `build.gradle`, `README.md`, `Config`, `SoulboundCompanions`,
`SoulboundCompanionsClient`, `companion/EvolutionStage`, `SoulStaffAccess`,
`identity/SoulboundRecord`, `identity/SoulboundIdentity`, `entity/LupusCompanionEntity`,
`item/CompanionEggItem`, `menu/SoulCommandMenu`, `client/SoulCommandScreen`,
`registry/ModItems`, `registry/ModEntities`, `registry/ModMenus` e arquivos de tradução.
As alterações anteriores já existentes no workspace foram preservadas.

## Builds e testes realmente executados

| Marco | Evidência |
| --- | --- |
| Inicial | `clean build`: sucesso; 134 verificações anteriores |
| 6.1, código de geração | Build com sucesso; recursos integrados no checkpoint seguinte |
| 6.2 | `clean build`: sucesso; 22 verificações iniciais do altar e 134 anteriores |
| 6.3 | Chamada incompatível de profile cache corrigida; build passou. Uma tentativa de `clean` encontrou JARs bloqueados pelo ambiente; builds limpos posteriores passaram |
| 6.4 / integração | `clean build`: sucesso; **221 verificações** — 87 altar + 24 identidade + 20 visual + 41 Bastão + 49 Recall |
| Dedicado | Mundo novo isolado em `runs/phase6-server/phase6-validation`; chegou a `Done`, foi salvo e encerrado por comando |
| Worldgen nativo | Seed `20261031`: nove candidatos consultados; três pedestais encontrados por `execute if block` |
| Reinício | Os três chunks foram recarregados e seus pedestais passaram novamente em `execute if block` |
| Cliente | Carregou renderers/modelos e atlas de blocos/itens; encerrado após a checagem. Nenhuma partida foi homologada |

Pedestais observados no teste isolado: `(-4264,93,-4248)`, `(-4120,70,3896)` e
`(4168,84,-4008)`. A menor distância horizontal entre esses centros supera 8.000
blocos. Isso é uma amostra real, não uma inspeção exaustiva de todas as seeds/vilas.

As 87 verificações cobrem políticas, quatro rotações, patamares de risco e fronteiras,
negação de estados, preservação dos UUIDs/Recall na evolução, roundtrip de registro,
NBT comprimido em disco, diário de aplicação, reservas, transferências e PNGs/JSONs.
Não simulam rede, login de múltiplos jogadores, inventários completos em partida,
crafting por cliques, falhas físicas, combate ou experiência visual.

## Homologação e comandos

```powershell
.\gradlew.bat clean build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

O `runPhase6Server` adicional usa diretório isolado e porta 25576. Os testes locais
utilizaram RCON restrito a loopback; sua senha fica somente no diretório ignorado
de execução, nunca nos fontes. Não reutilize a configuração de teste offline em
um servidor público.

Comandos de diagnóstico:

```text
/give @s soulbound:companion_egg
/give @s soulbound:awakening_core
/give @s soulbound:offering_altar 3
/give @s soulbound:ritual_stone 4
/give @s soulbound:xp_platform
/soulbound recover
/soulbound altar inspect
/soulbound altar candidate 0 0
/soulbound god
/data get entity @e[type=soulbound:lupus_companion,sort=nearest,limit=1]
```

`candidate` exige permissão de operador e recebe índices de células, não coordenadas
de bloco. Não carrega chunks. Obter uma Centelha/Core Carregado vazio pelo Criativo
ou `/give` não cria o token oficial; teste sua obtenção pelo fluxo real. Para iniciar
a homologação, siga a ordem e os cenários do checklist, em cópia ou mundo novo.
