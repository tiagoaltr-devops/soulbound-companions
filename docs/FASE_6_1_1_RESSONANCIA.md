# Fase 6.1.1 — Ruínas e Ressonância Ancestral

## Implementação

Minecraft 1.21.11, NeoForge 21.11.48, Java 21 e dependências preservados.
Esta revisão substitui a política de geração da Fase 6.1. Evolução, Forja, rituais,
regras de vínculo e nascimento gratuito permanecem. O Recall continua sem custo de XP.

A raridade anterior vinha de uma candidata por célula de pelo menos 512 chunks.
Agora cada candidata vanilla de vila passa por uma seleção determinística de prioridade
local, considerando biomas compatíveis com vilas. A aproximação de bioma usa o sampler
nativo ao nível do mar; não é uma busca por chunks. A existência da vila é conferida
separadamente antes de construir a ruína.

As prioridades impedem duas selecionadas próximas, independentemente da ordem de
geração dos chunks. A separação considera **1.000 blocos + 256 de margem**: cada sala
pode se deslocar até 128 blocos do centro do chunk de origem. Portanto a separação
entre centros reais permanece pelo menos 1.000 blocos. Há também uma reserva
conservadora ao redor das possíveis posições antigas, para proteger ruínas da Fase 6.
A barreira do spawn continua sendo de 2.000 blocos horizontais, verificada ao construir.

## Integração com vilas

Foram inspecionadas as APIs instaladas de `JigsawPlacement`, `PoolElementStructurePiece`,
`SinglePoolElement`, `StructureStart` e `StructureTemplate`. Não são substituídos pools
ou arquivos vanilla globalmente. Somente uma vila selecionada pode receber uma ruína.

Na thread de worldgen, o mod reconstrói o plano jigsaw determinístico da variante
vanilla, sem carregar chunks. Prioriza o template de fonte e usa praças de encontro
como alternativa decorativa. A vila real precisa existir no chunk de origem e ser
da mesma variante antes de a peça construir seus blocos.

A peça da ruína executa em `top_layer_modification`, depois da etapa das vilas.
O piso fica na altura planejada da praça. A sala ocupa 13×13 ou 7×7, conforme o espaço
disponível, dentro da peça decorativa. Pedra/terracota/arenito acompanham a variante.
Sinos, camas, estações de trabalho e blocos de suporte são preservados. Interseções
com casas e outras peças protegidas recusam o local. Pontos de criação de aldeões,
animais e vegetação decorativa não são tratados como casas; suas entidades não são
removidas pela construção.

Isso é uma sobreposição seletiva de decoração planejada, **não uma alteração dos pools
jigsaw**. Peças compactas ou obstruídas podem continuar sem ruína. Uma vila não é
sacrificada para cumprir uma taxa artificial de geração. Não há Forja natural.
Os guardiões da prova passaram a procurar posições livres em raios limitados, para
acomodar as praças menores; dificuldade, recompensa e regras da prova foram mantidas.

## Origem, integridade e índice

As peças persistem UUID, versão, posição da vila e parâmetros de geração. O novo
`AncestralPedestalBlockEntity` guarda `NaturalRuin`, `NaturalPosition` e o chunk de vila.
Um pedestal criado por comando nasce com a identificação natural vazia. A construção
natural exige `WorldGenRegion`; `/place` não recebe a mesma credencial de origem.

O índice fica no `AltarSavedData`, com `origin=NATURAL_WORLDGEN`, integridade,
dimensão/centro, associação com vila, versão e última verificação. A fila de worldgen
publica evidências imutáveis; apenas a thread do servidor altera o SavedData. O bloco
real e sua marca precisam conferir antes do registro. **Não é necessário clicar no
pedestal nem concluir a prova para indexá-lo.**

Para descobrir áreas geradas e descarregadas, utiliza-se `chunkScanner().scanChunk`
de forma assíncrona. A verificação exige:

1. Start nativo de ruína e start válido de vila no chunk de origem.
2. Identificador e centro compatíveis com a peça persistida.
3. Chunk de destino com geração suficiente e bloco de pedestal no voxel exato,
   conferido na paleta/armazenamento de bits da seção.
4. Marca natural da BlockEntity para as novas peças.

Um start sem sala, uma Forja ou um pedestal artificial não bastam. Ruínas antigas
sem BlockEntity podem ser migradas pela combinação start nativo/vila/bloco físico
no centro antigo. Seus UUIDs, conclusões, cooldowns e demais dados são preservados.
Registros antigos sem os campos novos começam como `LEGACY_UNVERIFIED`, não como
natural automaticamente. A integridade permanentemente invalidada não é reativada
pela leitura de outra cópia do bloco.

