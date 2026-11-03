# Fase 6.4 — Formas e Junior

## Arquitetura e compatibilidade

Uma única `LupusCompanionEntity` conserva owner/bond/entity UUIDs. Trocar de forma
muda perfil, dimensões, atributos, controles e modelo, sem substituir a entidade.
`CompanionArchetype` separa os perfis de forma, movimento, combate e habilidade;
o renderer escolhe geometrias independentes, sem exigir anatomia Baby no Junior.
Não foram adicionadas bibliotecas ou dependências.

O mesmo SoulboundRecord/SavedData agora contém `forms`: `archetypeId`,
`unlockedForms`, `activeForm`, `switchEnd` e `ultimateEnd`. A API expõe os três
campos de identidade visual diretamente. `evolutionStage` continua representando
o maior estágio permanente. Todos os métodos de cópia comuns preservam as formas.

Dados sem `forms` migram para `soulbound:lupus`, Baby desbloqueado e as formas
equivalentes ao estágio anterior; forma ativa e progresso antigo são preservados.
Pleno/God legados não foram apagados nem ampliados. A seleção nova oferece somente
Baby/Junior; selecionar Pleno novamente não faz parte desta fase. Ressurreição
legada mantém seu reset para Baby. Não há outro arquivo de identidade ou gacha.
Futuro requisito: primeiro login sorteará arquétipo somente quando houver vários.

## Infusão e Núcleo Junior

Reutiliza Core do Despertar bruto, receita especial de combinação e diário de
aplicação existentes. A Forja ativa apresenta Infusão Baby → Junior, requisitos,
oferendas, disponibilidade, custo e confirmação. Continua cobrando 30 níveis do
dono/35 do visitante e 2 diamantes/8 ametistas/16 lápis-lazúlis, nas posições atuais.

A infusão dura 400 ticks (20 segundos). Exige Baby canônico vivo, disponível no
mesmo mundo a até 32 blocos da Forja; outro ritual do vínculo é recusado. O goal
temporário aproxima o Baby da Forja, anima concentração e suspende Follow/Stay e
combate sem sobrescrever modo/âncora. Ao concluir, precisa estar a até 4 blocos.
Partículas e sons sinalizam início/conclusão. O resultado é o **Núcleo Junior**,
o token `charged_core` do tipo `awakening`, associado ao autor e vínculo.

O Baby não evolui na conclusão. Combine o Núcleo com o Bastão na bancada 3×3:
o servidor valida dono, vínculo, estágio, disponibilidade e uso único; o diário
existente persiste o consumo antes do desbloqueio. Junior fica desbloqueado, mas
Baby permanece ativo. Tokens antigos sem campo de vínculo aceitam somente seu
proprietário original e os requisitos canônicos atuais; novos tokens validam ambos.
Outros Cores legados conservam suas regras. Cancelamento/devolução sem restituir
XP e recuperação de reservas após reinício seguem a política existente.

## Formas, movimento e combate

No menu do Bastão: Baby, Junior (bloqueado até aplicar Núcleo), Seguir/Combate,
Ficar e Seguir (passivo), além dos controles anteriores. Forma ativa, combate e
cooldown da ultimate são sincronizados pelo servidor. A tooltip distingue estágio
máximo e forma ativa. Trocas têm intervalo persistente de 60 ticks (3 segundos),
verificam colisão, transições incompatíveis e produzem som/partículas.

Baby preserva voo e não ataca, mesmo em Seguir/Combate. Junior usa navegação e
controle terrestres, 24 de vida e velocidade de movimento base 0,28; acompanha e
corre quando distante. Recall conserva a entidade canônica e continua sendo a
recuperação de distância. Ressonância usa destinos terrestres no Junior.

Junior em Seguir/Combate procura alvos a cada 10 ticks, priorizando hostis com alvo
no dono e depois distância. Alcance de procura: 12 do dono, limitado pela distância
do companheiro; abandona alvos afastados. Ataque básico: 4 de dano/20 ticks por
padrão. Ficar, Passivo, ritual e transformação bloqueiam ataques.

Ruptura Ancestral: **R**, remapeável nos controles, com Bastão em uma das mãos.
O payload não recebe UUID, alvo, dano ou coordenadas do cliente. O servidor valida
identidade, Junior ativo, modo, disponibilidade e cooldown; grava 1.200 ticks
(60 segundos) antes dos danos. Raio de 5, dano padrão 8, com linha de visão.
Cooldown não é apagado ao trocar de forma/reconectar. Não há explosão nem destruição.

