# Fase 5.1 — Chamado da Alma

Minecraft 1.21.11, NeoForge 21.11.48 e Java 21. Nenhuma versão de dependência ou
wrapper foi alterada. O sistema usa o Bastão da Alma e o menu da Fase 5 existentes.

## Uso

Segure o bastão, use Shift + clique direito e escolha **Chamar companheiro**.
O registro precisa ser ACTIVE, com proprietário/vínculo válidos e cooldown
encerrado. O chamado funciona em Follow ou Stay. Depois do sucesso, o companheiro
fica próximo do dono em FOLLOW_PASSIVE, com partículas, som e mensagem.

O menu mostra o prazo restante e desabilita o botão durante cooldown ou operação
pendente. O cooldown de recall é separado do intervalo curto dos comandos
Seguir/Ficar: são **600 ticks**, equivalentes a 30 segundos a 20 TPS. O prazo usa
`server.overworld().getGameTime()`, persistido no mundo, não o tick contador de uma
sessão do servidor. Reiniciar não zera o prazo; tempo com o servidor desligado ou
pausado não o consome. Abaixo de 20 TPS, os segundos reais serão maiores.

## Localização canônica e migração

O mesmo `soulbound_identity.dat` agora contém quatro campos opcionais por jogador:

| Campo | Conteúdo | Padrão de migração |
| --- | --- | --- |
| lastKnownDimension | Chave da dimensão, como minecraft:the_nether | Ausente |
| lastKnownPosition | Vetor XYZ dos pés da entidade | Ausente |
| lastKnownUpdate | GameTime global do último registro | 0 |
| recallCooldownEnd | GameTime global de fim do cooldown | 0 |

Os campos ficam no nível principal do registro no NBT, sem um novo arquivo por
companheiro. `SoulRecallState` agrupa os campos apenas no Java para respeitar o
limite de aridade do RecordCodecBuilder. O codec aceita registros antigos sem
esses campos, preservando proprietário, vínculo, companheiro e progresso.
Todas as cópias imutáveis do registro retêm o estado de recall.

A localização é atualizada em memória a cada **40 ticks**, ao admitir a entidade,
ao salvá-la, ao descarregar seu chunk e nas saídas/entradas por dimensão. Mudanças
normais marcam SavedData como alterado; não há `saveAndJoin` a cada tick. Eventos
de salvamento também coletam posições atuais e enfileiram a gravação assíncrona.
O sucesso do recall enfileira um snapshot com o destino e o cooldown.

Um registro antigo ACTIVE cuja entidade já está descarregada pode não ter como
informar onde ela está. Nesse caso o chamado falha com mensagem específica:
carregue a área original uma vez para registrar a posição. Não é inventada uma
localização, não se pesquisa todo o disco e não se libera a reserva.

## Recuperação controlada

`SoulRecallService` procura primeiro o `companionUuid` nos mapas de entidades
carregadas de todas as dimensões. Confere tipo, ownerUuid, bondUuid, UUID, vida e
identidade canônica. Mais de uma entidade visível com o UUID ou uma entidade
incompatível provocam recusa, sem escolher arbitrariamente outra.

Se não estiver carregada, usa somente a dimensão e o chunk da posição registrada:

1. Confirma que a dimensão existe e a localização é válida.
2. Reserva uma operação exclusiva por bondUuid; há no máximo oito recalls
   pendentes por servidor para limitar trabalho concorrente.
3. Adiciona o ticket próprio `soulbound:soul_recall` com
   `ServerChunkCache.addTicketAndLoadWithRadius`, raio 0 e flags de carregamento
   e manutenção temporária da dimensão ativa. O centro fica FULL/visível para
   carregar entidades, sem solicitar simulação de mobs.
4. A cada `ServerTickEvent.Post`, verifica a conclusão e a carga das entidades.
   Não usa `join`, `getChunkFuture` bloqueante nem espera de disco na thread
   principal. Nenhum callback de IO manipula o mundo.
5. Revalida o jogador e o item que iniciou o chamado antes de transferir.
6. Libera o ticket ao concluir, falhar, expirar, desconectar ou desligar o servidor.

O timeout é de **200 ticks**. O ticket não é persistente e ainda tem expiração de
segurança de **240 ticks**. Em 1.21.11, tickets se agrupam por tipo/nível, sem uma
chave de proprietário: recalls para o mesmo chunk compartilham uma lease com
contador de referências. Encerrar um deles não remove o ticket do outro.

O carregamento FULL pode exigir chunks vizinhos como dependências internas do
Minecraft; isso não é uma varredura de região feita pelo mod. Ao liberar o ticket,
o descarregamento fica a cargo do ciclo normal do Minecraft e de outros tickets
que já existam. O mod não promete descarregamento instantâneo de um chunk.

