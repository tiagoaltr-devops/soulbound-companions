# Fase 6.10.4.1 — Inventário e talentos no Grimório

Minecraft 1.21.11 / NeoForge 21.11.48 / Java 21. Esta fase substitui os
redirecionamentos das abas Inventário e Talentos descritos na Fase 6.10.4.
Não altera itens registrados, balanceamento, atributos, custos, modelos ou codecs
persistentes. Não foram realizados commits ou pushes.

## Um menu e uma tela

`SoulCommandMenu` agora estende `CompanionInventoryMenu`, usando seu construtor
protegido com o tipo de menu já registrado do Grimório. Uma sessão possui
**45 slots reais: nove do companheiro e 36 do jogador**, incluindo a hotbar.
As sete abas permanecem no mesmo `GrimoireScreen` e no mesmo `containerId`.
Não se abrem as telas antigas ao navegar ou investir pontos.

Os botões de abas enviam IDs vanilla ao servidor. A aba selecionada é sincronizada
por `DataSlot`; o cliente pode antecipar sua apresentação, mas somente o servidor
autoriza movimentações. Os IDs antigos de Inventário/Talentos passam a selecionar
as abas correspondentes. O payload antigo de retorno permanece restrito às telas
legadas e não reabre uma sessão que já esteja no Grimório unificado.

## Inventário integrado

Os mecanismos originais de snapshot, tipos permitidos, captura de desgaste,
commit no `SoulboundSavedData`, gravação verificada e projeção dos equipamentos
continuam no `CompanionInventoryMenu`. Não existe um segundo armazenamento.

Foram separados os conceitos de validade informativa do hub e autorização física
do inventário. Um hub remoto pode continuar aberto, mas movimentar itens exige
aba Inventário ativa, dono vivo/não espectador, sessão válida, companheiro canônico
vivo na mesma dimensão, distância máxima de oito blocos e ausência de ritual.
Essas condições são reavaliadas no clique e no shift-click, independentemente do
estado visual dos slots ou de pacotes enviados pelo cliente.

Equipamentos e alimentos ficam em uma linha superior; inventário e hotbar do
jogador ficam abaixo. O layout reposiciona slots mantendo containers e índices.
As quatro armaduras possuem placeholders vanilla; itens e slots vazios têm
tooltips. Encantamentos, componentes, contagens e durabilidade usam os `ItemStack`
e a serialização já existentes. Há predição visual vanilla no cliente; ela não
executa o commit nem confere autoridade. O servidor valida e reconcilia o resultado.

Ao trocar de aba, os slots ficam inativos e o estado de arraste distribuído
(`quick-craft`) é cancelado. **O item no cursor não é removido ou transferido.**
Não há fechamento de container nem emissão de drops para realizar a navegação.
A chave do vínculo pode ficar na mão, no cursor ou no inventário do proprietário
sem encerrar a sessão ao ser reorganizada. Comandos que exigem a mão específica
mantêm suas validações anteriores: coloque o Grimório novamente na mão para usá-los.

Fora da aba Inventário, teclas 1–7 continuam navegando. Dentro dela, as teclas
numéricas ficam disponíveis ao comportamento vanilla da hotbar; use a sidebar
para trocar de aba. Setas paginam textos onde houver páginas.

## Árvore integrada

`TalentMenu.sync` e `TalentMenu.invest` concentram a sincronização e a chamada
de investimento reutilizadas tanto pelo menu legado quanto pelo Grimório.
`TalentDefinitions` e `ProgressionService` mantêm todas as regras de orçamento,
pré-requisitos, máximo, forma desbloqueada, nível e persistência. O cliente recebe
ranks/motivos de recusa e não calcula uma autorização independente.

A aba apresenta cinco ramos, os nós de cada ramo e conexões dos pré-requisitos
explícitos. Nós investidos, disponíveis e bloqueados têm estados visuais distintos;
qualquer nó pode ser selecionado para consulta. O painel à direita mostra nome,
rank/máximo, descrição/efeitos existentes, requisitos, custo de **um ponto por rank**
e motivo de bloqueio. Nível e pontos disponíveis ficam visíveis acima da árvore.

O botão de investimento envia somente o índice da definição ao menu atual.
O servidor exige a aba Talentos, revalida proprietário/sessão e delega à progressão
existente. A resposta atualiza pontos, ranks, motivos e atributos sem trocar de
container. Detalhes longos são paginados; os ramos atuais possuem até cinco nós,
que cabem na área reservada. Não foi criado outro registro de talentos ou pontos.

## Arquivos desta retomada

