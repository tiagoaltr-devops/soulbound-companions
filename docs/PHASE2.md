# Fase 2 — Baby Explorador

Minecraft 1.21.11, NeoForge 21.11.48 e Java 21, sem alterar dependências.
Esta fase está autorizada pelo pedido de Baby Explorador; a restrição de busca
mineral no AGENTS.md descreve o escopo anterior da Fase 1.

## Habilidades

**Alerta:** consulta entidades no servidor a cada 10 ticks. O raio esférico de
12 blocos tem o Lupus como centro. Mobs classificados por vanilla como `Enemy`
e mobs que tenham o proprietário como alvo são considerados ameaças. Alvos do
proprietário têm prioridade; entre candidatos equivalentes, vence o mais próximo.
O Lupus emite o rosnado clássico vanilla e olha para a ameaça durante até 2
segundos, com pelo menos 100 ticks (5 segundos a 20 TPS) entre alertas. A fuga
tem prioridade sobre o olhar de alerta. Nenhum alvo ou IA do hostil é alterado.

**Medo:** uma ameaça a menos de 4 blocos ativa a fuga, acima de Seguir/Ficar na
prioridade das metas. A cada 10 ticks, até quatro candidatos de destino são
avaliados, mais distantes da ameaça e com caminho terrestre alcançável. Destinos
em fluidos ou chunks ausentes são rejeitados. A fuga termina após 40 ticks sem
ameaça detectada a menos de 6 blocos. Flutuar continua com prioridade para
prevenir afogamento. As distâncias de medo são limitadas pelo raio do sensor.

O modo salvo nunca muda durante a fuga:

- **Seguir:** retoma o acompanhamento do proprietário após o perigo.
- **Ficar:** guarda o ponto do comando em `StayAnchor`, foge mesmo em Ficar e
  tenta retornar a esse ponto quando o perigo próximo desaparece. O renderer
  deixa de mostrar a pose sentada durante fuga e retorno. As tentativas de
  retorno têm duração máxima de 200 ticks e intervalo de 40 ticks; sem caminho,
  o Lupus aguarda e tenta novamente. Trocar o modo cancela o retorno; emitir
  novamente Ficar registra o local atual.

`OwnerUUID`, `EvolutionStage` e `BehaviorMode` continuam com as mesmas chaves.
`StayAnchor` também é persistido. Companheiros antigos em Ficar usam sua posição
atual como âncora ao carregar. Alertas, cooldowns e buscas parciais são temporários
e reiniciam após descarregar/carregar a entidade. O Baby continua sem ataques,
sem aquisição de alvo de combate e sem controle por outros jogadores.

**Instinto Mineral:** examina uma esfera de raio 12 ao redor da posição inicial
da busca. As buscas começam no mínimo a cada 200 ticks (10 segundos), sem
sobreposição. O cursor percorre o cubo envolvente, mas só lê blocos na esfera.
Cada posição visitada, mesmo ignorada, consome o orçamento de 128 posições por
tick por companheiro. A confirmação final do minério também respeita o orçamento.
O acesso usa exclusivamente `ServerChunkCache.getChunkNow` e o bloco do chunk
já carregado: a busca não solicita chunks nem cria tickets.

Prioridade fixa: ancient debris > diamante > esmeralda > ouro. São detectados
`ancient_debris`, `diamond_ore`, `deepslate_diamond_ore`, `emerald_ore`,
`deepslate_emerald_ore`, `gold_ore`, `deepslate_gold_ore` e `nether_gold_ore`.
Uma única indicação é emitida ao concluir a busca: partículas verdes e som de
experiência no Lupus. A quantidade de partículas e a altura do som aumentam com
a prioridade; não há coordenadas, direção, marcação no minério ou partículas
atravessando o cenário até o bloco.

## Configuração

`Config.java` usa `ModConfigSpec`, registrado como `ModConfig.Type.SERVER`.
O arquivo gerado é `config/soulbound-server.toml`; o NeoForge também admite
configuração por mundo em `serverconfig/soulbound-server.toml`. Para testes
reprodutíveis, edite com o mundo/servidor fechado e reabra. As regras são
calculadas no servidor, independentemente das preferências locais dos clientes.

| Chave | Padrão | Limites |
| --- | --- | --- |
| `threatRadius` | 12 blocos | 4–32 |
| `alertCooldownTicks` | 100 ticks | 20–12000 |
| `mineralRadius` | 12 blocos | 1–32 |
| `mineralIntervalTicks` | 200 ticks | 20–12000 |
| `mineralBlocksPerTick` | 128 posições por Lupus | 1–2048 |

## Limitações conhecidas

- A detecção de hostilidade é uma heurística: alguns mobs neutros também
  implementam `Enemy`. Alvos mantidos em um `Brain` ou em IA de outros mods podem
  não aparecer em `Mob.getTarget()`. Não se exige linha de visão.
- A fuga depende da navegação vanilla e não garante sobrevivência, uma rota
  livre de todas as ameaças, nem retorno por terreno inacessível. Um hostil
  parado no caminho pode provocar novas fugas. Não há teleporte.
