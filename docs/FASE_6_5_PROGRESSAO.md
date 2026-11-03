# Fase 6.5 — Progressão e talentos

## Arquitetura e migração

`ProgressionState` integra o mesmo `SoulboundRecord` em `soulbound_identity.dat`.
Persiste nível, XP dentro do nível, pontos conquistados, investimentos por ID,
recibos de recompensas/descobertas, frações de bônus de XP e timers de Fúria.
Campos ausentes assumem nível 1, XP 0 e nenhum investimento. Owner/bond/entity
UUIDs, arquétipo, Junior, cooldowns e progressão ritualística antiga não mudam.
Métodos de cópia, morte, Recall e ressurreição preservam o novo estado.

Responsabilidades: `ProgressionState` mantém a aritmética imutável; `ProgressionService`
valida recompensas/investimentos; `TalentDefinitions` define IDs, escopos e requisitos;
`TalentEffects` calcula perfis; menus sincronizam valores por DataSlots vanilla.
Não há segunda identidade, dependência nova, modelo novo, gacha ou árvore de outro arquétipo.

## XP e pontos

- Níveis 1–30, XP inicial 0. Para L → L+1: `50 + 20*(L-1) + 5*(L-1)^2`.
- Vários níveis podem ser adquiridos de uma vez. Um ponto por nível desde 2;
  pontos conquistados sempre equivalem a `nível - 1`, até 29. Saldo = conquistados
  menos investimentos, compartilhado por todos os ramos. Nível 30 não acumula XP extra.
- Bônus percentuais usam centésimos persistidos, evitando perder +5% em pequenas
  recompensas. Aritmética limitada/saturada evita overflow em grandes concessões.

| Fonte | XP base | Confirmação e repetição |
| --- | --- | --- |
| Hostil comum | 8 | Dano efetivo e remoção KILLED; UUID da morte por vínculo |
| Hostil com vida máxima ≥40 | 25 | Mesma regra |
| Ender Dragon/Wither | 200 | Mesma regra; são identificados antes da categoria por vida |
| Descoberta | 30 | Dimensão + tipo + chunk inicial, ou UUID de ruína natural |
| Provação Ancestral | 100 | ID da ruína, com intenção persistida junto à Centelha |

Participação em combate exige perda real de vida/absorção, hostil válido e contribuição
nos últimos 300 ticks. Até 16 participantes por vítima são registrados. Spawner,
trial spawner, comandos, ovos, conversões, origem desconhecida e mobs sem IA não
concedem XP. Origens aceitas: NATURAL, CHUNK_GENERATION, STRUCTURE, PATROL,
REINFORCEMENT e EVENT. Guardiões da provação e aliados não dão XP de combate.
Isso reduz fontes artificiais comuns; não é proteção absoluta contra farms naturais
ou outros mods que falsifiquem a origem da entidade.

Exploração é checada uma vez por segundo com dono e companheiro próximos. Somente
referências locais do chunk FULL e starts já carregados são usados; exige presença
na bounding box confirmada. Tipos atuais: vilas, cidade ancestral, mansão, monumento,
fortaleza do Nether, bastião, fortaleza do End, cidade do End, posto de saqueadores,
pirâmides, ruínas de trilha e câmaras de provação. Ruínas Soulbound são confirmadas
pelo pedestal natural carregado no chunk visitado. Não há busca global ou carga de
chunks; starts descarregados podem adiar a recompensa até uma visita confirmável.

Recibos persistentes impedem revisita/repetição. A Centelha guarda uma intenção de
100 XP, reaplicável sem duplicação em entregas posteriores. Conclusões antigas sem
essa intenção não concedem XP retroativo. O XP pertence ao vínculo, não ao jogador.

## Árvore Lupus

IDs abaixo usam prefixo `soulbound:`. A coluna ramo indica os pontos já investidos
no próprio ramo; “anterior” exige pelo menos 1 ponto. Todos exigem saldo livre e
arquétipo Lupus. Especializações exigem a forma desbloqueada, não necessariamente ativa.

