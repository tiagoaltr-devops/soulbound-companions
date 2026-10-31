# Fase 5 — Bastão da Alma

Minecraft 1.21.11, NeoForge 21.11.48 e Java 21, sem alterações de versões ou wrapper.
O único item registrado continua sendo `soulbound:companion_egg`. A transformação
é uma projeção visual/funcional do mesmo vínculo, não uma troca por outro item.

Atualização: a [Fase 5.1](PHASE51_SOUL_RECALL.md) acrescenta o botão de recall e
dois DataSlots ao menu (nove no total). Seguir/Ficar continuam exigindo a entidade
carregada localmente; o Chamado da Alma pode recuperá-la de outra área/dimensão.

## Aparência sincronizada

`SoulEggIdentity.refresh` já produz a tooltip a partir do registro oficial. Agora
também escreve os componentes vanilla sincronizados `ITEM_MODEL` e `ITEM_NAME`.
`SoulItemForm` define o mapeamento:

| Estado oficial | Aparência | Definição em assets/soulbound/items |
| --- | --- | --- |
| READY | Ovo da Alma | companion_egg.json |
| SUMMONING | Ovo da Alma | companion_egg.json |
| ACTIVE | Bastão da Alma | soul_staff.json |
| DEAD | Ovo Fragmentado | shattered_egg.json |
| MISSING | Bastão Inativo | inactive_staff.json |

Os identificadores adicionais acima são definições de modelos, não novos itens
registrados. Cada definição usa o sistema atual `minecraft:model`, referenciando
um modelo gerado ou handheld. Não há predicates/overrides de modelos legados.

