# Fase 6.9 — Sobrevivência, equipamentos e IA defensiva

Minecraft 1.21.11 / NeoForge 21.11.48 / Java 21. Sem novas dependências, receitas,
modelos, evoluções, talentos ou estruturas. Cliente e servidor precisam do JAR atualizado.

## 6.9.1 — HP, regeneração e XP

Baby, Junior e Pleno têm **33,6 HP-base**; God **67,2**, antes de talentos e itens.
Os demais atributos das formas foram mantidos. Trocar as formas normais ou elevar
o máximo com equipamento/talento não cura: conserva HP absoluto, limitado ao máximo.
Na migração, um Baby antigo ferido não recebe cura automática pelo aumento de base.

Entrar em God conserva o percentual de HP efetivo e registra o fator usado nessa
ativação. Sair divide o HP vivo por esse fator, limitado ao novo máximo. Isso
contabiliza dano/cura reais e impede equipar bônus aditivo durante God e removê-lo
após o retorno para criar HP. Sem mudanças de máximo durante God, mantém o cálculo
proporcional anterior. Se houver mudanças de equipamento/talento durante God,
o percentual na saída pode diferir: prioriza não gerar cura. `GodHealthScale` no
NBT é um fator histórico de conversão, não uma segunda cópia de HP. `Health`
vanilla continua sendo a única fonte de HP persistido.

No carregamento, os modificadores transitórios de HP dos itens canônicos são
restabelecidos antes de restaurar o mesmo `Health` salvo. Isso evita perder vida
acima do máximo sem equipamento na leitura vanilla. O ciclo nativo assume depois
os mesmos IDs, sem somar modificadores duplicados.

Após **160 ticks sem dano de combate causado/recebido**, sem alvo e sem transição
ou ritual, o mesmo fluxo de regeneração cura a cada 40 ticks:

| Forma | Cura natural / 40 ticks |
| --- | --- |
| Baby | 0,5 |
| Junior | 1,0 |
| Pleno | 1,5 |
| God | 2,0 |

Regeneração Espiritual acrescenta **0,2 HP por rank a cada 40 ticks**, mantendo
0,1 HP/s por rank, até três ranks. Não há duas regenerações concorrentes. Dano
ambiental sem entidade causadora não reinicia o prazo de combate. Dano de combate
absorvido pela barreira ainda conta como combate. Cura nunca repõe a barreira.
Prazos de combate/regeneração são do vínculo, marcados dirty sem flush por tick.

XP mantém fórmula, teto 30, pontos, filtro de origem, contribuição e deduplicação
por UUID. A janela de 300 ticks agora usa o instante da morte, não a remoção do
cadáver 20 ticks depois ou após unload. A concessão continua exigindo remoção
definitiva KILLED e credenciais válidas do vínculo. Comandos/ovos/origem ausente
continuam recusados por padrão; a opção de desenvolvimento anterior não mudou.
Conversões/divisões sem procedência elegível continuam excluídas; não foi liberada
genericamente a origem CONVERSION/TRIGGERED nem enfraquecido o anti-farm.

## 6.9.2 — Nove slots e alimentação manual

No Bastão, **Equipamentos**, a até oito blocos do companheiro vivo/canônico:
elmo, peitoral, calças, botas, arma e quatro alimentos. Não existe armazenamento
genérico. Equipamentos têm limite de uma unidade; alimentos usam o limite do item.
Clique, shift-click, cursor e mudanças são processados pelo menu nativo no servidor.
O menu identifica proprietário e bond, valida itens e recusa sessão inválida,
companheiro morto/ausente ou ritual conflitante. Atualizações concorrentes de
desgaste/alimento recarregam a projeção antes de aceitar um clique desatualizado.

`SurvivalState` fica dentro de `SoulboundRecord`: nove snapshots de ItemStack,
prazo de combate, próxima regeneração, próximo alimento e histerese defensiva.
O codec anterior de 16 campos foi composto com um MapCodec adicional, mantendo
os mesmos campos legados e um `survival` opcional. Mundos antigos iniciam vazios.
Os snapshots reutilizam `AltarInventory` com ItemStack.CODEC e contexto dos
registries: quantidade, componentes, encantamentos e dados customizados persistem.

Slots nativos da entidade são cópias de trabalho. O vínculo é a autoridade;
desgaste/quebra é devolvido ao registro e entradas obsoletas de NBT não substituem
o inventário oficial. Itens não caem na morte; ressurreição, Recall, unload e troca
de forma conservam os nove slots. O renderer não adiciona camadas de equipamentos:
armaduras/arma permanecem invisíveis nos modelos espirituais existentes.