| ID | Máx. | Nível mín. | Pontos no ramo | Anterior |
| --- | --- | --- | --- | --- |
| vitalidade_ancestral | 5 | 2 | 0 | — |
| forca_espiritual | 5 | 2 | 0 | — |
| sintonia_da_alma | 3 | 3 | 2 | — |
| pele_runica | 3 | 4 | 3 | vitalidade_ancestral |
| regeneracao_espiritual | 3 | 6 | 5 | pele_runica |
| olhar_ancestral | 3 | 2 | 0 | — |
| ressonancia_aprimorada | 3 | 3 | 1 | olhar_ancestral |
| aura_protetora | 3 | 5 | 3 | ressonancia_aprimorada |
| vinculo_de_exploracao | 3 | 4 | 2 | olhar_ancestral |
| golpe_aprimorado | 5 | 2 | 0 | — |
| instinto_predador | 3 | 4 | 2 | golpe_aprimorado |
| ruptura_potencializada | 3 | 6 | 4 | golpe_aprimorado |
| sintonia_de_combate | 3 | 8 | 5 | ruptura_potencializada |
| furia_ancestral | 1 | 12 | 8 | instinto_predador |

Efeitos principais: vida +4%/ponto, dano em forma ofensiva +3%, XP +5%, redução
de dano recebido +3%, regeneração +0,1 HP/s após 5 segundos sem combate/dano.
Regeneração exige entidade viva/canônica, sem alvo e fora do ritual.

Baby: Olhar aumenta busca mineral em 2 blocos/ponto por padrão, configurável via
`talentMineralRadiusPerRank`, limite total 32; o orçamento por tick não muda.
Ressonância adiciona marcadores curtos de direção, sem mudar busca/worldgen.
Aura concede Resistência I ao dono a 6/8/10 blocos, renovada por 2 segundos sem
empilhar amplificadores. Vínculo de Exploração adiciona 5% XP de descobertas/ponto.

Junior: básico +4% dano/ponto; velocidade de ataque +3%/ponto (intervalo dividido
pelo fator, arredondado para cima, mínimo 5 ticks); ultimate +8% dano/ponto;
cooldown -4%/ponto, piso seguro de 600 ticks. Fúria dá +10% dano por 100 ticks após
eliminação elegível, com intervalo interno persistente de 400 ticks, sem renovar
continuamente ou empilhar. Bônus de dano principal, forma e Fúria são multiplicativos.
Modo, forma, invulnerabilidade e filtros de combate continuam obrigatórios.

Perfis são recalculados somente ao mudar investimentos/forma. Atributos usam valores
base, não uma pilha de modificadores. Vida preserva proporção inclusive quando há
modificadores externos de vida máxima; refresh idêntico não cura nem drena vida.

## Interface e redistribuição

Menu do Bastão: botão de progressão com nível, XP e pontos, barra e acesso aos
talentos. A tela de talentos mede 320×224, com abas Principal/Baby/Junior e cartões
navegáveis. Mostra efeito, investimento, máximo, requisitos e bloqueio. O cliente
envia apenas o índice da definição no menu do dono; o servidor revalida vínculo,
saldo, ramo, forma, nível, predecessor e ritual. Saldo não muda otimisticamente.

Na Forja ativa, **Redistribuir talentos** (ao lado de Trocar núcleo): deixe o núcleo
vazio, deposite **8 ametistas no altar oposto** à plataforma e fique sobre ela.
Custo fixo de **10 níveis do jogador**, inclusive visitante; duração **200 ticks**,
sucesso 100%. O tooltip informa esses requisitos antes de clicar.

A reserva reutiliza `AltarSavedData.Ritual`, bloqueia nova operação e investimentos.
Cancelamento devolve materiais e não devolve XP, como os rituais atuais. Reinício
recupera reservas interrompidas. Sucesso grava um diário interno `talent_reset`
antes de zerar investimentos no registro canônico. Replay é idempotente; nível,
XP, recibos, formas, progresso ritualístico e UUIDs permanecem. Todos os pontos
conquistados ficam livres. Probabilidades de ativação/reativação não mudam.

## Validação