- `menu/CompanionInventoryMenu.java`: construtor reutilizável, slots com ativação,
  autorização física, predição vanilla e coordenadas de apresentação.
- `menu/SoulCommandMenu.java`: herança do inventário, aba sincronizada, dados de
  talentos e navegação/investimento no mesmo container.
- `menu/TalentMenu.java`: sincronização e investimento compartilhados.
- `client/GrimoireScreen.java`: inventário nativo, árvore, conexões, detalhes,
  tooltips e navegação dentro da identidade visual existente.
- `network/GrimoireOpenPayload.java`: retorno legado não reabre o hub integrado.
- Traduções `pt_br.json` e `en_us.json`: custo, rótulo de equipamentos e orientação.
- `src/test/.../client/GrimoireChecks.java`: geometria dos 45 slots e codec real
  dos IDs de investimento no pacote vanilla.
- `src/test/.../companion/UnifiedGrimoireChecks.java`: nova verificação dedicada.
- `src/test/.../companion/Phase69ServerChecks.java`: chamada da nova verificação.
- Este relatório, manual, README e nota de atualização no relatório 6.10.4.

Caminhos Java principais relativos a `src/main/java/br/com/soulbound/`.
As modificações da Fase 6.10.4 já presentes no workspace foram preservadas.

## Validação executada

- `build --console=plain`: suíte automatizada existente, mais verificações de
  apresentação integradas ao conjunto do Grimório, agora com **340 verificações**.
- Geometria: 45 slots dentro da área útil e sem sobreposição, em seis viewports,
  incluindo 320×180 e 320×240. Codec vanilla de clique de botão preserva o ID do
  último talento, que é maior que 127 na numeração adotada.
- Servidor dedicado isolado: **28 verificações novas do menu unificado**,
  **43 do Grimório/ataque direcionado**, **23 de itens** e **54 regressões de
  equipamentos/sobrevivência/God**. Houve save e encerramento normais.
- Casos novos: 45 slots reais, mesmo container ID, shift-click, pickup, quick-craft
  interrompido, troca de abas com cursor, cliques forjados em slots ocultos,
  investimento com atualização de ranks/pontos, orçamento esgotado, terceiros,
  sessão desatualizada, afastamento e o próprio Grimório no cursor.
- Reinício real do mesmo servidor isolado: verificações existentes de snapshot
  canônico e cooldown do Recall, permitindo atualização legítima de localização.

Logs locais: `build/grimoire-integrated-build.log`,
`build/grimoire-integrated-native.log` e `build/grimoire-integrated-restart.log`.
Artefato: `build/libs/soulbound-1.0.0.jar`. Use o mesmo JAR em cliente e servidor.

Esses testes exercitam as operações nativas no servidor e verificam geometria
sem GPU. Não homologam aparência, conexão de dois clientes, latência ou gestos
reais de mouse. O fechamento definitivo mantém o tratamento vanilla do cursor;
as limitações anteriores de transação entre SavedData/playerdata/chunks e crash
continuam. Não há garantia contra edição externa ou rollback seletivo de arquivos.

## Homologação manual pendente

1. Abra Inventário e Talentos: confirme que cabeçalho, sidebar e fundo permanecem
   no Grimório, sem mostrar as telas antigas.
2. Teste os nove slots, arrastar/soltar, shift-click, atalhos da hotbar, tooltips,
   itens encantados, danificados e com componentes customizados.
3. Troque várias vezes de aba com alimentos, arma e o próprio Grimório no cursor;
   confira quantidades ao retornar e ao fechar.
4. Inicie um arraste distribuído e troque de aba: nenhuma stack deve duplicar,
   desaparecer ou ser aplicada a slots ocultos.
5. Afaste-se além de oito blocos, descarregue o companheiro ou inicie ritual:
   informação permanece acessível; movimentação física é recusada.
6. Com dois clientes, tente acessar o vínculo do outro; confirme recusa de slots
   e investimento e ausência de alterações cruzadas.
7. Selecione cada ramo e seus nós; confira conexões, estados e descrições. Invista,
   esgote pontos e tente talentos bloqueados/máximos: somente operações válidas passam.
8. Teste escalas 2, 3 e 4, janela pequena, navegação e paginação de detalhes;
   verifique leitura, ausência de clipping e alinhamento dos hitboxes dos slots.
9. Reentre/reinicie após equipar, alimentar e investir; confira vínculo, UUIDs,
   componentes, ranks e pontos preservados.
10. Confira formas, God, Recall, ataque direcionado e demais abas depois da navegação.

Não foi declarada homologação visual ou multiplayer real nesta fase.
