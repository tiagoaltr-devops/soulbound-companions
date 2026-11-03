# README histórico — primeiras fases

Este documento preserva o README anterior. Algumas descrições estão desatualizadas.
Consulte o [README atual](../README.md) e o [manual do jogador](MANUAL_DO_JOGADOR.md).


Soulbound Companions — Baby Flutuante
=======

Compatibilidade: Minecraft Java 1.21.11, NeoForge 21.11.48, Java 21.
Mod ID: `soulbound`. As dependências e o Gradle wrapper originais foram preservados.

A Fase 2 adiciona alertas de ameaças, fuga emergencial com retorno ao ponto de
Ficar e Instinto Mineral incremental. Consulte [Fase 2](PHASE2.md) para
configuração, arquivos alterados, limitações e testes desta fase. Os resultados
de inicialização documentados mais abaixo se referem à Fase 1.

O sistema [Soulbound Identity](SOULBOUND_IDENTITY.md) estabelece um vínculo
permanente por UUID de jogador, entrega inicial do Ovo da Alma e proteção contra
invocações duplicadas. O ovo agora permanece no inventário após a invocação.
Para recuperar somente o ovo do vínculo existente, use `/soulbound recover`.

A [Fase 4 — visual e voo Baby](PHASE4_BABY_VISUAL.md) adiciona modelo próprio,
textura original e navegação tridimensional. O Lupus continua sendo Baby, com os
mesmos UUIDs e regras de Soulbound Identity. Ficar agora mantém voo estacionário.

A [Fase 5 — Bastão da Alma](PHASE5_SOUL_STAFF.md) mantém o mesmo item e muda
sua aparência conforme o estado oficial. Com o companheiro ACTIVE, clique direito
no ar alterna Seguir/Ficar; Shift + clique direito abre o menu de comandos Baby.

A [Fase 5.1 — Chamado da Alma](PHASE51_SOUL_RECALL.md) acrescenta **Chamar
companheiro** ao menu. O chamado recupera a entidade canônica, inclusive de outro
chunk ou dimensão, retorna a Follow e tem cooldown persistente de 30 segundos.

### Executar

No PowerShell, na raiz do projeto:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

O artefato atual é `build/libs/soulbound-1.0.0.jar`. Um JAR antigo com outro
nome pode continuar em `build/libs`; use somente o artefato `soulbound` atual.

### Teste manual (mundo de teste com comandos habilitados)

1. Confirme Soulbound Companions na lista de mods. Entre no mundo e confira a
   entrega do Ovo da Alma. Para recuperar o item, use `/soulbound recover`.
   Para testes de normalização, use `/give @s soulbound:companion_egg`.
2. Use o ovo na face superior de um bloco sólido com espaço livre. Deve aparecer
   exatamente um Lupus Baby mágico flutuante; o jogador que usou
   o ovo deve ser seu proprietário. O item passa a exibir o Bastão da Alma.
3. Confira que o mesmo item permanece após invocar, tanto em sobrevivência quanto
   em criativo, e que outros usos comandam o Lupus sem criar outro. Uma posição
   bloqueada deve impedir a invocação sem alterar o estado de prontidão.
4. Caminhe, suba e desça em terreno acessível. O Lupus deve seguir em três
   dimensões e parar próximo do proprietário. Não há teleporte: obstáculos, dimensões diferentes
   e chunks descarregados podem interromper o acompanhamento.
5. Clique com o botão direito no Lupus. Confira a mensagem de Ficar e o voo
   estacionário próximo ao ponto do comando. Afaste-se e confirme que ele permanece
   no local. Clique novamente para voltar a Seguir (passivo).
6. Em multiplayer, um segundo jogador deve receber a mensagem de falta de
   permissão e não mudar o modo nem tomar posse. Confira também com itens nas
   mãos, incluindo laço, comida e outro ovo.
7. Coloque o Lupus em Ficar, salve, saia e reabra. Repita em Seguir.
   Confira proprietário e modo preservados. Para inspecionar os dados, use:
   `/data get entity @e[type=soulbound:lupus_companion,sort=nearest,limit=1]`.
   Confira `OwnerUUID`, `EvolutionStage: "BABY"`, `BehaviorMode` e vida máxima 12.