Fundação e integração passaram em builds incrementais. **506 verificações** no
build final: 80 novas de progressão e 426 anteriores. Log: `build/phase65-build.log`.
Cobertura nova: fórmula, múltiplos níveis, overflow/cap, recibos, frações, migração,
fontes artificiais, pontos, limites/requisitos/arquétipo/formas, bônus, perfis sem
empilhamento, reset, preservação por morte/ressurreição, NBT comprimido e traduções.

No servidor isolado existente: **45 verificações nativas** aprovadas, incluindo
regressões de combate/ultimate/morte/recuperação, XP por morte natural, recompensa
duplicada recusada, level-up, investimento, atributo idempotente (inclusive com
modificador externo), replay do reset, pontos devolvidos e vida proporcional.
Log: `build/phase65-native.log`. Servidor salvo e encerrado por `stop`.
Não foram gerados mundos novos nem executados testes de worldgen.

Os testes nativos invocam endpoints do servidor e o diário do reset; não executam
cliques da interface, débito de XP por jogador conectado ou o ritual completo de
reset. Exploração real, conclusão de prova com XP, aura, regeneração e efeitos
visuais novos precisam de homologação manual. Não há declaração de aprovação visual.
Os arquivos físicos de jogador/altar/identidade não são transação única; permanece
a janela já documentada entre salvar XP debitado e salvar reserva. Não foram
executados testes de corte abrupto, rollback seletivo ou edição externa.

## Checklist manual

- [ ] Abrir cópia de mundo antigo; conferir nível 1/XP 0, Junior, UUIDs e cooldowns.
- [ ] Derrotar hostis naturais com participação do Junior; conferir XP independente
  do jogador, level-up e pontos. `/summon`/ovos/spawners não são fonte de XP.
- [ ] Visitar estrutura com start carregado e ruína natural: 30 XP uma vez por vínculo.
  Revisitar/reconectar não pode repetir. Concluir nova prova: 100 XP uma vez.
- [ ] Investir nos três ramos; conferir limites, pré-requisitos, Junior bloqueado,
  saldo único, restrições de propriedade e mensagens em pt_BR/en_US.
- [ ] Conferir vida proporcional, dano, intervalo, resistência, regeneração, aura,
  alcance mineral limitado, marcadores de Ressonância e cooldown modificado da ultimate.
- [ ] Trocar formas: efeitos exclusivos ficam inativos, investimentos permanecem.
  Baby continua sem atacar; Ficar/Passivo continuam bloqueando combate/ultimate.
- [ ] Fazer Redistribuição com 10 níveis e 8 ametistas no altar oposto, núcleo vazio;
  repetir cliques, cancelar, desconectar e reiniciar. Conferir reserva/consumo únicos
  e devolução de pontos sem apagar XP, nível ou formas.
- [ ] Testar morte/ressurreição, Recall e dois jogadores, preservando toda a progressão.
- [ ] Inspecionar tela de talentos em GUI 320×240 e outras escalas comuns.

## Arquivos

Novos: `companion/progression/ProgressionState`, `ProgressionService`,
`TalentDefinitions`, `TalentEffects`; `menu/TalentMenu`; `client/TalentScreen`;
`ritual/TalentResetService`; teste `companion/ProgressionChecks` e este guia.

Alterados: `SoulboundRecord`, `LupusCompanionEntity`, `JuniorCombat`,
`JuniorCombatGoal`, `CompanionFormService`, `CompanionEvolution`, `MineralInstinct`,
`AncestralResonanceGoal`, `TrialEngine`, `AltarTokens`, `AltarSavedData`,
`RitualService`, `ForgeMenu`, `ForgeScreen`, `SoulCommandMenu`, `SoulCommandScreen`,
`ModMenus`, `SoulboundCompanionsClient`, `Config`, traduções, `AltarChecks`,
`CombatServerChecks`, manual e checklist. Java sob `src/main/java/br/com/soulbound`,
testes sob `src/test/java/br/com/soulbound`. Alterações anteriores preservadas.
Sem push remoto ou início da Fase 6.6.
