# Fase 6.10.4 — Grimório da Alma e ataque direcionado

Atualização: a [Fase 6.10.4.1](FASE_6_10_4_1_INTEGRACAO.md) integrou inventário e
talentos na própria tela do Grimório. As descrições abaixo sobre abrir menus
separados registram a implementação anterior.

Minecraft 1.21.11 / NeoForge 21.11.48 / Java 21. Sem dependências novas, mudanças
de wrapper, commits, publicação ou push. O pacote original `soulbound_grimorio_pack/`
foi preservado. Somente seus PNGs de componentes e do item foram integrados aos
resources; as imagens de prévia não são utilizadas como interface.

## Item e migração

O registro continua **`soulbound:companion_egg`**. Após a invocação, a projeção
`ACTIVE` usa o modelo `soulbound:soul_grimoire` e o nome Grimório da Alma.
`MISSING` usa o mesmo livro com nome Grimório Inativo; `READY`/`SUMMONING`
continuam ovo e `DEAD` continua Ovo Fragmentado. Os estados bloqueados permitem
consulta informativa, sem autorizar invocação ou comandos físicos.

Essa escolha evita substituir stacks, receitas e credenciais: o mesmo item já
protegido torna-se visualmente o Grimório. Nenhum novo vínculo ou inventário foi
criado. A primeira invocação atualiza a stack usada imediatamente. `/soulbound recover`
recupera a apresentação correspondente ao estado oficial, mantendo o UUID reservado.
Não há receita nova de Grimório nem ID separado para obter outro vínculo.

A migração do Bastão legado é idempotente: a sincronização existente no login,
no uso e periodicamente atualiza `ITEM_MODEL`, nome e tooltip de stacks válidas.
Proprietário/vínculo e progresso permanecem no registro canônico. Credenciais
adulteradas ou de terceiros continuam recusadas. Inventário/cursor usam a
consolidação de cópias já existente; não há nova rotina de exclusão de itens.

Containers fechados não são varridos globalmente: uma cópia antiga pode conservar
a aparência até ser observada. Slots externos seguem a recuperação já existente,
que persiste intenção de entrega antes de remover/restituir o item. A migração
visual não confere autorização a cópias em containers e não cria um segundo
companheiro. Comandos administrativos/rollback seletivo não recebem garantia absoluta.

## Hub e sincronização

Clique direito no Grimório abre o hub sem Shift. O menu servidor `SoulCommandMenu`
foi reutilizado, com botões existentes e IDs adicionais para cancelar foco/usar
habilidade. O identificador de menu registrado não mudou.

| Aba | Dados e ações |
| --- | --- |
| Visão Geral | Nome, espécie, forma, nível/XP, HP quando conhecido, modo, estado, pontos e seleção de formas |
| Inventário | Abre o menu canônico de 9 slots existente; exige dono vivo, não espectador, companheiro vivo a até 8 blocos, mesma dimensão e fora de ritual |
| Talentos | Abre a árvore existente, com seus pontos, ramos, pré-requisitos e investimento no servidor |
| Habilidades | Alerta, Instinto Mineral, habilidade da forma ativa, cooldowns de investida/ruptura quando aplicáveis e Recall/God |
| Status | Base e valor efetivo de atributos nativos, modificadores presentes, bônus de talentos e regeneração calculada pelas regras existentes |
| Comandos | Seguir/Combate, Ficar, Passivo, Recall, Ressonância e cancelamento do alvo |
| Vínculo | Dono/UUIDs, arquétipo, progresso evolutivo, maior estágio, formas desbloqueadas e contagem de ressurreições quando registrada |

Datas de criação e cronologia detalhada não existentes são identificadas como
indisponíveis. HP de entidade não carregada não é estimado. God consulta sua
barreira canônica. Um ícone de pata é usado como fallback de retrato; não se
instancia entidade fictícia no cliente. Talentos continuam com as regras atuais
de investimento; leitura não dispensa validações de mutações.

Painéis e botões usam nine-slice com dimensões reais das texturas e cantos fixos.
A sidebar possui seleção, ícones e tooltips. O painel ajusta tamanho ao viewport;
texto longo/UUIDs são quebrados em linhas e paginados. Teclas **1–7** trocam de aba;
setas esquerda/direita mudam páginas. Inventário e talentos têm retorno ao Grimório,
revalidando o menu atual e o item de vínculo em uma das mãos.

`GrimoireSnapshot` produz uma projeção somente de leitura. O servidor compara
projeções a cada 10 ticks durante o menu aberto e envia apenas mudanças por
`GrimoirePayload`, ligado ao ID do menu. O cliente descarta dados de outro menu.
UUIDs, inventário e progressão não recebem fonte autoritativa nova. O HUD anterior
mantém seu próprio fluxo e frequência. Botões enviam intenções/IDs, sem saldos,
atributos ou UUIDs de dono fornecidos pelo cliente.

## Ataque direcionado

