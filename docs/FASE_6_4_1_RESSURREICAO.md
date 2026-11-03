# Complemento 6.4.1 — Ressurreição e sobrevivência

## Diagnóstico confirmado

Inspeção **somente leitura** dos arquivos `soulbound_identity.dat` em `run/saves`:

| Mundo | Estado salvo | Ressurreição necessária | Estágio / forma ativa | Formas |
| --- | --- | --- | --- | --- |
| New World | DEAD | Sim | Baby / Baby | Baby |
| New World (1) | ACTIVE | Não | Baby / Baby | Baby |
| New World (2) | DEAD | Sim | Junior / Junior | Baby, Junior |

O último mundo confirma DEAD, e não MISSING, com `FOLLOW_COMBAT`. Essa evidência
confirma a morte/progressão preservadas; não prova qual era o modo ou estado da IA
em cada instante anterior. A mensagem genérica do ovo, isoladamente, não diagnosticava
o estado. Comando de diagnóstico reproduzível: `gradlew.bat identitySnapshot`.
O comando não faz parte do build normal e não escreve nos mundos.

Combate e ultimate mantêm as correções já concluídas no
[relatório anterior](FASE_6_4_1_COMBATE.md): modo explícito, tecla no mundo/menu,
autorização sem item na mão, feedback e cooldown. Não há Berserker. A HUD agora
também diferencia **Combate habilitado/desabilitado**, mesmo sem alvo adquirido.
Ataque básico não foi reimplementado: voltou a passar nos testes nativos.

## Correção da recuperação

Havia um ritual legado de Restauração, mas a interface inicial priorizava Despertar
e não apresentava uma opção explícita de ressurreição. Além disso, a aplicação antiga
reiniciava estágio/progresso e formas. Agora o dono de um vínculo DEAD vê
**Ressuscitar Companheiro** numa Forja ativa, com requisitos de Restauração, custo,
mensagens de rejeição e contagem do ritual. Não exige entidade viva para abrir/iniciar.

Reutilizados sem receitas novas: Core da Restauração bruto; 4 diamantes no altar
oposto, 1 lírio-do-vale à esquerda e 1 lágrima de ghast à direita; 25 níveis do dono
da Forja/30 do visitante; duração padrão de 300 ticks. Config/datapacks existentes
continuam sendo a fonte desses valores. A plataforma e o multibloco são necessários.

Reserva e cancelamento reutilizam a transação existente. Ao concluir, um token
interno de recuperação é consumido e seu diário é persistido junto à retirada da
reserva. A identidade passa de DEAD para READY, com Baby ativo e Seguir passivo;
o jogador invoca pelo ovo. Não se cria entidade automaticamente na conclusão.
Owner/bond/entity UUID, arquétipo, estágio máximo, formas, progresso e cooldowns são
preservados. God temporário termina; o desbloqueio permanente não é apagado.

O diário é confirmado somente após persistir a identidade. Reinício pode reaplicar
a recuperação pendente sem consumo duplicado; operação já aplicada é idempotente.
Operações antigas pendentes conservam o tratamento legado. O fluxo legado de Core
Carregado também passa a preservar progressão e liberar READY. Não se recria ACTIVE,
SUMMONING ou MISSING. UUID carregado em qualquer dimensão bloqueia a recuperação.
Ovos DEAD continuam rachados e bloqueados, agora com mensagem específica pt_BR/en_US.
`/soulbound recover` continua recuperando apenas o item do vínculo.

Os arquivos de jogador/altar/identidade não são uma única transação física. Continua
existindo a janela já documentada entre débito de XP e reserva; não se promete
proteção contra rollback seletivo, edição externa ou falha física de armazenamento.

## Vida e testes

Nenhum aumento de vida/armadura foi feito. Baby tem 12 e Junior 24 pontos. O cálculo
existente já preservava proporção; foi centralizado/validado para impedir valores
inválidos, excesso de vida ou cura por alternância. Junior com Baby ferido pode
continuar ferido: transformar não restaura vida integral.

Build incremental: **BUILD SUCCESSFUL**, **426 verificações**. Log:
`build/phase641-survival-build.log`. Testes adicionados:
**28 de sobrevivência**, incluindo recuperação READY, UUIDs, arquétipo, Junior,
progresso/cooldowns, rejeição de estados, serialização/NBT comprimido e proporção.

Servidor isolado existente: **32 verificações nativas aprovadas**. Incluem os 18
casos anteriores de combate/ultimate, Junior 24/24, meia vida nas duas direções,
refresh sem cura, dano controlado 3 → 21/24, remoção KILLED → DEAD persistido,
diário de ressurreição, repetição recusada, READY, nova admissão com o mesmo UUID,
Baby 12/12, ACTIVE e Junior selecionável novamente. Servidor salvo/encerrado com
`stop`. Log: `build/phase641-survival-native.log`. Não foram gerados novos mundos
nem executadas buscas/testes de worldgen. Nenhum save do jogador foi alterado.

Esses testes chamam o endpoint servidor de recuperação e simulam a admissão após
READY. Não simulam cliques da Forja, consumo de XP por FakePlayer, tecla física,
interface cliente ou queda abrupta do processo. Não se declara homologação visual.

## Homologação manual

1. Em cópia do mundo com DEAD, usar o ovo: conferir textura rachada e nova mensagem.
2. Abrir Forja ativa: conferir Ressuscitar, Core bruto, três oferendas e 25/30 níveis.
3. Testar falta de XP, Core e cada oferenda; rejeição não deve consumir materiais.
4. Iniciar; conferir 15 segundos, reserva única, bloqueio de cliques repetidos e
   retomada da exibição ao reabrir a janela. Testar cancelamento/desconexão/reinício.
5. Concluir e usar o ovo: Baby, mesmos UUIDs e Junior ainda disponível. Alternar para
   Junior; reiniciar e conferir progresso. Ovo recuperado pelo comando não duplica.
6. Tentar ressuscitar vínculo vivo, MISSING ou de outro dono; todas as tentativas
   indevidas devem ser recusadas pelo servidor.
7. Validar combate em hostis móveis, retorno ao dono, filtros, R/remapeamento,
   efeitos e cooldown com dois jogadores; Ficar/Passivo/Baby não podem atacar.

## Arquivos deste complemento

`SoulboundRecord`, `CompanionEvolution`, `AltarSavedData`, `RitualService`,
`ForgeService`, `ForgeMenu`, `ForgeScreen`, `CompanionEggItem`, `SoulCommandMenu`,
`SoulCommandScreen`, traduções pt_BR/en_US; testes `SurvivalChecks` (novo),
`IdentitySnapshot` (novo), `AltarChecks`, `CombatServerChecks`; `build.gradle`
(task de inspeção explícita), manual, checklist e este documento. Alterações
anteriores preservadas. Sem novas dependências, gacha, arquétipos, Pleno/God ou push.