## Ressonância Ancestral

No menu do Bastão, **Localizar Ruína** está disponível para o Lupus Baby e os estágios
seguintes, incluindo a transformação God. O servidor valida item, proprietário,
owner/bond/companion UUID, estado ACTIVE, entidade canônica próxima, dimensão,
cooldown e conflito com Recall. A indicação é horizontal e funciona no Overworld.

A busca consulta o índice e comprova fontes geradas ainda não registradas por leitura
assíncrona de disco. Procura a mais próxima comprovada dentro da área consultada,
ignorando as concluídas pelo UUID do jogador e as permanentemente inválidas. Ocupação
da prova e cooldown da ruína não a excluem, pois ela pode voltar a ser utilizada.

Limites: raio padrão de 4.096 blocos, 256 starts por consulta, quatro consultas
simultâneas, até duas novas verificações por tick e duas pendentes por consulta.
O tempo máximo da consulta é 400 ticks. Se o limite de starts reduzir a cobertura,
o alvo precisa ficar dentro da região completamente consultada; deslocamento do
jogador durante a busca também reduz essa cobertura. Não há `join`, carregamento
nem geração de chunks na habilidade.

Encontrado um alvo, a **mesma entidade** prepara um caminho aéreo de 8, 10 ou 12
blocos na direção da ruína. Emite partículas de alma, mantém-se por 30 ticks na ponta
e volta à proximidade do dono. O estado transitório tem limite total de 200 ticks.
O modo e a âncora de Stay não são regravados. Fuga e reação a ameaças têm prioridade;
interrupções, falta do dono, mudança de modo/estado ou Recall cancelam a orientação.
O Recall mantém sua regra anterior de retornar a Follow após ser concluído.

O cooldown padrão é de 600 ticks e fica em `Progress.resonanceCooldownEnd`, no SavedData
oficial, seguindo o relógio do servidor. Não há custo de XP. Só é aplicado quando um
alvo foi comprovado e um caminho seguro preparado. Ausência de alvo, bloqueio de
caminho, erro de leitura ou timeout não consomem a recarga. Os textos pt_BR/en_US e o
botão mostram disponibilidade, busca em andamento e tempo restante.

**Limite de descoberta:** “não visitada” significa uma ruína já gerada que ainda não
foi usada pelo jogador, inclusive em chunks descarregados. A habilidade não anuncia
ruínas em terreno que nunca foi gerado. Em um mundo sem ruínas geradas na área de
consulta, ela informa que não sente nenhuma, sem inventar destino. Vilas exploradas
antes da atualização não recebem novas construções; a frequência nova exige chunks novos.

## Configuração e comandos

- `data/soulbound/worldgen/structure_set/ancestral_ruins.json`:
  `minimum_separation`, padrão 1000, intervalo 1000–4000.
- `data/soulbound/worldgen/structure/ancestral_ruin.json`:
  `minimum_spawn_distance`, padrão 2000, intervalo 2000–32000.
- Config SERVER: `resonanceRadius` (4096) e `resonanceCooldownTicks` (600).

As distâncias são parâmetros de datapack de worldgen. Configure antes da exploração;
alterações não movem ruínas antigas nem tornam suas separações retroativamente maiores.

Somente operadores recebem coordenadas e informações do índice:

```text
/soulbound altar ruins
/soulbound altar ruin_scan <x> <z>
/soulbound altar ruin_candidates <regionX> <regionZ> <radius>
/soulbound altar ruin_village_probe <x> <z>
```

`ruins` mostra registros, mais próxima elegível para o operador jogador, distância,
exclusões e estado das filas. `ruin_scan` agenda leitura de disco, sem gerar chunks.
`ruin_candidates` recebe regiões do placement de vila e é apenas previsão; não é
localização de uma ruína comprovada. `ruin_village_probe` inspeciona starts de um
chunk já carregado. O comando antigo `candidate` é um diagnóstico da política antiga.
Jogadores comuns recebem somente a direção visual e as mensagens da habilidade.

## Testes executados

- `clean build`: **256 verificações** — 221 anteriores + 35 novas.
- Novas verificações: origem física, rejeição de pedestal artificial e start sem
  vila, leitura de peças antigas, migração sem perda de conclusões, seleção da mais
  próxima, exclusão por conclusão/integridade, cooldown persistente e máquina de voo.