- **G**: raycast de entidade viva e envio apenas de seu ID. Repetir sobre o mesmo
  alvo cancela. **V**: cancelar sem precisar apontar novamente. Ambos são remapeáveis.
- **R** continua reservado à ultimate. Conflitos com esse atalho e entre os
  controles novos são detectados; o usuário recebe orientação de remapeamento.
  A ajuda do hub mostra as teclas remapeadas.
- Config SERVER `targetCommandRange`: padrão **24 blocos**, intervalo permitido 4–48.
  `targetCommandTimeoutTicks`: padrão **600 ticks / 30 s**, intervalo 40–2400.
- O servidor confere jogador vivo, vínculo canônico, companheiro vivo/aptidão,
  dimensão, alcance, visibilidade e interseção do olhar com o bounding box do alvo.
  Um ID recebido não autoriza um alvo distante ou atrás de parede.
- A perseguição tem raio máximo de **duas vezes** o alcance configurado ao redor
  do dono e timeout fixo por ordem. O foco não renova o prazo a cada tick.
- O goal direcionado tem prioridade superior à aquisição automática e inferior à
  sobrevivência/ritual. Reutiliza navegação e controladores atuais, sem teleporte.
  O dano continua exigindo linha de visão do companheiro.
- Baby nunca recebe ordem ofensiva. Proprietário, próprio companheiro, outros
  companheiros, guardiões de prova, animais domesticados, aliados, espectadores
  e entidades invulneráveis são recusados.

`TargetCommandActor` é o adaptador mínimo; `TargetCommandService` não depende do
controlador Lupus. O foco armazena somente UUIDs, dimensão, forma e prazo em memória.
A preferência canônica de modo e o ponto de Ficar não são alterados: enquanto o
foco vale, combate é temporariamente permitido em Ficar/Passivo. Ao encerrar,
volta a valer a preferência já armazenada, inclusive após reiniciar o servidor.

Cancelamento, morte/remoção, troca de dimensão/forma, vínculo inválido, dono ausente,
distância excessiva, ritual/Recall ou estado defensivo encerram o foco. Comandos
posteriores de modo prevalecem; Recall conserva sua regra anterior de retornar
a Seguir. Não há restauração de alvo salvo após reinício ou carregamento de chunk.

### PvP, passivos e proteção

A aquisição automática continua restrita a hostis. A política compartilhada de
dano só abre exceção para **a entidade explicitamente designada**, com autorização
revalidada. Áreas, combos e ultimates não ganham permissão de atingir outros
pacifistas/jogadores. O caso de um jogador inimigo é permitido somente com PvP
habilitado na dimensão e `ServerPlayer.canHarmPlayer`, além da recusa de aliados.

Na versão instalada, PvP é consultado em `ServerLevel.isPvpAllowed()`; não foi
presumida uma API de configuração global de versões anteriores. Ordens explícitas
publicam `AttackEntityEvent` do proprietário e passam pelo dano vanilla. A fonte
conserva o tipo de dano e o companheiro como atacante direto/portador da arma,
identificando o proprietário como causador para as verificações de PvP/proteção.
Não há bypass de eventos ou dano aplicado diretamente ao HP.

Mods/plugins podem ter políticas específicas; integração com todos eles não foi
homologada. Efeitos de retaliação e atribuição de abate podem observar o proprietário
como responsável pela ordem explícita. Ataques automáticos conservam a fonte anterior.
Essa fase não adiciona XP por matar jogadores/pacíficos: os filtros de progressão
existentes continuam restringindo as recompensas de combate.

Retícula discreta e nome do foco são enviados somente ao dono. Falhas apresentam
mensagens traduzidas: alvo inválido/protegido, companheiro indisponível, alcance,
visibilidade, PvP desabilitado ou conflito de teclas. A retícula sai ao limpar foco
e não substitui a HUD de combate.

## Arquivos

Novos Java principais, relativos a `src/main/java/br/com/soulbound/`:

- `companion/GrimoireSnapshot.java`.
- `companion/target/TargetCommandActor.java`, `TargetCommandService.java`.
- `network/GrimoirePayload.java`, `GrimoireOpenPayload.java`,
  `TargetCommandPayload.java`, `TargetStatusPayload.java`.
- `client/GrimoireScreen.java`, `GrimoireTextures.java`, `GrimoireButton.java`,
  `TargetControls.java`.

Modificados: `Config`, entradas comum/cliente, `SoulStaffCommands`,
`CompanionFormService`, `JuniorCombat`, `LupusCompanionEntity`, `JuniorCombatGoal`,
`PlenoCombatController`, `CompanionEggItem`, `SoulItemForm`, `SoulCommandMenu`,
`TalentMenu`, `CompanionInventoryMenu`, telas de inventário/talentos e `JuniorControls`.

Recursos: definição/modelo JSON de `soul_grimoire`, PNG do item, componentes PNG em
`textures/gui/grimoire/`, traduções PT-BR/EN-US. Build: verificação dos novos recursos
no JAR. Documentos: este relatório, README e manual do jogador.