8. Aproxime jogadores e mobs, ataque o Lupus e ataque o proprietário. O Baby
   nunca deve atacar ou retaliar. Ele ainda pode sofrer dano e morrer.
9. Troque entre português brasileiro e inglês e confira os nomes e mensagens.
10. Em um servidor dedicado de teste com a mesma versão, instale o JAR e confira
    a inicialização, a invocação e o controle por dois jogadores. A entrada de
    cliente e o renderer estão isolados do código comum.

O build confirma compilação e empacotamento. Os testes de jogabilidade acima
exigem execução manual; não são considerados aprovados apenas pelo build.
Evolução, combate, transformação GOD, rituais e ressurreição não estão implementados.

### Organização

- `registry/ModEntities.java`: tipo `soulbound:lupus_companion` e atributos.
- `registry/ModItems.java`: item `soulbound:companion_egg`.
- `entity/LupusCompanionEntity.java`: dono, interação e persistência por
  `ValueInput`/`ValueOutput`, APIs da versão 1.21.11.
- `entity/ai/FollowCompanionOwnerGoal.java`: acompanhamento terrestre no servidor.
- `companion/`: somente os modos e o estágio implementados nesta fase.
- `item/CompanionEggItem.java`: invocação exclusiva por jogador no servidor,
  sem configurar spawners nem aceitar dados de entidade do item.
- `client/LupusCompanionRenderer.java`: modelo vanilla `WOLF_BABY` e render state.
- `assets/soulbound/`: traduções e definição/modelo do item.

A tradução de exemplo do template permanece sem registro ativo. `Config.java`
foi reaproveitado na Fase 2 para os parâmetros de exploração do servidor.

### Validação realizada e arquivos alterados

- Build inicial e build após implementação: `BUILD SUCCESSFUL`.
- Inicialização do cliente com o mod e carregamento dos recursos: executada.
- Inicialização do servidor dedicado: chegou a `Done`, sem carregar a entrada
  de cliente. O servidor de desenvolvimento foi encerrado após a checagem.
- Conteúdo do JAR, metadados e sintaxe dos quatro JSONs novos: conferidos.
- Invocação por console não validada: a execução Gradle neste ambiente não
  encaminhou os comandos digitados ao servidor.
- Jogabilidade, controle multiplayer e persistência após reabrir o mundo:
  pendentes dos testes manuais descritos acima.

Arquivos modificados:

- `AGENTS.md`: compatibilidade com Minecraft 1.21.11 e NeoForge 21.11.48.
- `gradle.properties`: Mod ID `soulbound`, sem mudar versões de dependências.
- `src/main/java/br/com/soulbound/SoulboundCompanions.java`.
- `src/main/java/br/com/soulbound/SoulboundCompanionsClient.java`.
- `src/main/templates/META-INF/neoforge.mods.toml`: descrição do mod.
- `README.md`: execução, limitações e roteiro de testes.

Arquivos criados (Java sob `src/main/java/br/com/soulbound/`):

- `companion/BehaviorMode.java` e `companion/EvolutionStage.java`.
- `entity/LupusCompanionEntity.java`.
- `entity/ai/FollowCompanionOwnerGoal.java`.
- `item/CompanionEggItem.java`.
- `registry/ModEntities.java` e `registry/ModItems.java`.
- `client/LupusCompanionRenderer.java`.
- `src/main/resources/assets/soulbound/lang/en_us.json`.
- `src/main/resources/assets/soulbound/lang/pt_br.json`.
- `src/main/resources/assets/soulbound/items/companion_egg.json`.
- `src/main/resources/assets/soulbound/models/item/companion_egg.json`.

As execuções de desenvolvimento também geram logs, configuração e um mundo
de servidor em `run/`, além dos artefatos em `build/`.

Installation information (template original)
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
# Fase 6 — Altar da Alma

Ruínas, provas individuais, Forja multibloco, rituais e progressão foram integrados.
O nascimento inicial e o Soul Recall continuam sem custo de XP. Veja o
[relatório de implementação](FASE_6_IMPLEMENTACAO.md) e o
[checklist de homologação](FASE_6_CHECKLIST.md), incluindo resultados dos testes
e limitações. Jogabilidade e multiplayer desta fase ainda exigem homologação.