- Política sintética com 3.721 candidatas: 164 selecionadas, contra aproximadamente
  uma por célula de 512 chunks anteriormente; todas as selecionadas respeitaram a
  separação com margem. Essa medição não é a porcentagem de vilas reais no mundo.
- Mundo novo, seed **20261101**: na amostra final de quatro candidatas selecionadas,
  foram confirmadas **quatro vilas e quatro pedestais reais**, em savana e planície,
  incluindo `plains_fountain_01`. Uma amostra anterior também confirmou uma praça
  de taiga. Amostras pequenas não garantem uma taxa universal por vila.
- Pedestais finais confirmados com `execute if block`:
  `(6605,87,5764)`, `(7915,68,3276)`, `(8853,103,6373)`, `(10020,73,1916)`.
  Suas separações horizontais superam 1.000 blocos.
- Índice vazio simulado **somente no mundo isolado de teste**, guardando o registro
  anterior em backup: 192 starts consultados por disco e quatro ruínas reconhecidas,
  enquanto `execute if block` continuou respondendo que os destinos não estavam
  carregados. Não houve clique de jogador nos pedestais.
- Pedestal criado com `setblock` no teste: marca natural vazia e índice inalterado.
- Teste com `FakePlayer` do NeoForge e navegação real: ida de aproximadamente nove
  blocos, retorno em Follow e Stay, âncora preservada, identidade preservada,
  cancelamento sem dono e interrupção por Recall. Isso é automação de servidor,
  **não teste manual multiplayer ou homologação visual**.

A rotina nativa está em fontes de teste, é incluída apenas com
`-Pphase611LiveChecks` e exige o diretório isolado `runs/phase611-server`. Não entra
no JAR e não altera execuções normais. Os logs de validação ficam nesse diretório
ignorado. O primeiro ensaio precisou de tickets de simulação para a entidade realmente
receber ticks; essa preparação do teste foi corrigida. Outro ensaio detectou e corrigiu
o reinício imediato de uma orientação sem reenviar seu caminho ao navegador.

Não remova o SavedData de um mundo real para reconstruir o índice: esse arquivo
também guarda progresso, recompensas e transações. A recomposição da origem física
não recupera histórico perdido; nesses casos é necessário restaurar o backup oficial.

## Arquivos e homologação

Novos: `RuinSelection`, `NaturalRuinProof`, `RuinIndex`, `RuinDiagnostics`,
`AncestralPedestalBlockEntity`, `companion/AncestralResonance`,
`entity/ai/AncestralResonanceGoal`, `entity/ai/ResonanceFlightState`, dois arquivos de
testes, gerador de traduções desta fase e este relatório.

Alterados: `RuinPlacement`, `RuinStructure`, `AncestralBlock`, `AltarSavedData`,
`TrialService`, `TrialEngine` (admissão natural e posições livres), `ModAltar`,
`LupusCompanionEntity`, `LupusCompanionRenderer`, `SoulRecallService`, `SoulCommandMenu`,
`SoulCommandScreen`, `Config`, JSONs de worldgen, traduções, `build.gradle`, ferramenta
RCON e documentação da Fase 6. As receitas e sistemas de evolução/Forja/rituais não
foram reformulados.

Roteiro manual:

1. Execute `\.\gradlew.bat runClient` e use um mundo novo com chunks ainda inexplorados.
2. Como operador, confira candidatas e depois pedestais reais/registro automático.
3. Obtenha seu Lupus pelo fluxo normal, abra o Bastão e clique em Localizar Ruína.
4. Observe ida/pausa/retorno, primeiro em Follow e depois em Stay. Confira UUID e
   âncora antes/depois. Repita em cavernas, obstáculos e perto de ameaças.
5. Teste cooldown, reconexão, cancelamento por logout/morte/portal e Recall durante
   busca/voo. Confirme que XP não muda.
6. Conclua uma ruína e tente novamente: ela deve ser ignorada. Outra em cooldown ou
   ocupada ainda pode ser indicada. Um pedestal artificial não deve virar alvo.
7. Com dois jogadores, tente controlar o Lupus alheio e compare alvos de quem já
   concluiu ruínas diferentes. Valide a interface e as mensagens em pt_BR e en_US.

Aparência da integração em todos os biomas, cliques da HUD e multiplayer com pessoas
continuam pendentes de homologação. `/locate` vanilla continua sem integração com o
placement próprio; use a habilidade ou os diagnósticos acima.
