# Fase 6.7 — Lupus Pleno

Minecraft 1.21.11 / NeoForge 21.11.48 / Java 21; dependências e wrapper preservados.

## Implementação

Pleno usa a mesma entidade e o mesmo vínculo. A Ascensão exige Junior desbloqueado,
nível 15, vínculo ACTIVE, companheiro disponível e Forja ativa. Reutiliza o Core
bruto e as oferendas de Ascensão existentes, sem novas receitas. Custa 45 níveis
do dono ou 50 do visitante, dura 600 ticks e não tem sorteio. Cancelamentos seguem
o escrow existente: materiais retornam, XP não. O Núcleo Pleno fica vinculado ao
proprietário e bond; combiná-lo com o Bastão desbloqueia a forma sem ativá-la.

O seletor oferece Baby/Junior/Pleno, com cooldown de 3 s e validação de espaço.
Vida-base 33,6; velocidade 0,266; alcance 2,3; resistência à repulsão 0,20.
A armadura-base Junior é zero: seu aumento de 15% continua zero.
A vida preserva sua proporção ao trocar de forma. A HUD existente recebe a forma
e os atributos reais pelo mecanismo de sincronização já instalado.

Combate exclusivo de Seguir/Combate, com filtro compartilhado de hostis e linha
de visão: combo 100/100/150%; Investida 130%, 12 s; Abalo 80%, raio 4, 18 s,
priorizando dois hostis; R prepara Cataclismo por 30 ticks, raio 6 e dano 250%.
O cooldown de 90 s é reservado antes da preparação e permanece se ela for cancelada.
Não há teleporte ofensivo, explosão, dano a jogadores ou alteração de blocos.

Cinco talentos Pleno de três ranks usam os pontos existentes (nível máximo 30).
Domínio +4% terceiro golpe, Investida +5%, Impacto +5%, Cataclismo +6%, Guardião
+3 pontos percentuais de resistência por rank. A especialização exige desbloqueio;
seus efeitos só atuam em Pleno e participam do reset existente.

Os cooldowns Junior, Cataclismo, Investida e Abalo ficam em `CompanionForms`,
com defaults compatíveis com saves anteriores. Trocas, cópias do registro e
ressurreição preservam os prazos, UUIDs, nível e talentos. Preparações transitórias
não são retomadas após descarregar a entidade. Recall cancela a preparação.

Modelo, textura e animações originais: humanoide robusto, ombreiras, runas ciano
e machado de duas mãos integrado. Animações procedurais para locomoção, três
golpes, investida, abalo, preparação/impacto, dano, morte e ritual. Somente as
classes cliente registram o modelo. Nenhum recurso do JAR de terceiro foi usado.

## Arquivos desta fase

Novos: `companion/PlenoRules`, `entity/ai/PlenoCombatController`,
`client/PlenoCompanionModel`, `textures/entity/lupus_pleno.png`, `PlenoChecks`
e este documento.

Alterados: `CompanionForms`, `CompanionArchetype`, `CompanionFormService`,
`JuniorCombat`, `SoulboundRecord`, `TalentDefinitions`, `TalentEffects`,
`LupusCompanionEntity`, `JuniorCombatGoal`, `RitualFocusGoal`, `CompanionEvolution`,
`RitualService`, `ForgeService`, `ForgeMenu`, `AltarTokens`, `TalentMenu`,
`SoulCommandMenu`, `TalentScreen`, `SoulCommandScreen`, `ForgeScreen`,
`BabyCompanionRenderState`, `LupusCompanionRenderer`, `SoulboundCompanionsClient`,
traduções pt_BR/en_US, `AltarChecks` e `MANUAL_DO_JOGADOR.md`.

## Limites e homologação

A Investida requer corredor livre e chão firme quase plano; em escadas, buracos
ou passagens estreitas o Pleno usa a navegação e ataques comuns. O corpo maior
não atravessa aberturas próprias para Junior. Animações e encaixe das mãos no
machado precisam de inspeção visual em jogo. A cadência depende dos ticks do servidor.

Testar manualmente em cópia de mundo:

1. Recusar ritual abaixo do nível 15 sem cobrar XP; concluir 30 s como dono/visitante.
2. Interromper ritual; conferir materiais, XP e ausência de núcleo duplicado.
3. Combinar núcleo próprio/estrangeiro; selecionar as três formas sem mudar UUIDs.
4. Conferir vida proporcional, Vitalidade, HUD e ausência de cura por transformação.
5. Observar combo, Investida em chão/obstáculos e Abalo com dois hostis.
6. R em Junior/Pleno/Baby, Passivo/Ficar; verificar cooldowns individuais após relog.
7. Cancelar preparação por Recall, desconexão e morte; não deve haver impacto tardio.
8. Investir/resetar talentos e conferir escopo por forma e conservação dos pontos.
9. Ressuscitar e conferir formas, progresso, identidade e cooldowns preservados.
10. Inspecionar modelo, animações e textura no cliente; testar dois jogadores em dedicado.

`./gradlew.bat build --console=plain`: **BUILD SUCCESSFUL**, sem `clean`.
Foram aprovadas **620 verificações**: 44 Pleno, 90 progressão, 60 HUD, 68 Junior,
28 sobrevivência, 144 altar, 52 ressonância, 24 identidade, 49 Recall, 41 Bastão
e 20 visual Baby. Incluem roundtrip do codec, defaults legados, identidade,
cooldowns independentes, desbloqueio sem transformação, nível mínimo, escopo de
talentos e decodificação do PNG. Não simulam navegação, combate ou renderização.
O primeiro build compilou, mas uma fixture nova tentou recuperar um registro
ACTIVE; foi corrigida para DEAD e o build repetido passou.
Não houve execução de Minecraft nem homologação de jogabilidade nesta fase.
