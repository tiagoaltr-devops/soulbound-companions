# Fase 6.3.1 — Ativação e apresentação da Forja

## Resultado técnico

O callback cliente já enviava o botão ao menu correto. O servidor recusava estado,
permissão, estrutura, oferendas ou essência retornando `false`, sem explicar o motivo.
Além disso, a implementação anterior era instantânea: não havia agendamento de
15 segundos. Não foi possível determinar qual requisito específico estava ausente
na partida relatada. As novas mensagens identificam posição, material e quantidade.

A ativação agora reserva oferendas e resultado no `soulbound_altar.dat`, por 300 ticks
do servidor. O registro mantém as reservas, autor, orientação, término e sorteio.
Cliques repetidos e transferências dos espaços reservados são bloqueados. A primeira
ativação não cobra XP, não falha e atribui o proprietário por UUID na conclusão.
Quantidades excedentes permanecem nos altares; consumo e reserva são gravados juntos.
Reativações conservam probabilidades e desgaste; a essência pode ser segurada na mão
principal ao confirmar, usando o mecanismo existente de transferência com recibos.

A HUD apresenta somente descrição/Ativar na Forja nunca ativada, progresso durante
a canalização, funcionalidades completas na ativa e requisitos/risco/Reativar na
inativa com dono. Erros aparecem na janela e no chat. O servidor valida as ações,
inclusive pacotes incompatíveis com o estado. Fechar a janela não cancela o processo.

Oferendas possuem uma BlockEntity de apresentação, sem inventário ou capabilities.
Seu snapshot conserva componentes do ItemStack, mas o SavedData permanece a autoridade.
Atualizações ocorrem na alteração do conteúdo e no carregamento, não a cada tick.
Altares antigos recebem a representação ao carregar. O renderer cliente usa
`BlockEntityRenderState`, `ItemStackRenderState` e `SubmitNodeCollector` da 1.21.11,
com giro/oscilação, sem ItemEntity. Consumo e devolução dos rituais também atualizam
essa representação, preservando seus custos e regras.

Efeitos: som de beacon no início, partículas SOUL discretas e mais intensas nos
últimos três segundos; sucesso com pulso ciano, coluna curta de almas e som mágico;
falhas com partículas roxas e vidro quebrando, mais intenso na destruição definitiva.
O resultado é persistido antes dos efeitos. Não há raio real, explosão ou dano.

## Verificação e limites

`gradlew.bat build`: **BUILD SUCCESSFUL**, **330 verificações** (144 altar,
52 Ressonância e 134 anteriores). As novas verificações exercitam oferendas,
reserva/consumo único, prazo, proprietário, persistência NBT em disco, bloqueio
simultâneo, permissões, estados da HUD, resultados únicos e traduções.
Log: `build/phase631-build.log`. Não foram iniciados mundos ou partidas nesta fase.
Esses testes não simulam cliques em rede, renderização, sons ou um reinício real.

Ativações salvas retomam o prazo do relógio do servidor após reiniciar. Não se carregam
chunks para concluir: a finalização aguarda a estrutura disponível. Se ela estiver
alterada, a reserva volta aos espaços canônicos e o jogador recebe aviso. Uma parada
entre persistir o resultado e reproduzir efeitos pode omitir o efeito, sem repetir
consumo. As garantias continuam limitadas pelas transações físicas já documentadas.

## Homologação manual

- [ ] Reiniciar com o JAR atualizado; usar a Forja fabricada com Centelha válida.
- [ ] Nunca ativada: apenas título, descrição, Ativar e Fechar; nenhum núcleo/ritual/risco.
- [ ] Testar estrutura incompleta, altar ausente e material insuficiente: verificar erro
  na janela, sem consumo. Conferir as quatro rotações, olhando da plataforma para a Forja.
- [ ] Oposto: 2 diamantes; esquerda: 8 ametistas; direita: 1 eco. Verificar itens
  flutuantes ao depositar/retirar, inclusive componentes personalizados e multiplayer.
- [ ] Confirmar: som/partículas, reserva única, contador de 15 segundos e ações bloqueadas.
  Fechar/reabrir e clicar repetidamente; não reiniciar o prazo nem consumir outra vez.
- [ ] Concluir: pulso/coluna/som uma vez, mensagem de despertar, proprietário correto,
  HUD ativa e XP preservado. Conferir `/soulbound altar inspect` e reiniciar normalmente.
- [ ] Reiniciar durante a canalização: verificar reserva e conclusão única. Desmontar
  uma pedra durante outra tentativa: verificar devolução e mensagem de cancelamento.
- [ ] Recolocar uma Forja recuperada pelo dono: HUD de reativação, essência na mão,
  riscos preservados. Homologar sucesso, falha comum e crítica em ambiente de teste.
- [ ] Conferir que outros jogadores não reativam/quebram a Forja alheia e que o
  consumo de oferendas por rituais remove os itens flutuantes. Validar Follow/Stay/Recall.

## Arquivos desta fase

Novos: `ritual/ForgeActivation`, `ritual/OfferingBlockEntity`, `client/OfferingRenderer`
e este documento. Alterados: `ForgeService`, `ForgeMenu`, `ForgeScreen`, `AltarSavedData`,
`AltarInventory`, `InventoryTransfers`, `OfferingBlock`, `RitualService`,
`RitualDefinitions`, `ModAltar`, `SoulboundCompanionsClient`, `AltarChecks`, traduções
pt_BR/en_US e checklist. Caminhos Java sob `src/main/java/br/com/soulbound`, exceto
`AltarChecks` em `src/test/java`. Alterações anteriores do workspace foram preservadas.