Testes: novos `client/GrimoireChecks` e `companion/GrimoireServerChecks`, integrados
às verificações e à fixture de sobrevivência existentes. `BabyVisualChecks` e
`Phase69ServerChecks` receberam apenas as chamadas/etapas correspondentes.

## Evidências e limitações

- Build: `./gradlew.bat build --console=plain`; suíte anterior preservada e
  **69 verificações novas de recursos/layout**. Retângulos foram conferidos em
  viewports 320×180, 320×240, 426×240, 640×360, 960×540 e 854×480.
- Dedicado isolado: **43 verificações novas de integração**, mais **23 de itens**
  e **54 de sobrevivência/equipamento/God** anteriores, com encerramento e save reais.
- Casos novos incluem login/primeira invocação, recuperação pelo comando real,
  migração idempotente, terceiros, inventário remoto/próximo, bloqueio de botão
  forjado, informações de morto, roundtrip do payload, precedência do goal,
  alvo passivo explícito, recusa de passivo colateral, proteção via evento,
  fonte/arma reais, modo preservado, cancelamento, ID/range inválidos,
  PvP e equipe via política vanilla e timeout por ticks reais.
- Reinício real do mesmo mundo isolado: duas verificações de snapshot canônico/
  cooldown do Recall; timestamps de localização podem avançar legitimamente.
- PvP habilitado foi testado com `ServerPlayer` nativo sem conexão gráfica:
  `FakePlayer.canHarmPlayer` retorna falso por definição. Isso não homologa PvP
  real em rede, dano entre dois clientes nem plugins externos.

Logs locais: `build/grimoire-build.log`, `build/grimoire-native.log`,
`build/grimoire-restart.log`. Confira aprovação das fixtures e ausência de exceções,
além do exit code do Gradle. Artefato: **`build/libs/soulbound-1.0.0.jar`**.
Cliente e servidor devem usar o mesmo JAR com os novos payloads.

O teste de layout é geométrico, sem abrir o jogo. Renderização, escala efetiva de
fontes, tooltips, navegação por teclado e retícula precisam de inspeção no cliente.
Não foram homologados dois clientes reais, desconexão durante transferências,
dimensões customizadas, rollback seletivo ou falhas físicas de armazenamento.
As limitações transacionais já documentadas de playerdata/SavedData/chunks continuam.

## Homologação manual — até 12 cenários

| Grupo / objetivo | Procedimento | Resultado esperado |
| --- | --- | --- |
| Grimório: nascimento | Entrar com jogador novo, invocar pelo ovo e reconectar | Mesmo vínculo/entidade; um item com visual de Grimório |
| Grimório: migração/recuperação | Abrir cópia de mundo com Bastão antigo; remover só o item e usar recover | Migração sem trocar UUIDs/progresso; recuperação não duplica companheiro |
| Grimório: terceiros/containers | Dois clientes tentam usar cópia alheia, baú, drop e Criativo | Sem controle cruzado ou reserva extra; proteções anteriores preservadas |
| UI: escalas | Abrir todas as abas em escalas 2, 3 e 4, inclusive janela pequena; usar 1–7 e páginas | Abas/botões acessíveis, texto legível e bordas nine-slice sem deformação |
| UI: distância e retorno | Abrir hub longe; tentar inventário; aproximar até 8 blocos e voltar do inventário/talentos | Informação remota; operação física negada longe e permitida nas condições válidas |
| UI: dados reais | Receber dano, curar, trocar forma/equipamento e investir talento; abrir estado morto/descarregado | HP/atributos/cooldowns atualizados; sem atributos ou progresso duplicados; incógnitas explícitas |
| Ataque: hostil e passivo | G mirando cada tipo com Junior/Pleno/God; observar companheiro Baby | Ordem aceita nas formas ofensivas; Baby recusa; sem teleporte/atravessar paredes |
| Ataque: preferência | Ordenar em Ficar e Passivo; repetir G, usar V e botão cancelar | Ordem temporária; modo e ponto de Ficar preservados, sem foco residual |
| Ataque: PvP/equipes | Dois clientes, PvP da dimensão off/on e depois mesma equipe | Recusa com PvP off/aliado; alvo legal somente por ordem explícita |
| Ataque: áreas/proteção | Passivos/jogadores não selecionados junto de alvo; usar combo/ultimate; testar proteção instalada | Sem dano colateral não autorizado; proteção pode cancelar dano |
| Ataque: invalidação/defesa | Matar/remover alvo, sair do alcance, trocar dimensão, Recall, baixar HP a estado defensivo e aguardar timeout | Foco limpo e preferência restabelecida; sobrevivência e cooldowns preservados |
| Reinício e rede | Encerrar com ordem ativa; reiniciar e conectar dois clientes; conferir HUD/retícula e atalhos remapeados | Nenhum alvo restaurado; dados canônicos preservados; feedback somente ao dono |

Fase implementada e com verificações automatizadas executadas. Homologação visual
e multiplayer real permanece pendente; FakePlayers não substituem esses testes.
