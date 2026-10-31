# Soulbound Identity

Compatibilidade mantida: Minecraft 1.21.11, NeoForge 21.11.48, Java 21 e wrapper
existente. As habilidades do Baby Explorador permanecem na entidade canônica.

Atualização: a [Fase 5.1](PHASE51_SOUL_RECALL.md) adiciona localização persistente e
recall somente para ACTIVE. As restrições de DEAD/MISSING e a reserva canônica
continuam válidas; os campos adicionais são opcionais na migração.

## Arquitetura e autoridade

O registro global usa `SavedDataType` e codec, APIs da versão instalada, no
`DimensionDataStorage` do Overworld. O arquivo é
`<mundo>/data/soulbound_identity.dat`, compartilhado por todas as dimensões.
O UUID do jogador é a chave oficial. O nome é apenas uma informação de exibição,
atualizada ao entrar no servidor. Trocar o nome não cria outra identidade.

Cada `SoulboundRecord` contém:

- `ownerUuid` e `ownerLastKnownName`;
- `bondUuid`: identificador permanente do vínculo, independente do item;
- `companionUuid`: UUID reservado da única entidade desse vínculo;
- `species`, `evolutionStage` e `behaviorMode`;
- `companionStatus` e `resurrectionRequired`;
- `evolutionProgress` e `godCooldownEnd`, persistidos sem mecânicas novas;
- `eggDelivered` e `eggDeliveryPending`, para entrega idempotente e recuperação.
- `lastKnownDimension`, `lastKnownPosition`, `lastKnownUpdate` e
  `recallCooldownEnd`, adicionados na Fase 5.1 sem alterar os UUIDs.

As mutações usam cópias imutáveis e marcam o SavedData como alterado. UUIDs de
vínculo e companheiro são exclusivos entre proprietários; um vínculo existente
não pode ser substituído. A entidade mantém os campos necessários à persistência
e apresentação, mas sua admissão e os comandos dependem do registro oficial.

| Estado | Significado | Nova invocação pelo ovo |
| --- | --- | --- |
| `READY` | UUID reservado, entidade ainda não invocada | Sim, somente Lupus Baby e sem exigência de ressurreição |
| `SUMMONING` | Reserva persistida antes de inserir a entidade | Não |
| `ACTIVE` | Companheiro admitido; pode estar em chunk/dimensão descarregados | Não |
| `DEAD` | Entidade removida por morte; ressurreição necessária | Não |
| `MISSING` | Entidade descartada; exige revisão administrativa | Não |

Ausência da entidade na memória nunca libera a reserva. O uso do ovo valida o
UUID do jogador, o proprietário e identificador presentes no item, o estado do
registro e entidades com o UUID reservado já carregadas em qualquer dimensão.
Antes de `addFreshEntity`, grava `SUMMONING` no disco. A admissão pelo evento
`EntityJoinLevelEvent` confere o registro e bloqueia entidades não canônicas.
Uma inserção recusada sem exceção restaura o estado anterior. A morte é observada
na remoção definitiva (`KILLED`), sem confundir cancelamento de dano com morte.
Descarregar chunks, sair do servidor ou trocar de dimensão não libera o vínculo.

Nos limites de entrega e invocação, a gravação é aguardada com `saveAndJoin` e
o arquivo é relido para verificar que contém o snapshot esperado. Isso também
detecta erros de escrita que a API vanilla apenas registra no log. Dados
inválidos ou duplicados no disco são recusados; não se cria silenciosamente
outro registro para substituir um arquivo existente que falhou ao carregar.

## Entrega e recuperação do ovo

No primeiro login, cria-se o registro, confirma-se a persistência e tenta-se
entregar um ovo. Sem slot livre, a entrega fica pendente no registro, sem jogar
o item no chão nem sobrescrever itens. A cada segundo tenta-se concluir a entrega
pendente. Após a entrega, atualiza-se o registro e salva-se o inventário dos
jogadores. Reconectar não marca uma nova entrega para registros já concluídos.

O ovo não é consumido ao invocar. É limitado a uma unidade por stack. Em cópias
obtidas no Criativo ou com `/give @s soulbound:companion_egg`, um item sem campos
de proprietário/vínculo é normalizado exclusivamente para o registro já existente
do portador. Um item com credenciais incompletas, inválidas ou de outro dono é
recusado no uso. Nunca são lidos progresso, estágio ou dados de entidade do item
para substituir o registro. Cópias do mesmo ovo no inventário/cursor do dono são
consolidadas na sincronização; a reserva canônica impede invocações concorrentes
mesmo antes dessa consolidação.