Armaduras usam `EQUIPPABLE`, posição correspondente e tipos de entidade permitidos.
Armas aceitam `WEAPON` ou modificador de ataque na mão principal. Atributos são
aplicados pelo ciclo nativo, incluindo armor/toughness/HP/movimento/repulsão e
dano/velocidade de ataque suportados. IDs transitórios evitam acúmulo. Vida-base
da forma e bônus dos itens não são somados manualmente duas vezes.

As habilidades partem do ATTACK_DAMAGE efetivo, preservando seus multiplicadores.
Cadência mantém o intervalo configurado sem arma e aplica a razão de ATTACK_SPEED
efetivo sobre a referência nativa 4. Encantamentos ofensivos passam uma vez pelo
helper nativo de dano/pós-ataque. Armadura usa absorção/encantamentos nativos;
durabilidade utiliza hooks nativos e é persistida. Ataques em área podem desgastar
a arma por alvo atingido. Baby continua sem iniciar ataques, mesmo equipado.

Alimentação manual: dono, companheiro vivo, ferido e sem preparação/ritual. Cura
usa `FoodProperties.saturation()`, que nesta versão já contém nutrição × modificador
× 2. Exemplos: frango 7,2; bife/costeleta 12,8; cenoura dourada 14,4; maçã dourada
9,6. Consome uma unidade inclusive em Criativo; HP cheio ou cura cancelada não
consomem. Não executa efeitos especiais do alimento nem restaura escudo.

## 6.9.3 — Defesa e alimentação automática

Histerese considera apenas HP real: entra com **≤50%**, sai com **≥65%**.
Suspende aquisição/execução ofensiva, limpa alvo e cancela preparação pendente
com segurança; cooldown de habilidade reservado não é devolvido. O goal defensivo
terrestre tenta até dez destinos (duas distâncias e direções simétricas) com
intervalo de 40 ticks. Sem caminho, libera a navegação normal para não travar.
O planejamento reconhece apoio sólido quando o flag onGround ainda não assentou;
o flag é ajustado apenas durante a chamada e restaurado imediatamente. Sem
teleporte, quebra de blocos ou passagem através de obstáculos. Pathfinder nativo
da versão consulta chunks já disponíveis. Baby reaproveita sua fuga aérea anterior.

Novo recuo só ocorre em Seguir/Combate. Ficar/Passivo podem consumir alimentos,
sem receber ataques ou deslocamentos ofensivos novos. O modo e âncora de Ficar
não são sobrescritos. Após recuperação, o combate anterior pode ser retomado.

Alimentação automática avalia a cada dez ticks, respeitando **100 ticks entre
consumos**, sem interromper ações/preparações. Em defesa pode comer durante o
perigo; em estado ofensivo aguarda ficar sem alvo. Prioriza alimento sem desperdício
mais próximo do HP ausente; se todos excederem, escolhe o menor excesso.
Debita uma unidade canonicamente antes da cura; cancelamento da cura restitui o
alimento. Prazo não é reiniciado por forma, God, Recall, relog ou ressurreição.

`USE_REMAINDER` é preservado: alimentação manual substitui a última unidade pelo
recipiente ou o devolve ao inventário do dono. Automática envia o recipiente ao
dono online; se não houver espaço ou o dono estiver offline, não consome esse
alimento. Não transforma slots de comida em inventário de tigelas nem descarta
recipientes no chão. Alimentos sem componente alimentar/saturação positiva não
são aceitos. Não simula efeitos proprietários de mods.

## Compatibilidade e limites

- Atributos suportados pela entidade e componentes nativos são reaproveitados.
  Atributos customizados que exigem registro específico na entidade, ações exclusivas
  de jogadores, tiros, carregamento de arco, escudos ativos e habilidades de mods
  não são implementados artificialmente.
- Mending não recebe automaticamente o XP do vínculo: ele é separado de orbes
  vanilla. Binding/Vanishing não são usados para destruir ou tornar irretiráveis
  os itens do vínculo. Hooks de proteção/dano de outros mods ainda precisam de teste.
- `FOOD` e `USE_REMAINDER` são compatíveis com itens de modpacks que os forneçam;
  callbacks especiais de consumo de mods não são executados.
- Inventário, HP do chunk e playerdata não formam transação física única. Fluxo
  normal de cliques/fechamento/logout é serializado no servidor, mas não se promete
  atomicidade contra crash, rollback seletivo ou edição externa. Uma interrupção
  entre débito do alimento e HP salvo pode perder o consumo, sem recriá-lo.