- A busca mineral exige dono vivo, fora do modo espectador, na mesma dimensão
  e a até 32 blocos do Lupus. Movimento superior a metade do raio cancela uma
  busca parcial para evitar indicação de uma área abandonada.
- O resultado não é instantâneo: com os padrões, o cubo de 25³ posições leva
  cerca de 123 ticks (6,15 segundos a 20 TPS). Abaixo de 20 TPS, os intervalos em
  segundos aumentam. Orçamento pequeno ou raio grande pode prolongar a busca
  além do intervalo configurado.
- Chunks ausentes são ignorados, e um bloco alterado depois de ser examinado
  pode ficar para a busca seguinte. O melhor candidato é revalidado antes do
  sinal; se desaparecer, a indicação é suprimida naquele ciclo.
- O limite é por entidade, não global. Muitos companheiros somam trabalho.
  Sons e partículas são sinais no mundo, visíveis/audíveis aos jogadores próximos.
- Não foram implementados combate, evolução, transformação GOD ou rituais.

## Testes manuais

1. Execute `./gradlew.bat runClient`, abra um mundo de teste com comandos e use
   `/give @s soulbound:companion_egg 16`. Teste a invocação, vida 12 e consumo do
   ovo, além de Seguir/Ficar e controle exclusivo pelo dono.
2. Em sobrevivência, à noite ou em local coberto, invoque um zumbi entre 6 e 12
   blocos do Lupus. Observe o alerta e o olhar, e cronometre que os alertas não
   ocorram em intervalo menor que 5 segundos a 20 TPS. Afaste para além de 12
   blocos e confira que não há novos alertas.
3. Com dois hostis, deixe um perseguir o dono e outro mais perto, sem alvo.
   Confira que o alerta prioriza aquele que persegue o dono. O Lupus não deve
   atacar nem modificar o alvo dos hostis.
4. Aproxime o hostil para menos de 4 blocos em terreno aberto. Observe a fuga,
   sem ataques, e remova/afaste o perigo. Confira a retomada de Seguir após o
   período de segurança.
5. Repita em Ficar. Anote visualmente o ponto inicial: deve fugir, levantar da
   pose sentada e retornar ao ponto quando houver caminho. Teste também terreno
   cercado, água e comando do proprietário durante a fuga.
6. Em uma área sem minérios naturais próximos, posicione cada um dos oito
   minérios, um por vez, até 12 blocos do Lupus. Aguarde uma busca completa,
   mantendo o dono próximo e o Lupus parado. Confira som e partículas no Lupus,
   sem coordenadas ou indicação do ponto exato do minério. Teste fora do raio
   em área sem outros minérios para conferir ausência de sinal.
7. Posicione ouro e diamante juntos, depois ancient debris. Compare o tom e
   quantidade de partículas: a prioridade mais alta deve vencer ao final da
   busca, independentemente da ordem de visita dos blocos.
8. Com raio mineral 2 e orçamento 1, confira o atraso adicional. Na fronteira
   de chunks carregados, confira com ferramentas de diagnóstico que a busca
   não aumenta o conjunto de chunks carregados. Muitos Lupus devem ser avaliados
   com profiler para dimensionar o custo total do servidor.
9. Salve e reabra com Lupus em Seguir, Ficar e durante uma fuga em Ficar. Confira
   dono, modo e âncora usando `/data get entity
   @e[type=soulbound:lupus_companion,sort=nearest,limit=1]` em uma única linha.
   Teste também um mundo da Fase 1, sem o campo `StayAnchor`.
10. Em servidor dedicado, conecte dois jogadores: cada Lupus deve seguir apenas
    seu dono, rejeitar comandos do outro e usar a configuração do servidor.
    Repita os alertas, fuga e persistência sem carregar classes de cliente.

## Arquivos desta fase

Criados sob `src/main/java/br/com/soulbound/`:

- `companion/ThreatSensor.java`.
- `companion/MineralInstinct.java`.
- `entity/ai/BabyFleeThreatGoal.java`.
- `entity/ai/ReturnToStayGoal.java`.

Modificados sob o mesmo diretório:

- `Config.java`: substitui as opções de exemplo pela configuração de exploração.
- `SoulboundCompanions.java`: registra a configuração SERVER.
- `entity/LupusCompanionEntity.java`: integra sensores e metas, sincroniza movimento
  emergencial e persiste a âncora de Ficar.
- `client/LupusCompanionRenderer.java`: pose compatível com fuga/retorno.

Documentação criada: `docs/PHASE2.md`. Documentação modificada: `README.md`.

## Validação

`./gradlew.bat clean build`: **BUILD SUCCESSFUL**. A primeira compilação revelou
que `SoundEvents.WOLF_GROWL` não existe em 1.21.11; o alerta foi corrigido para
usar o conjunto de sons clássico de `SoundEvents.WOLF_SOUNDS` dessa versão.
Nenhum teste de jogabilidade desta fase foi declarado aprovado. O roteiro acima
permanece pendente de execução/confirmação manual.