`/soulbound recover` é um comando pessoal, sem argumento de proprietário. Ele
reentrega somente o item para o vínculo existente, mantendo UUIDs, progresso e
estado. Com inventário cheio, deixa a entrega pendente. Se o companheiro está
ativo, morto ou indisponível, recuperar o ovo **não** autoriza invocar outra
entidade. A morte do jogador também guarda a intenção de entrega e recupera seu
ovo no respawn, preservando o estado do companheiro.

## Tooltip e sincronização

O servidor constrói a tooltip como `DataComponents.LORE` e grava somente os UUIDs
de proprietário e vínculo em `CUSTOM_DATA`. Componentes vanilla sincronizam esses
dados com o inventário do cliente. A tooltip exibe dono, espécie, estágio, estado,
modo e vínculo permanente, com traduções em português brasileiro e inglês.

O inventário e o cursor são atualizados no login e periodicamente (até 20 ticks).
A mudança Follow/Stay e a remoção definitiva do companheiro atualizam o ovo do
dono imediatamente. Mutações futuras de estágio, progresso ou ressurreição devem
passar pelo registro canônico; a sincronização periódica já lê seu estado atual.
Esta implementação não adiciona essas mecânicas. Uma tooltip administrativa
adulterada pode aparecer brevemente no cliente; ela nunca autoriza uma operação.

## Transferências e limites

- O hook `onDroppedByPlayer` bloqueia o drop comum por Q. O evento de toss cobre
  outros drops pela interface, persistindo a entrega pendente antes de cancelar
  o item já removido do inventário.
- Drops do jogador na morte são recolhidos para entrega pendente do dono real.
- O hook de itens em containers impede inserir o ovo em bundles/shulker items.
- Os hooks de interação de stacks bloqueiam a colocação normal pelo cursor em
  slots externos. O servidor também examina o menu aberto a cada tick e no
  fechamento, removendo ovos válidos de slots externos depois de persistir a
  recuperação para o dono. Isso cobre caminhos como shift-click e teclas numéricas
  que não passam pelo hook de colocação normal.
- A coleta de ovo com dono diferente é recusada. Mesmo que um item seja entregue
  por administração a outro jogador, seu uso não altera o vínculo de nenhum deles.

Ovos podem existir transitoriamente em slots externos até a próxima checagem.
Automações podem extrair antes dessa checagem. Inventários externos fechados e
capabilities de outros mods não são varridos globalmente. Comandos administrativos,
edição externa e mods que contornem eventos/validações não têm proteção absoluta.
Cópias físicas fora do inventário monitorado podem permanecer; a garantia central
é um vínculo e uma reserva de entidade por jogador, não impossibilidade física de
copiar bytes do item.

Os arquivos do registro, jogador e chunks não formam uma transação única. Uma
interrupção brusca pode deixar `SUMMONING`/`ACTIVE` sem entidade salva, ou um ovo
pendente/ausente. O sistema privilegia bloquear duplicação: não libera a reserva
automaticamente. O ovo pode ser recuperado pelo comando; uma entidade ausente
exige revisão administrativa se o recall de ACTIVE também não localizar a
entidade na posição conhecida. Não há ressurreição, altar ou recriação de
companheiro implementados. A confirmação de gravação não garante tolerância a
falhas físicas do armazenamento. Gravações verificadas também podem pausar o
servidor brevemente; não ocorrem em cada tick normal.

## Migração de mundos das fases anteriores

Um Lupus antigo carregado do disco com proprietário e sem `SoulboundBond` pode
ser adotado se o dono ainda não tem entidade reservada/ativa: preserva-se seu UUID,
estágio e modo e associa-se o vínculo canônico. Entre múltiplos Lupus antigos do
mesmo dono, o primeiro admitido vence. Os demais têm a entrada recusada e geram
aviso no log. Não há fusão automática de entidades ou progresso.

Se o jogador invocar um novo Lupus antes de carregar seu Lupus antigo, a nova
reserva vence e o antigo será recusado ao carregar. Para preservar um companheiro
específico de um mundo antigo, carregue sua área antes de usar o novo ovo. Faça
essa validação em uma cópia do mundo. Um `/summon` sem identidade válida já não é
um caminho de criação de companheiros; use a entrega ou normalização do ovo.

## Plano de testes no Minecraft

Execute `./gradlew.bat runClient` ou um servidor dedicado com o JAR gerado.
Os testes abaixo precisam de execução/confirmação manual.