Não localizar a entidade no chunk, falha de carregamento ou timeout não criam
uma substituta, não aplicam cooldown e não transformam ACTIVE em READY/MISSING.
O vínculo permanece reservado. O serviço não chama fábrica de entidades.

## Destino e transferência entre dimensões

O destino é escolhido entre no máximo 96 candidatos a 2–4 blocos do jogador,
com pequenas variações verticais. Confere a hitbox atual da entidade, uma margem
de segurança, chunks já carregados no destino, colisões, fluidos, blocos perigosos,
portais, altura de construção e world border. Sem candidato seguro, o chamado é
cancelado antes de mover a entidade. Como o Baby voa sem gravidade e não acumula
dano de pouso normal, não é exigido um bloco de chão sob o destino.

O transporte usa `Entity.teleport(TeleportTransition)` de 1.21.11, sem portal
search nem construção de portal. A mesma API atende Overworld, Nether e End.
Na troca de dimensão, a API vanilla pode criar outro objeto Java via `restoreFrom`;
isso transporta a identidade e os dados da entidade, não invoca um novo companheiro
lógico. O mod verifica que o objeto admitido no destino preservou o UUID canônico,
ownerUuid, bondUuid e estado ACTIVE. Não há atribuição de um UUID novo.

`CHANGED_DIMENSION` continua sem ser interpretado como morte ou descarte pelo
Soulbound Identity. O registro de destino é atualizado após a admissão. Depois
da confirmação, o Baby limpa metas/referências temporárias da área antiga, para
o voo residual e retorna a Follow. Estágio, evolução, progresso, atributos e
cooldown GOD não são redefinidos; o cooldown próprio do recall é aplicado só
após confirmar a transferência e o modo.

Montarias/passageiros são recusados para não transportar outras entidades junto.
Se outro mod bloquear ou interferir na transferência, o serviço informa falha,
mantém a reserva e não aplica cooldown sem confirmar sucesso. Não existe fallback
que clone ou recrie a entidade.

## Autorização e regressão

O cliente envia somente o novo ID de botão, usando a sessão de menu existente.
Não escolhe ownerUuid, bondUuid, companionUuid, origem ou destino. O servidor
confere a janela, a mão, as credenciais e os UUIDs da sessão contra SavedData.
Essas validações são repetidas durante uma espera assíncrona. Trocar/perder o
item, desconectar ou perder o estado ACTIVE cancela a solicitação pendente.

DEAD, MISSING, SUMMONING, READY, vínculo incorreto, outro jogador ou exigência de
ressurreição não autorizam recall. O cooldown impede uma nova operação após
sucesso; a fila impede chamadas simultâneas antes de haver cooldown.

Não foram alterados os cálculos de voo, fuga, alertas, busca mineral ou aparência
do item. `/soulbound recover` continua recuperando apenas o item, sem consumir ou
zerar o cooldown de recall. A atualização de localização foi adicionada aos
pontos existentes de persistência da identidade e da entidade.

Limitações: dados desatualizados após interrupção brusca/edição externa podem
apontar para um chunk antigo; uma falha nativa ou interferência de outro mod na
admissão entre dimensões pode exigir revisão administrativa. Registro, jogador
e arquivos de entidades não formam uma transação única. A gravação de sucesso é
enfileirada, não uma garantia contra falha física de armazenamento. Em todos
esses casos, não se libera automaticamente o vínculo nem se cria substituta.

## Arquivos desta fase

Criados sob `src/main/java/br/com/soulbound/`:

- `companion/identity/SoulRecallState.java`.
- `companion/recall/CompanionLocationTracker.java`.
- `companion/recall/RecallQueue.java`.
- `companion/recall/SoulRecallDestinations.java`.
- `companion/recall/SoulRecallService.java`.
- `registry/ModTickets.java`.

Modificados sob o mesmo diretório:

- `companion/identity/SoulboundRecord.java`: migração/codec e cópias dos campos.
- `companion/identity/SoulboundSavedData.java`: evita rescan de todos os donos
  quando uma atualização de localização mantém os mesmos UUIDs.
- `companion/identity/SoulboundIdentity.java`: localização nos eventos existentes.
- `entity/LupusCompanionEntity.java`: rastreamento e conclusão de recall.
- `companion/ThreatSensor.java`: descarte de referências transitórias antigas.
- `menu/SoulCommandMenu.java` e `client/SoulCommandScreen.java`: botão, prazo e
  estado pendente usando mais dois DataSlots.
- `SoulboundCompanions.java`: registro do ticket.

Criados também este guia e
`src/test/java/br/com/soulbound/companion/recall/SoulRecallChecks.java`.
Modificados `build.gradle`, `lang/pt_br.json`, `lang/en_us.json`, `README.md` e
as notas de atualização em `docs/SOULBOUND_IDENTITY.md` e `docs/PHASE5_SOUL_STAFF.md`.
Nenhum arquivo das fases anteriores foi removido.

## Validação automatizada

