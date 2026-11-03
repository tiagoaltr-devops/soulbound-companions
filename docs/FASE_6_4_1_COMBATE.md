# Fase 6.4.1 — Combate, modos e Ruptura Ancestral

## Diagnóstico e correções

- O botão de combate ainda era apresentado como **Seguir**; agora aparece como
  **Seguir / Combate**, distinguindo-o de Seguir (passivo). A troca de forma
  continua preservando o modo. Não foi criado Berserker.
- O atalho só era processado com `screen == null`. Pressioná-lo dentro do próprio
  menu do companheiro não enviava solicitação. O menu agora trata a tecla configurada
  pela API `KeyEvent`/`KeyMapping.matches` da 1.21.11; o fluxo normal no mundo continua
  via ClientTick. Chat e outras telas não acionam a habilidade.
- A validação exigia implicitamente Bastão na mão, retornando um erro genérico.
  Agora usa UUID do jogador da conexão, registro oficial e entidade canônica viva
  no mesmo mundo a até 32 blocos, sem exigir item na mão. O payload continua sem
  UUID/dano/alvos fornecidos pelo cliente.
- Recusas indicam Junior necessário, Ficar, Passivo, companheiro indisponível,
  transição ou cooldown restante. Feedback limitado a uma mensagem por segundo.
- HUD recebe estado e cooldown do servidor e mostra a tecla real configurada,
  habilidade pronta, bloqueio ou segundos restantes. Som da ultimate alterado
  para ressonância de ametista; sem explosão real.

Não foi reproduzida uma falha no seletor/ataque básico quando o modo real era
FOLLOW_COMBAT. O teste nativo confirmou dano em zumbi e esqueleto. Esse fluxo foi
preservado. Não há evidência suficiente para atribuir a falha específica da partida
do jogador a outra causa além das condições de uso e apresentação identificadas.

## Validação

No mundo isolado **já existente** `runs/phase611-server/phase611-homologation`,
um FakePlayer e entidades reais exercitaram IA e dano: **18 verificações aprovadas**.
Cobrem admissão canônica, Junior/combate, alvo zumbi, dano melee em zumbi e esqueleto,
ausência de ataques em Ficar/Passivo, rejeição de aldeão/animal/jogador/Soulbound,
ultimate aceita sem Bastão, dano em hostil, repetição bloqueada, prazo de 1.200 ticks,
Baby sem combate/ultimate e preservação do UUID na troca. Servidor salvo e encerrado
por `stop`. Log: `build/phase641-native.log`. Não houve teste visual por jogador.

A primeira execução do harness foi recusada pela verificação de diretório, que
precisou usar caminho absoluto; isso foi corrigido antes do resultado acima.
Uma execução inicial sem valor explícito das flags iniciou o teste antigo de
Ressonância, que passou com 286 verificações; não foi tratado como teste de combate.

Build final: **BUILD SUCCESSFUL**, **398 verificações** (68 Junior e 330 anteriores).
Log: `build/phase641-build.log`.
Foram adicionados oito casos para estados da ultimate compartilhados pelo servidor
e HUD. As verificações anteriores de vínculo, formas, filtros e cooldown permanecem.

Execução opt-in do teste nativo (somente no diretório isolado):

```powershell
.\gradlew.bat runPhase611Server -Pphase611LiveChecks=true -PcombatLiveChecks=true
```

O harness de teste não integra o JAR publicado. Não há mudança de dependências,
identidade, receitas, rituais, Forja, modelos ou worldgen nesta correção.

## Homologação manual pendente

- [ ] Junior: selecionar **Seguir / Combate** e observar aproximação, golpe/animação
  e dano em zumbi/esqueleto; conferir filtros com aliados, jogadores e animais.
- [ ] Ficar e Passivo: conferir bloqueio de melee e mensagem correspondente ao usar R.
- [ ] Pressionar a tecla no mundo e no menu do companheiro, com mãos vazias; observar
  animação, onda ciano, som e dano em área somente nos alvos válidos.
- [ ] Remapear R e conferir a nova tecla na HUD; verificar conflitos com outros mods.
- [ ] Repetir solicitação: uma execução e cooldown próximo de 60 s, decrescendo;
  trocar forma/reconectar não deve liberar antes do prazo.
- [ ] Baby, entidade indisponível e transição: mensagens específicas, sem dano.
- [ ] Confirmar Follow/Stay/Recall e identidade nos dois jogadores do teste multiplayer.

Os recursos de 6.4 listados no pedido como homologados pelo jogador foram preservados.
Os efeitos audiovisuais, tecla física/rede do cliente e apresentação da HUD ainda
exigem homologação manual; o teste nativo chamou o endpoint servidor da ultimate.

## Arquivos desta correção

`companion/JuniorCombat.java`, `CompanionFormService.java`,
`client/JuniorControls.java`, `SoulCommandScreen.java`, `menu/SoulCommandMenu.java`,
traduções pt_BR/en_US, `build.gradle` (flag do teste isolado),
`src/test/.../companion/CombatServerChecks.java` (novo), `JuniorChecks.java`,
`ritual/ResonanceServerChecks.java` (exclusão mútua dos harnesses), este guia,
manual do jogador e checklist. Sem push e sem iniciar outra fase.