As quatro texturas PNG RGBA 16×16 são originais, desenhadas por
`tools/New-SoulItemTextures.ps1`. O ovo tem tons de azul e uma marca mágica; o bastão
tem haste diagonal e gema verde-água; o ovo fragmentado tem rachadura; o bastão
inativo usa tons acinzentados. Nenhuma imagem externa é lida pelo script.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/New-SoulItemTextures.ps1
```

A sincronização anterior continua: atualização no login, recuperação, uso e
alterações já notificadas de estado/modo, com reconciliação periódica do inventário
em até 20 ticks. SUMMONING pode durar menos de um tick de sincronização e, por
isso, não precisa aparecer como uma etapa visual separada. Seu modelo é o ovo.
Nomes administrativos personalizados podem se sobrepor ao nome visual normal;
nome/modelo/lore nunca concedem autorização.

## Comandos e autorização

Com o vínculo ACTIVE e o bastão na mão:

- Clique direito no ar alterna FOLLOW_PASSIVE e STAY.
- Shift + clique direito abre o menu de comandos.
- O item também encaminha essas ações ao usá-lo num bloco, quando a interação
  vanilla com o próprio bloco não tiver prioridade. Ao clicar no próprio Lupus
  com o item, usa-se o mesmo caminho de comando; interação sem o item permanece.
- Uma mudança efetiva mostra o modo na actionbar, toca som próximo do jogador
  e emite partículas no companheiro. A tooltip é atualizada pela sincronização
  oficial existente. Há intervalo mínimo de 6 ticks entre comandos pelo bastão.

`SoulStaffAccess` e `SoulStaffCommands` verificam o jogador real do servidor,
credenciais de proprietário/vínculo no item, registro canônico ACTIVE, ausência
de exigência de ressurreição e suporte ao Lupus Baby. A entidade é obtida pelo
`companionUuid` oficial e novamente validada com ownerUuid/bondUuid/UUID e
`SoulboundIdentity.isCanonical`. Item adulterado só para parecer um bastão não
passa nessa validação.

Os comandos precisam da entidade carregada na mesma dimensão do dono. A consulta
é apenas ao mapa de entidades carregadas, sem carregar chunks, procurar no disco,
teleportar ou criar companheiro. Se estiver ausente, o comando é recusado com
feedback. O registro continua ACTIVE e sua reserva permanece ocupada.

A aplicação do modo chama `LupusCompanionEntity.commandBehaviorMode`, que valida
novamente o dono e usa a transição Follow/Stay já existente. Ovos, UUIDs, progresso,
cooldown GOD, espécie e estágio não são recriados. Solicitar o mesmo modo é
idempotente: não redefine a âncora de Ficar.

## Menu Baby

`SoulCommandMenu` é um menu vanilla sem slots de inventário, aberto pelo servidor
com `SimpleMenuProvider`. `SoulCommandScreen` é registrada apenas no cliente.
Ela apresenta:

- Nome do companheiro no cabeçalho, obtido do servidor na abertura.
- Espécie, estágio, estado e modo atual do registro.
- Disponibilidade local para comandos.
- Botões Seguir e Ficar; o botão do modo já selecionado fica desabilitado.
- Indicadores informativos de Alerta de Ameaças e Instinto Mineral, mostrando
  disponibilidade e sinais recentes. Esses indicadores não ligam/desligam as
  habilidades nem alteram buscas. “Ativo” indica habilidade disponível no Baby,
  não uma promessa de que uma busca esteja executando naquele instante.

Sete valores pequenos são enviados do servidor por DataSlots vanilla. O nome é
o título enviado na abertura. Os botões usam o pacote vanilla de clique em menu,
com `containerId` e um ID de botão (0 = Seguir; 1 = Ficar). Não existe payload que
aceite do cliente o UUID de um proprietário ou de uma entidade-alvo.

O processamento vanilla confere a janela aberta e chama `stillValid`. O menu
também verifica o jogador dono da sessão, a mão que abriu o menu, o item ainda
autorizado e os três UUIDs capturados no servidor. Cada clique revalida o estado
canônico e aceita somente os dois botões previstos. O serviço de comandos faz a
checagem final. Remover/trocar o item ou perder a autorização invalida o menu.

Com um ACTIVE descarregado/em outra dimensão, o menu pode abrir mostrando o
registro, mas os comandos ficam desabilitados e os indicadores sem leitura local.
Se a entidade voltar a estar disponível na mesma dimensão, os dados periódicos
do menu refletem isso sem criar outra entidade.

## Estados especiais e compatibilidade

DEAD exibe Ovo Fragmentado e bloqueia comandos; MISSING exibe Bastão Inativo e
também bloqueia comandos. Um menu aberto perde validade quando o registro deixa
de autorizar comandos. A atualização de morte continua ocorrendo na remoção
definitiva da entidade, conforme Soulbound Identity, sem alterar o fluxo anterior.

Nesta Fase 5 original não foram adicionados altar, ressurreição, evolução ou
recall; o recall de ACTIVE foi adicionado posteriormente na Fase 5.1. `/soulbound recover`
continua recuperando somente o item do vínculo existente e aplica a aparência do
estado atual. Recuperar um item DEAD/MISSING não muda seu estado nem libera uma
invocação. A proteção contra duplicação/drop/transferência continua associada à
mesma classe e ao mesmo item registrado.

Não foram alterados as metas de voo, Follow, Stay, fuga, recuo, alerta ou a busca
mineral. O novo setter de comando encaminha para o comportamento existente. O
pacote de persistência/registro da identidade e seu schema não foram modificados.

Limitações: comandos não ficam enfileirados para entidades descarregadas; o título
do menu é o nome no momento da abertura; uma interface pode mostrar dados antigos
por alguns ticks enquanto chega a sincronização. A autorização não depende desses
dados visuais. Administradores e outros mods continuam fora das garantias de
proteção absoluta já documentadas no guia de identidade.

## Arquivos desta fase

Criados sob `src/main/java/br/com/soulbound/`:

- `item/SoulItemForm.java`.
- `companion/SoulStaffAccess.java` e `companion/SoulStaffCommands.java`.
- `menu/SoulCommandMenu.java`.
- `client/SoulCommandScreen.java`.
- `registry/ModMenus.java`.

Modificados sob o mesmo diretório:

- `item/SoulEggIdentity.java`: projeção de modelo/nome oficial.
- `item/CompanionEggItem.java`: comando no ar, menu e encaminhamento de ACTIVE.
- `entity/LupusCompanionEntity.java`: entrada validada para o modo existente e
  encaminhamento de interações com o bastão.
- `SoulboundCompanions.java` e `SoulboundCompanionsClient.java`: registros do menu
  e da tela nos lados apropriados.

Recursos criados em `src/main/resources/assets/soulbound/`:

- `items/{soul_staff,shattered_egg,inactive_staff}.json`.
- `models/item/{soul_staff,shattered_egg,inactive_staff}.json`.
- `textures/item/{soul_egg,soul_staff,shattered_egg,inactive_staff}.png`.

Recursos modificados: `models/item/companion_egg.json`, `lang/pt_br.json` e
`lang/en_us.json`. Criados também o script `tools/New-SoulItemTextures.ps1`, este
guia e `src/test/java/br/com/soulbound/companion/SoulStaffChecks.java`.
`build.gradle` ganhou a task de verificações sem novas dependências; `README.md`
foi atualizado. Alterações anteriores do workspace foram preservadas.

## Validação executada

`./gradlew.bat clean build`: **BUILD SUCCESSFUL**.

- 24 verificações anteriores de identidade.
- 20 verificações anteriores do visual Baby.
- 41 novas verificações de política de acesso, preservação de campos, mapeamento
  dos cinco estados e referências reais entre definições/modelos/texturas PNG,
  além dos nomes e controles localizados.

O primeiro build encontrou um tipo incorreto na conversão de texto recortado da
tela; foi corrigido usando a ordenação visual de texto da API atual. O build final
não tem erro de compilação. Permanecem avisos de APIs depreciadas já utilizadas
pelo item e avisos do Gradle, sem mudar dependências para eliminá-los.

O servidor dedicado foi iniciado até `Done`, sem erro observado de classes de
cliente. O cliente também iniciou e carregou os recursos sem erro observado no
log. Ambos foram encerrados após a checagem. Isso não comprova uso da tela,
comandos em mundo ou estabilidade multiplayer; os testes manuais abaixo estão
pendentes de execução/confirmação.

## Roteiro de testes manuais

1. Execute `./gradlew.bat runClient`. Em mundo de teste novo, receba o ovo e confira
   sua textura e tooltip. Invoque o Lupus: o mesmo item deve virar Bastão da Alma.
   Compare ownerUuid, bondUuid, companionUuid, espécie e progresso antes/depois.
2. Com o bastão na mão, clique direito no ar sem Shift. Confira alternância,
   actionbar, som, partículas e modo na tooltip. Repita com mão secundária e ao
   clicar no Lupus. Aguarde pelo menos 6 ticks entre mudanças deliberadas.
3. Use Shift + clique direito para abrir o menu. Confira nome, espécie, Baby,
   ACTIVE e modo. Use Seguir/Ficar e confirme o comportamento em mundo. O botão
   já selecionado deve ficar desabilitado e não redefinir a âncora.
4. Com menu aberto, aproxime uma ameaça e aguarde uma detecção mineral: os
   indicadores devem refletir os sinais recentes sem controlar as habilidades.
5. Troque/remova o item enquanto houver uma sessão aberta (por comandos de teste
   ou pacote controlado). Tente reutilizar cliques da janela antiga: devem ser
   recusados. Botões inválidos também não devem produzir alterações.
6. Com dois jogadores, tente usar o item de A como B, abrir comandos de A por B
   e repetir cliques de outra sessão. Nenhum modo ou dado do vínculo de A pode
   ser alterado por B. Uma aparência de bastão forjada não deve autorizar comandos.
7. Descarregue a região do companheiro ou vá para outra dimensão. O item deve
   continuar ACTIVE; comandos devem ser recusados e o menu mostrar indisponibilidade
   local. Volte à região e confira a disponibilidade sem nova invocação.
8. Mate o companheiro em um mundo de teste e aguarde sua remoção definitiva:
   deve aparecer Ovo Fragmentado, sem Follow/Stay. Recupere o item com o comando:
   ele deve continuar DEAD, com os mesmos UUIDs e exigência de ressurreição.
9. Em cópia de teste do mundo, prepare uma fixture MISSING com o servidor fechado:
   ao entrar, confira Bastão Inativo e comandos bloqueados, sem recriação automática.
   Para SUMMONING, confira a projeção de ovo e bloqueio de comandos/invocação.
10. Obtenha novas cópias no Criativo ou por `/give @s soulbound:companion_egg`.
    Confira normalização para o estado/progresso oficial, consolidação das cópias,
    proteção contra duplicação e `/soulbound recover`.
11. Reinicie o servidor e reconecte. Confira aparência, tooltip, modo e UUIDs.
    Repita voo, cavernas/florestas, fuga e minérios como regressão das funcionalidades
    já testadas anteriormente. Troque o idioma entre português brasileiro e inglês.

Nenhum teste de jogabilidade desta fase foi declarado aprovado sem execução real.