- Snapshot de itens é codificado quando muda; desgaste/regen marcam SavedData dirty.
  Transferências e alimentos fazem flush em fronteiras sensíveis, não por tick.
- Não há mudança estética dos modelos nem redesign da HUD. A tela nova ainda
  depende de inspeção de layout em cliente real.

## Arquivos principais

Novos: `companion/SurvivalState`, `SurvivalRules`, `CompanionSurvival`,
`CompanionEquipment`, `CompanionFood`, `entity/ai/DefensiveRetreatGoal`,
`menu/CompanionInventoryMenu`, `client/CompanionInventoryScreen`, testes
`Phase69Checks`/`Phase69ServerChecks` e este guia.

Integrados: `SoulboundRecord`, `LupusCompanionEntity`, `CompanionEvolution`,
`TalentEffects`, `JuniorCombat`, `PlenoCombatController`, `ProgressionService`,
`SoulboundIdentity`, `ModMenus`, `SoulCommandMenu`/`SoulCommandScreen`, registro
cliente, traduções e manual. Gradle só ganhou uma seleção opt-in para o diretório
isolado de testes; wrapper/dependências não mudaram. Fixtures antigas foram
ajustadas aos HP novos e para não executar ressonância na fixture de sobrevivência.

## Validação

**Build final: BUILD SUCCESSFUL**, sem `clean`, com **1.540 verificações**.
Foram aprovadas adicionalmente **23 verificações de itens nativos** e **50 de
integração em servidor dedicado**, totalizando **1.613 verificações executadas**.
O servidor isolado chegou a `Done`, executou a fixture e foi salvo/encerrado.

Verificações novas incluem HP-base, ausência de cura na troca/equipamento,
recarga NBT com HP acima do máximo sem armadura, atributos nativos e ausência de
acúmulo, Smite/Protection, desgaste de arma, abate NATURAL e XP único, componentes
e encantamentos serializados, nove slots, shift-click de ida/volta, rejeição de
item e outro jogador, saturação e recipiente manual, maçã sem absorção, cooldown
automático, histerese, recuo pelo GoalSelector, Ficar, regeneração God, escudo
inalterado pela cura, equipamento de HP durante God e dados de ressurreição.
Os testes de regras usam 28 verificações novas e toda a suíte anterior permaneceu
aprovada. Uma execução direta de itens no executor simples falhou por ausência
de loader FML; foi substituída pelo contexto real do servidor. A construção da
fixture também expôs a recusa de navegação antes de onGround, tratada no planejador.

A fixture nativa usa FakePlayer e um mundo isolado em `runs/phase69-server`;
não homologa rede de dois clientes, layout visual, corte abrupto do processo ou
todos os equipamentos de modpacks. A ressurreição foi verificada por transição
de registro; não foi refeito todo o ritual com jogador. Não houve teste manual
com jogador nem declaração de homologação das funcionalidades novas.

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat runPhase611Server -Pphase611LiveChecks -PsurvivalLiveChecks --console=plain
```

O segundo comando reaproveita a infraestrutura de teste já existente, usa porta
25579 e requer configuração local de teste/eula em diretório ignorado. Nunca use
configuração offline de teste num servidor público. As fixtures ficam fora do JAR.

## Homologação manual — até 12 cenários

**6.9.1:**

1. Conferir 33,6 nas formas normais e 67,2 God sem talentos/equipamentos.
2. Ferir, alternar formas e trocar Vitalidade/equipamento de HP; não criar cura.
3. Equipar HP durante God, curar/receber dano, relogar e retornar; não explorar HP.
4. Medir oito segundos e regeneração por forma/rank; abates naturais, `/summon`
   com configuração padrão e XP após relog/unload do cadáver.

**6.9.2:**

5. Abrir pelo Bastão, validar nove slots e impedir acesso por outro jogador/morto.
6. Testar cursor, shift-click, teclas numéricas, inventário cheio e desconexão.
7. Comparar armadura/toughness/velocidade/dano e encantamentos vanilla; retirar e relogar sem acúmulo.
8. Alimentar com carne, maçã dourada, sopa e itens de modpack; testar HP cheio e recipiente sem espaço.

**6.9.3:**

9. Atingir 50%, recuar e alimentar; voltar ao combate somente aos 65%.
10. Testar corredores, bordas, ausência de alimento/caminho, Ficar e Passivo.
11. Medir cinco segundos entre consumos, seleção com menor desperdício e proteção de casts.
12. Morrer/ressuscitar/Recall/reiniciar: conservar itens e prazos; God sem recarga de escudo/cooldown.
