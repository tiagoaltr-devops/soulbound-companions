# Fase 6.8 — Lupus God: Avatar Primordial

Minecraft 1.21.11, NeoForge 21.11.48, Java 21. Sem novas dependências, entidade,
receitas, materiais, estruturas ou teclas. Baby, Junior e Pleno permanecem disponíveis.

## Ritual e uso

Com Forja ativa, Pleno desbloqueado, vínculo ACTIVE e nível de companheiro 25,
insira o **Core Divino bruto** existente. Oferendas existentes: Essência Dracônica /
estrela do Nether / 4 cristais do End, na ordem relativa à plataforma. Custo padrão:
60 níveis do dono da Forja ou 65 do visitante; duração 900 ticks (45 segundos).
Os custos continuam usando a configuração SERVER existente. O ritual não sorteia falha.
Companheiro vivo, canônico e disponível a até 32 blocos; aproxima-se da Forja e
deve estar a até quatro blocos ao concluir. Validações precedem o débito de XP.

O resultado é **Núcleo God**, o token `charged_core` de tipo `divine`, vinculado ao
owner UUID e bond. Combine com seu Bastão na bancada para desbloquear. Não ativa
God nem troca UUIDs. O journal existente registra consumo/aplicação; cópias do
mesmo núcleo não dão usos adicionais. Cancelamento segue escrow: materiais retornam,
XP não. Reconectar/retomar não emite outra recompensa já entregue.

No menu do Bastão, com **Pleno ativo**, use o botão God existente. A transformação
dura 1.200 ticks e reserva 6.000 ticks (300 segundos) de cooldown imediatamente. Durante God,
trocas manuais para outras formas são recusadas; ao terminar retorna a Pleno.
Recall mantém o prazo e escudo remanescente, mas cancela habilidades pendentes e
retorna ao modo Passivo como antes. Morte encerra God. Ressurreição retorna Baby,
preservando desbloqueios, nível, XP, talentos e todos os cooldowns.

## Estado canônico, relógio e migração

`CompanionForms` passa a incluir `GodState`, com defaults para saves antigos:
prazo de duração, escudo atual/máximo e cooldowns de Investida, Ruptura e ultimate.
God desbloqueado pertence ao conjunto de formas; `godCooldownEnd` já existente
continua no registro. O estágio permanente máximo continua PLENO; somente a forma
ativa pode ser GOD. Não há segunda identidade ou entidade.

Todos os prazos usam o game time persistido do Overworld. Com servidor rodando,
o tempo continua mesmo com dono offline ou chunks descarregados. Com servidor
desligado, tempo de duração/cooldown fica pausado. Um índice transitório de donos
God é reconstruído do SavedData no início; apenas esses vínculos são examinados
por tick. Ao carregar a entidade, estado expirado também é normalizado antes dos
atributos. Preparação de ultimate é transitória e cancelada ao descarregar.

Desbloqueios do God legado no registro de altar são migrados para as formas do
vínculo. Seu antigo bônus de escala/atributos e eventual timer legado são encerrados;
o cooldown já existente permanece. Núcleos Divinos antigos sem credencial de bond
não autorizam desbloqueio: requerem revisão administrativa, não normalização automática.

Gravações críticas são verificadas com o mecanismo existente: reserva de ativação,
concessão única do escudo, consumo da barreira e cooldowns. Uma ativação interrompida
entre reserva e concessão pode ficar sem barreira; não a recarrega ao retomar.
Arquivos de registro, entidade e relógio do mundo não formam transação física única:
rollback seletivo/crash de armazenamento não recebe garantia absoluta. Gravações
síncronas de dano na barreira podem causar pausas com muitos companheiros God.

## Atributos, barreira e combate

Base God: 67,2 HP, velocidade 0,3192, alcance 3,105, armadura 12 e resistência a
repulsão 0,60. Dano básico é duas vezes o Pleno antes dos talentos. Atributos são
definidos por base, sem modificadores acumuláveis. Vida conserva proporção tanto
na ativação quanto no retorno. Talentos gerais continuam aplicáveis; talentos
exclusivos Junior/Pleno não são herdados indevidamente por God.

A barreira inicial vale 30% do máximo de vida efetivo após aplicar God, incluindo
modificadores existentes. Não cura HP. Seu máximo é o snapshot da ativação, não
uma recarga após trocar talentos ou carregar chunks. Consome dano no evento
`LivingDamageEvent.Pre`, após validações de invulnerabilidade, cancelamentos e
armadura; absorção vanilla processa apenas o restante. Não usa corações de absorção
vanilla nem chama outro dano recursivamente. Fontes que ignoram invulnerabilidade,
como morte administrativa, ignoram a barreira. Fora de God ela fica zerada.

Em Seguir/Combate, reutiliza o controlador Pleno e seu filtro comum de hostis:

| Ataque | Dano sobre básico God | Raio / cooldown |
| --- | --- | --- |
| Julgamento Ancestral | 100 / 150 / 225% | terceiro golpe: onda de 2,5 blocos e repulsão 0,5 |
| Investida Celestial | 200% | 200 ticks; alvo entre 4 e 10 blocos |
| Ruptura Divina | 150% | raio 5, 300 ticks, ao menos dois hostis próximos |
| Apocalipse Ancestral (R) | 450% | preparo 40 ticks, raio 8, cooldown 2.400 ticks |