`./gradlew.bat clean build`: **BUILD SUCCESSFUL**, com 49 verificações novas de
recall e 85 verificações anteriores (134 no total). Os testes novos cobrem:

- Migração sem localização/cooldown e campos planos no codec.
- Gravação/leitura NBT comprimida, cooldown e localização após leitura de disco.
- Preservação dos três UUIDs, progresso, cooldown GOD, espécie, estágio e modo.
- Política ACTIVE, recusa de outro proprietário/vínculo e identidade de entidade.
- Exclusão por vínculo, timeout, limite global, repetição e liberação da fila.
- Seleção limitada de candidatos perto do dono e cancelamento sem candidato seguro.

Esses testes não iniciam mundos nem simulam transferência ou IO de chunks. O
primeiro teste isolado encontrou dependência indevida de uma constante de Level
que exige FML; foi corrigido usando chaves de dimensão independentes do loader.
Não houve erro de compilação Java na implementação do recall.

O servidor dedicado foi iniciado até `Done`, com o ticket e eventos registrados,
sem erro de classes client-only observado. Isso valida inicialização, não os
cenários de jogabilidade abaixo.

## Roteiro de testes de integração/manual

Use uma cópia do mundo e anote UUID, OwnerUUID e SoulboundBond do Lupus antes de
cada cenário. Para consultar a entidade carregada:
`/data get entity @e[type=soulbound:lupus_companion,sort=nearest,limit=1]`.
Inspecione o SavedData com servidor desligado quando precisar conferir os campos
globais. Não edite dados de produção durante testes.

| Caso | Procedimento | Resultado esperado |
| --- | --- | --- |
| Próximo | Invoque, abra o menu e chame com o Lupus ao lado | Mesmo UUID em posição livre próxima, Follow, partículas/som e cooldown |
| Stay | Defina Ficar, afaste-se e chame | A entidade sai de Stay, abandona a âncora antiga e segue o dono |
| Distante carregado | Outro jogador mantém a área do Lupus carregada; o dono chama de longe | Transporte do mesmo companheiro, sem ticket permanente |
| Chunk descarregado | Deixe em Stay, espere rastreamento/salve, afaste todos os jogadores além da distância de simulação/view e chame | Carregamento temporário da origem, admissão do UUID original, retorno e liberação do ticket |
| Dimensões | Deixe o Lupus no Nether e chame do Overworld; repita com End e nos sentidos inversos | UUID, vínculo, vida, atributos e progresso preservados; ACTIVE e Follow |
| Reinicialização | Deixe em Stay distante, salve e reinicie, sem visitar sua área antes de chamar | Localização persistida permite recuperação pelo chunk correto |
| Cooldown | Após sucesso, confira contador, tente repetir por sessão/pacote de teste e reinicie antes de acabar | Prazo não zera; mensagem de segundos no servidor; habilita após 600 ticks em execução |
| Destino inseguro | Em mundo de teste, obstrua todos os candidatos de 2–4 blocos e alturas próximas do dono | Nada movido, sem cooldown; após liberar espaço, pode tentar novamente |
| Outro dono | Com dois jogadores, tente usar item/sessão de A como B, incluindo ID de botão forjado | Nenhuma alteração no companheiro ou vínculo de A |
| Morto/MISSING/SUMMONING | Use fixtures seguras desses estados ou morte real seguida de remoção | Botão/menu sem autorização; nenhum transporte, recriação ou cooldown novo |
| Repetição | Envie dois pedidos do mesmo vínculo antes de terminar a carga | Apenas uma operação e um único transporte |
| Tickets compartilhados | Dois donos deixam companheiros no mesmo chunk descarregado e chamam juntos | Encerrar um pedido não cancela o carregamento do outro; ticket removido ao finalizar ambos |
| Falha/timeout | Em harness controlado, atrase a carga além de 200 ticks ou faça-a falhar | Erro específico, cooldown intacto, fila/ticket liberados, reserva preservada |
| Troca de item/logout | Inicie carga e troque o item na mão ou desconecte antes da conclusão | Solicitação cancelada e tickets liberados, sem transporte posterior pelo pedido cancelado |
| Dimensão removida/local desconhecido | Use fixture antiga sem localização ou chave de dimensão ausente | Mensagem específica, sem varredura, substituição ou liberação do vínculo |
| Ausência de duplicação | Após cada transporte, visite origem e destino e reconecte ambos os jogadores | Uma única entidade admitida com o UUID canônico; ovo não permite invocar outra |
| Regressão | Repita voo, Follow/Stay, fuga, alerta, minérios, troca visual e recuperação do item | Funcionalidades anteriores permanecem; recuperar o item não zera recallCooldownEnd |

Transferência real entre dimensões, recuperação de chunks, limpeza de tickets no
mundo, menu em jogo e regressão multiplayer permanecem pendentes de execução ou
confirmação manual. Nenhum desses testes foi declarado aprovado apenas pelo build.