| Caso | Procedimento | Resultado esperado |
| --- | --- | --- |
| Primeiro login | Use jogador e mundo de teste novos; entre e inspecione o inventário | Um ovo, tooltip com o dono, estágio Baby e estado pronto |
| Inventário cheio | Em um mundo de teste, prepare um jogador sem vínculo e com todos os slots ocupados antes de instalar o mod; entre e depois libere um slot | Nenhum item sobrescrito/dropado; entrega pendente concluída uma vez |
| Reconexão | Registre os UUIDs, reconecte várias vezes antes e depois de invocar | Mesmo vínculo e companheiro; nenhum ovo adicional normal |
| Dois jogadores | Cada jogador invoca seu Lupus; por ferramenta administrativa de teste, copie o ovo de A para B e tente usá-lo | Cada um controla apenas seu Lupus; o ovo de A é recusado por B |
| Criativo e comandos | Obtenha ovos no Criativo e por `/give`, com vínculo pronto e depois ativo | Normalização para o vínculo existente; no máximo um ovo mantido no inventário; segunda invocação recusada |
| Progresso preservado | Em cópia de teste do mundo, prepare fixture de registro com progresso/cooldown não padrão; normalize outro ovo | UUIDs, progresso, cooldown, estágio e modo continuam os do registro |
| Transferência | Teste Q, drop pelo cursor, baú com clique normal/shift/tecla numérica, bundle, shulker e coleta por outro jogador | Drop comum bloqueado; itens externos recuperados/pedidos ao dono; uso por terceiros sempre negado |
| Duplicação | Tente usar duas cópias rapidamente, inclusive após descarregar a área do Lupus e visitar outra dimensão | A reserva continua ocupada; nenhuma segunda entidade admitida |
| Reiniciar servidor | Invoque, alterne modo, encerre normalmente e reinicie; tente usar o ovo antes de visitar o Lupus | Estado/UUIDs preservados e nova invocação recusada mesmo com chunk ausente |
| Recuperar ovo antes de invocar | Remova somente o item por comando em mundo de teste e use `/soulbound recover`; invoque | Mesmo vínculo/UUID reservado; uma entidade; ovo permanece |
| Recuperar com companheiro ativo | Remova somente o ovo e use o comando de recuperação | Tooltip ativa; nova invocação recusada |
| Morte do jogador | Morra com keepInventory desligado e depois ligado | Ovo recuperado uma vez; vínculo e estado do companheiro preservados |
| Morte do companheiro | Mate o Lupus, aguarde sua remoção e recupere/obtenha outro ovo | Estado morto e ressurreição necessária; nenhuma nova entidade invocada |
| Regressão Baby Explorador | Teste Follow, Stay, fuga, alertas e Instinto Mineral com a entidade canônica | Habilidades anteriores preservadas, sem ataques |
| Migração | Carregue Lupus antigo de cada dono e depois um duplicado do mesmo dono | Primeiro UUID e modo preservados; duplicado recusado com aviso |

Para conferir o identificador na entidade:
`/data get entity @e[type=soulbound:lupus_companion,sort=nearest,limit=1]`.
Compare `OwnerUUID`, `SoulboundBond` e `UUID` antes/depois. O registro global deve
ser inspecionado por ferramenta NBT com o servidor desligado; não aparece no
comando `/data` de entidades. Nunca use o nome como critério de identidade.

## Arquivos desta implementação

Criados sob `src/main/java/br/com/soulbound/`:

- `companion/identity/CompanionStatus.java`.
- `companion/identity/SoulboundRecord.java`.
- `companion/identity/SoulboundSavedData.java`.
- `companion/identity/SoulboundIdentity.java`.
- `item/SoulEggIdentity.java`.

Modificados:

- `src/main/java/br/com/soulbound/entity/LupusCompanionEntity.java`.
- `src/main/java/br/com/soulbound/item/CompanionEggItem.java`.
- `src/main/resources/assets/soulbound/lang/en_us.json`.
- `src/main/resources/assets/soulbound/lang/pt_br.json`.
- `build.gradle`: task de verificações usando as dependências já existentes,
  sem mudar versões. O classpath de teste reutiliza as bibliotecas de main.
- `README.md`: novo fluxo do ovo e link para este guia.

Criados também: este guia, `src/test/resources/log4j2-test.xml` (logging de testes
somente no console) e
`src/test/java/br/com/soulbound/companion/identity/SoulboundIdentityChecks.java`.
Os arquivos de Baby Explorador já presentes no workspace foram preservados.

## Validação

`./gradlew.bat clean build` executa `identityChecks`, sem dependências novas de
JUnit. Resultado final: **BUILD SUCCESSFUL**, com **24 verificações aprovadas**.
As verificações cobrem serialização de todos os campos, gravação/leitura
NBT comprimida, reserva de invocação, rejeição de UUIDs duplicados, preservação de
progresso/cooldown e entrega/recuperação sem trocar a identidade. Isso não simula
eventos de login, rede, inventários ou jogabilidade.

O servidor dedicado foi iniciado até `Done`, com os handlers de identidade
registrados e sem erros de carregamento de classes de cliente. Foi encerrado
após a checagem. Os testes manuais desta implementação permanecem pendentes.