A onda causa 100% aos hostis secundários; exclui o alvo principal, já atingido
pelo terceiro golpe. Há linha de visão e uma aplicação por alvo em cada área.
Investida usa corredor seguro, chão firme e navegação, sem teleporte ou quebra de
blocos. Em terreno inadequado, permanece com navegação/ataques comuns.
Passivo/Ficar nunca iniciam combate. Ultimate tem cooldown reservado no início;
interrupção não o devolve. R mantém as habilidades próprias de Junior e Pleno.

Cinco talentos God de até três ranks compartilham os 29 pontos máximos:
Poder Primordial +5% terceiro golpe; Investida Suprema +5%; Ruptura Absoluta +5%;
Apocalipse Potencializado +6%; Essência Eterna +3 pontos percentuais de redução de
dano por rank. Investimento exige desbloqueio, nível e pré-requisitos da árvore.
Efeitos exclusivos durante God. O reset existente inclui todos os ranks.

## Visual e interfaces

Modelo próprio God Prime: torso afunilado, braços articulados, armadura refinada,
coroa, ornamentos simétricos, sigilos suspensos e machado em crescente com cabo,
pomo e núcleo. Textura original 128×128, escura com ouro/ciano mais luminosos;
23 regiões UV verificadas pelo gerador. Nenhum asset de terceiro foi utilizado.
Não foi alterada a geometria anterior de Baby, Junior ou Pleno.

Os mesmos estados de animação alimentam poses próprias para idle, caminhada,
três golpes, investida, ruptura, preparo/impacto, transformação, dano e morte.
As mãos seguem a arma integrada por pose de dois segmentos. Aura emite três
partículas a cada dez ticks, enviada pelo servidor. A hitbox usa a dimensão Pleno,
com escala 1, evitando crescimento artificial e colisão maior durante expiração.

O botão God do menu e a quinta aba de talentos usam validação no servidor. A HUD
mantém HP/XP e acrescenta escudo, tempo God e cooldown. Usa o pacote existente,
agora com versão de protocolo 2 e quatro campos adicionais. Snapshots são enviados
quando mudam, com prazo visual arredondado em segundos; não há pacote por frame.

## Arquivos principais

Novos: `GodState`, `GodRules`, `GodFormService`, `client/GodPrimeModel`,
`textures/entity/lupus_god_prime.png`, `tools/generate-god-prime.ps1`,
`GodChecks`, `GodVisualChecks` e este documento.

Integrados: `CompanionForms`, `SoulboundRecord`, `SoulboundSavedData`,
`SoulboundIdentity`, `EvolutionStage`, `CompanionArchetype`, `CompanionFormService`,
`JuniorCombat`, `PlenoCombatController`, `JuniorCombatGoal`, `LupusCompanionEntity`,
`CompanionEvolution`, `RitualService`, `ForgeService`, `ForgeMenu`, `ForgeScreen`,
`AltarTokens`, `TalentDefinitions`, `TalentEffects`, `TalentMenu`, `TalentScreen`,
`SoulCommandMenu`, `SoulCommandScreen`, `CompanionHudData`, `CompanionHudPayload`,
`CompanionHudSync`, `CompanionStatusHud`, `BabyCompanionRenderState`,
`LupusCompanionRenderer`, registro cliente de layer, traduções e testes agregadores.

## Validação e homologação

`./gradlew.bat build --console=plain`: **BUILD SUCCESSFUL**, sem `clean`.
Foram aprovadas **1.512 verificações**: 62 God, 342 visual God, 427 visual guerreiro,
20 visual Baby, 100 progressão, 51 debug, 60 HUD, 68 Junior, 28 sobrevivência,
44 Pleno, 144 altar, 52 ressonância, 24 identidade, 49 Recall e 41 Bastão.
God inclui políticas de ritual/credenciais, defaults antigos, serialização NBT
comprimida em disco, cooldowns separados, conservação de HP, consumo/remoção do
escudo, talentos/reset, ressurreição e roundtrip do pacote HUD. As poses são
montadas com a malha real sem janela/GPU e verificam a pegada da arma.
O primeiro build encontrou sintaxe incorreta na chamada de registro de layer;
a chamada foi corrigida e o build repetido passou.

Os testes automatizados de política, codec, HUD e poses não homologam combate,
eventos de dano, inventários da Forja ou rede num mundo real. Minecraft não foi
iniciado nesta fase. Não foram executados testes de falha física/crash.

1. Em cópia de mundo, recusar infusão abaixo do nível 25/sem Pleno sem cobrar XP.
2. Concluir infusão com dono e visitante: 60/65 níveis, 45 s, núcleo vinculado único.
3. Interromper/desconectar durante infusão; conferir materiais e ausência de núcleo extra.
4. Combinar núcleo próprio/estrangeiro e selecionar God apenas a partir de Pleno.
5. Ativar com HP parcial: conferir proporção, 67,2 HP base e barreira próxima de 20,16.
6. Receber dano, testar cancelamentos/proteções e barreira esgotada; não deve haver cura.
7. Esperar 60 s: retorno Pleno, remoção do escudo e cooldown restante; tentar reativar.
8. Relogar, descarregar chunks e reiniciar: duração/escudo/cooldowns não reiniciam.
9. Testar combo/onda, investida em obstáculos, ruptura em grupos e R contextual.
10. Interromper R por Recall/morte/modo incompatível; nenhuma aplicação tardia.
11. Investir/resetar talentos God e ressuscitar: preservar progresso/desbloqueios/CDs, voltar Baby sem barreira.
12. Inspecionar modelo/UV/poses/HUD no cliente e repetir com dois jogadores em dedicado.

Ajuste posterior: cooldown de novas ativações reduzido para 300 segundos. Prazos de ativações já gravadas conservam o deadline anterior até expirar; HUD/menu continuam exibindo o tempo restante real.