Melee e ultimate compartilham filtro de hostis (`Mob`/`Enemy`), excluindo jogadores,
aliados, companheiros, domados e Guardiões da provação. Dano usa o caminho vanilla
`hurtServer`, respeitando eventos e invulnerabilidade. Config SERVER adiciona
`juniorAttackDamage`, `juniorAttackIntervalTicks`, `juniorUltimateDamage`.

## Visual

Os altares forneciam a luz do próprio voxel opaco ao item. O renderer agora usa
packed light do ar acima do bloco, conservando o renderizador vanilla de ItemStack,
giro/oscilação, um item visual e sincronização por alteração. Sem ItemEntity.
O resultado em diferentes ambientes de iluminação precisa de conferência em jogo.

Junior possui mesh humanoide e PNG 64×64 originais de teste: armadura escura,
filetes de ouro, runas ciano, elmo e lâmina. Animações procedurais nativas cobrem
idle, caminhada/corrida, ataque, ultimate, transformação e concentração. A textura
é pixel art gerada localmente; nenhum arquivo/código do Cataclysm foi usado.
É um visual provisório, sem modelos novos Pleno/God.

## Testes e homologação

Build incremental: **BUILD SUCCESSFUL**, **390 verificações** — 60 Junior e
330 anteriores. As novas cobrem migração Baby/Junior/Pleno, UUIDs, progresso,
cópias do registro, desbloqueio sem transformação, dono/vínculo/token gasto,
formas bloqueadas, intervalo, filtros de combate, bloqueios de modos/transições,
cooldown/duplicação de ultimate e roundtrip NBT comprimido em disco. PNG também
foi decodificado. Log: `build/phase64-build.log`.

Esses testes são de políticas/persistência, não simulação de mobs, crafting em
rede ou reinício real do jogo. Não foram gerados mundos, executados testes de
worldgen ou iniciados cliente/servidor nesta fase. Homologação manual pendente:

- [ ] Copiar mundo antigo; confirmar vínculo, estágio, formas e UUIDs após carregar.
- [ ] Depositar/retirar/consumir oferendas; conferir iluminação de dia, noite e caverna.
- [ ] Executar infusão nas quatro orientações; observar aproximação/concentração,
  20 segundos, custo único e entrega de Núcleo, mantendo Baby.
- [ ] Cancelar por saída/desconexão; verificar devolução sem XP e ausência de Núcleo.
- [ ] Combinar Núcleo/Bastão; testar outro dono, cópia gasta e repetição por shift-click.
- [ ] Alternar Baby/Junior, conferir modelo, atributos, espaço livre, cooldown e UUID.
- [ ] Testar Junior em terreno, floresta, caverna, desníveis, Follow, Stay e Recall.
- [ ] Criar hostis de teste: conferir melee e bloqueios em Baby/Ficar/Passivo; aldeões,
  animais, jogadores, aliados e companheiros não podem ser atingidos.
- [ ] Pressionar R com Junior/Combate; conferir área, animação, dano, cooldown e spam.
  Trocar forma/reconectar não deve liberar a habilidade antes do prazo.
- [ ] Reiniciar normalmente em cada etapa; testar dois jogadores e servidor dedicado.
- [ ] Regressão de Ressonância, item de vínculo, recuperação, ativação e reativação.

## Arquivos

Criados (Java sob `src/main/java/br/com/soulbound`): `companion/CompanionForms`,
`CompanionArchetype`, `CompanionFormService`, `JuniorCombat`, `JuniorCorePolicy`;
`entity/ai/JuniorCombatGoal`, `RitualFocusGoal`; `network/JuniorUltimatePayload`;
`client/JuniorControls`, `JuniorCompanionModel`. Também `lupus_junior.png`,
`src/test/java/br/com/soulbound/companion/JuniorChecks.java` e este documento.

Alterados: `SoulboundRecord`, `BehaviorMode`, `LupusCompanionEntity`,
`FollowCompanionOwnerGoal`, `BabyFlightTargets`, `BabyFleeThreatGoal`,
`ThreatRecoilGoal`, `BabyHoverGoal`, `AncestralResonanceGoal`, `AncestralResonance`,
`SoulRecallService`, `CompanionEvolution`, `RitualService`, `AltarSavedData`,
`AltarTokens`, `ForgeService`, `ForgeMenu`, `ForgeScreen`, `SoulCommandMenu`,
`SoulCommandScreen`, `OfferingRenderer`, `BabyCompanionModel`,
`BabyCompanionRenderState`, `LupusCompanionRenderer`, `SoulEggIdentity`, `Config`,
inicializadores comum/cliente, traduções, `AltarChecks`, manual e checklist.
Alterações anteriores do workspace foram preservadas; sem push ou Fase 6.5.
