# Fase 6.6 — Complemento de HUD

Painel cliente de até 180×62 pixels lógicos, inicialmente em (6,6), com nome,
forma ativa, nível, coração vanilla, HP numérico/barra vermelha e XP/barra ciano.
Usa GuiGraphics e RegisterGuiLayersEvent da NeoForge 21.11.48. Não substitui
barras do jogador nem modifica menus, combate, atributos, XP ou rituais.

## Dados e sincronização

`CompanionHudSync` consulta o registro canônico e procura somente o UUID reservado
nos índices das dimensões carregadas. Não carrega chunks nem varre entidades.
HP atual/máximo vêm diretamente da entidade confirmada, incluindo modificadores
de forma/talentos. Nível, XP e forma vêm do SoulboundRecord existente.

Uma checagem a cada 2 ticks (até 10 Hz a 20 TPS) compara snapshots imutáveis.
Somente mudanças enviam `CompanionHudPayload` ao proprietário. Login, respawn e
troca de dimensão forçam atualização; logout limpa os caches. O pacote é apenas
servidor → cliente, sem mecanismo de alteração da identidade ou pedido por frame.
Latência normal de atualização é de até 2 ticks, além da rede.

O cliente guarda uma projeção transitória ligada à conexão e ao UUID do jogador.
Não persiste outro registro, não lê entidades clientes e não recalcula atributos.
Desconectar limpa os dados; outra conexão não reutiliza o snapshot anterior.

## Estados e posicionamento

- ACTIVE com entidade carregada: HP real, inclusive durante dano, cura, regeneração,
  Vitalidade e transformação. HP exibido é limitado ao máximo apenas na projeção.
- DEAD: Morto, 0 HP e barra vazia; nível/XP preservados. Não consulta entidade morta.
- READY: Não invocado, sem inventar HP. Após ressurreição permanece assim até invocar.
- SUMMONING/MISSING: mensagens próprias, sem HP presumido.
- ACTIVE descarregado: mensagem Companheiro descarregado, sem reutilizar HP antigo.
- Nível 30: MAX e barra de XP cheia, sem divisão por zero.

Não há máximo de vida confiável persistido no registro. Por isso, estados sem
entidade confiável não inventam esse máximo; o morto mostra 0 HP, não 0/12 ou 0/24.
Baby/Junior usam a mesma progressão, mas cada snapshot contém a vida real da forma.

A largura/posição usam coordenadas da GUI. Boss bars que cruzam a região deslocam
o painel para baixo, sem modificar a camada vanilla. Menus, F1, F3 e Tab ocultam
o painel. Viewports sem espaço seguro também o ocultam para preservar barras do
jogador. Outros mods que desenhem minimapas/painéis nessa região precisam de
checagem manual; não há reposicionamento universal de HUDs de terceiros.

## Validação

`gradlew.bat build`: **BUILD SUCCESSFUL**, **566 verificações**, sendo 60 novas
de HUD e 506 anteriores. Log: `build/phase66-hud-build.log`. Foi corrigida uma
incompatibilidade inicial de `FormattedText` para `FormattedCharSequence`, usando
o mesmo padrão de ordenação visual já empregado nos menus.

Novos testes cobrem invocação/HP, dano e cura projetados, alteração de máximo,
Baby/Junior, XP/level-up, MAX, DEAD, READY após recuperação, descarregamento,
valores inválidos, limites das barras, nomes longos, snapshots iguais e roundtrip
do codec de rede em 12 estados. Não simulam conexão real ou renderização OpenGL.
Não foram executados cliente, servidor, testes de jogabilidade ou worldgen nesta fase.

## Checklist no Minecraft

- [ ] Entrar/relogar: conferir nome, forma, nível, XP e ausência de dados de outro mundo.
- [ ] Invocar Baby; tomar dano e curar/regenerar: conferir HP numérico e barra.
- [ ] Investir em Vitalidade; alternar Baby/Junior: conferir máximo e proporção reais.
- [ ] Ganhar XP por combate e subir de nível: conferir novo XP necessário e barra.
- [ ] Testar vínculo de nível 30: MAX, barra cheia e ausência de erro.
- [ ] Morrer: Morto/0 HP, mantendo nível/XP; ressuscitar: Não invocado; invocar:
  Baby com HP real e progressão anterior.
- [ ] Descarregar o companheiro/trocar dimensão: não mostrar HP antigo como atual.
- [ ] Testar F1/F3/Tab, menus, boss bars, escalas de GUI e resolução 320×240 lógica.
- [ ] Multiplayer: cada jogador vê somente seu vínculo; conferir minimapas de terceiros.

## Arquivos

Novos: `network/CompanionHudData.java`, `CompanionHudPayload.java`,
`CompanionHudSync.java`, `client/CompanionStatusHud.java`,
`src/test/.../companion/CompanionHudChecks.java` e este documento.
Java principal sob `src/main/java/br/com/soulbound`.

Alterados: `SoulboundCompanions.java`, `SoulboundCompanionsClient.java`
(registros de eventos/payload/camada), traduções pt_BR/en_US, `AltarChecks.java`
(execução das verificações), manual e checklist. Alterações anteriores preservadas.
Sem dependências novas, push ou implementação de outras funcionalidades da Fase 6.6.
