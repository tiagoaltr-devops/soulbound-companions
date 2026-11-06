# Arquitetura do Soulbound para futuras espécies

Inspeção local em 05/11/2026. Nenhum código de produção foi alterado. Os caminhos abaixo são relativos à raiz do projeto; as classes Java ficam em `src/main/java/br/com/soulbound/`. CONFIRMADO significa leitura direta do código; INFERIDO indica proposta arquitetural, sem implementação/teste dessa proposta.

## Estrutura e versões

CONFIRMADO — `gradle.properties`: Minecraft `1.21.11`, NeoForge `21.11.48`, mappings `2025.12.20`. `build.gradle`: Java 21 e ModDevGradle `2.0.148`. Fontes comuns em `src/main/java`, assets/dados em `src/main/resources`, testes em `src/test/java`. A árvore já contém alterações locais anteriores a esta análise; elas foram preservadas. O analisador é um programa Java independente, fora dos source sets do mod.

## Componentes encontrados

| Área | Evidência direta | Reutilização e acoplamento |
|---|---|---|
| Entidade | `entity/LupusCompanionEntity.java`: `createAttributes`, `registerGoals`, `createNavigation`, `customServerAiStep`, `addAdditionalSaveData`, `readAdditionalSaveData` | Uma entidade concentra formas, atributos, inventário, ações e controladores Lupus. Não constitui uma base genérica pronta para espécies externas. |
| Registros | `registry/ModEntities.java`: `LUPUS_COMPANION`, `SPIRIT_GUARDIAN`, `registerAttributes` | Registro de EntityTypes NeoForge reutilizável como padrão. Não há fábrica/registro genérico de espécies. SpiritGuardian é entidade auxiliar, não uma segunda espécie selecionável no vínculo. |
| Atributos | `LupusCompanionEntity.createAttributes`; `ritual/CompanionEvolution.java`; `companion/CompanionEquipment.java`; `companion/progression/TalentEffects.java` | Separar base, efeitos de forma, equipamento e talentos na futura especificação. Não importar valores de boss diretamente como progressão de companheiro. Métodos recebem Lupus. |
| Combate | `companion/JuniorCombat.java`: `enabled`, `target`, `hit`; `entity/ai/JuniorCombatGoal.java`; `entity/ai/PlenoCombatController.java` | Política de alvo, hooks de dano/encantamento e autoridade de servidor são referências reaproveitáveis. Implementações concretas dependem de Lupus, ações numéricas, formas e timings próprios. |
| Habilidades | `companion/CompanionArchetype.java`: record `Ability`, constante `ANCESTRAL_RUPTURE`; `PlenoCombatController`; `companion/GodRules.java`, `GodFormService.java` | Existe um descritor de habilidade, mas não foi encontrado um registro extensível completo de habilidades/espécies. Não tratar propostas JSON como configurações carregadas pelo jogo. |
| IA | `entity/ai/`: `FollowCompanionOwnerGoal`, `ReturnToStayGoal`, `BabyHoverGoal`, `BabyFleeThreatGoal`, `ThreatRecoilGoal`, `DefensiveRetreatGoal`, `RitualFocusGoal`, `JuniorCombatGoal` | Seguir/dono/ficar/ritual/defesa são conceitos reutilizáveis. As implementações e prioridades são configuradas por `LupusCompanionEntity.registerGoals` e recebem essa classe. |
| Navegação | `LupusCompanionEntity.createNavigation`, `updateMovement`, `travel` | Baby usa voo; formas de combate usam chão conforme `CompanionArchetype.form`. Escolha de anatomia/movimento externa exige adaptar dimensões, controles e navegação. |
| Visual | `client/LupusCompanionRenderer.java`: `getTextureLocation`, `submit`, `extractRenderState`; `SoulboundCompanionsClient.java`: `registerLayers`, `registerRenderers` | Renderer alterna `BabyCompanionModel`, `IrushiaJuniorModel`, `BramblebackPlenoModel`, `GodPrimeModel`. Isolar sempre essas classes no cliente. |
| Animação | `client/BabyCompanionModel.java`: `setupAnim`, `ModelPart`, `Mth.sin`; modelos Junior/Pleno/God | Animação Java procedural via API vanilla e estado do renderer. Não foi identificada dependência GeckoLib no caminho visual inspecionado. JSON `.animation.json` externo não funciona automaticamente aqui. |
| Formas | `companion/CompanionForms.java`: `CODEC`, `LEGACY`, `migrate`; `CompanionFormService`; `GodState` | Estados persistidos e transições úteis como contrato; valores legados e decisões de forma ainda assumem `soulbound:lupus`, BABY/JUNIOR/PLENO/GOD. GOD é transformação temporária, não evolução permanente independente. |
| Progressão | `companion/progression/`: `ProgressionState`, `ProgressionService`, `TalentDefinitions`, `TalentEffects` | Identificadores de talentos e codec são estáveis. `TalentDefinitions.t` fixa arquétipo `soulbound:lupus`; efeitos usam entidade concreta. Espécies novas exigem definições e balanceamento próprios. |
| Inventário | `CompanionEquipment`, `SurvivalState`, `menu/CompanionInventoryMenu`, `client/CompanionInventoryScreen` | Fluxo captura/aplica equipamentos e sincroniza estado. `CompanionEquipment.apply/capture` recebe Lupus; slots/anatomia de cada criatura exigem revisão. |
| Grimório/rede | `client/GrimoireScreen`, `companion/GrimoireSnapshot`, `SoulStaffCommands`, `network/GrimoirePayload`, `GrimoireOpenPayload` | UI deve consumir snapshots; comandos devem continuar autorizados no servidor. Não incorporar lógica/renderers de terceiros na camada comum. |
| Ataque direcionado | `companion/target/TargetCommandActor`, `TargetCommandService`: `request`, `legal`, `authorized`, `tick`; `network/TargetCommandPayload` | Já existe uma interface de ator, uma oportunidade real de extensão. O serviço contém resolução/validações ligadas ao companheiro canônico e precisa ser revisado antes de aceitar nova espécie. |

## Persistência canônica: preservar o contrato

CONFIRMADO — `companion/identity/SoulboundRecord.java` declara o estado imutável com `ownerUuid`, `bondUuid`, `companionUuid`, `species`, estágios, modo, status, recall, formas, progressão e sobrevivência. `CODEC`/`BASE_CODEC` definem a serialização. Componentes do ovo e NBT da entidade são projeções; não substituem o registro.

CONFIRMADO — `SoulboundSavedData.java` usa `SavedDataType("soulbound_identity", ..., CODEC)` no armazenamento do overworld. O codec rejeita UUIDs duplicados. `get` exige thread de servidor e recusa reinicializar silenciosamente um arquivo existente inválido. `put` protege vínculo permanente, UUID reservado e unicidade global; `flush`/`verifyDisk` verificam persistência.

CONFIRMADO — `SoulboundIdentity.java` coordena emissão, adoção legada, ativação e validação canônica. A validação de entidade contém `record.species().equals("LUPUS")`; `SoulboundRecord.create` cria LUPUS/BABY e `canSummon` exige LUPUS. `CompanionForms.LEGACY/migrate` fixa `soulbound:lupus`. Um campo textual `species` já existe, mas isso não equivale a suporte funcional a espécies genéricas.

## Proposta futura, sem refatoração nesta entrega

INFERIDO — Os contratos de identidade, autoridade de servidor, progressão, inventário e comando podem orientar uma implementação própria. Antes de adicionar espécies seria necessário projetar uma definição de espécie que associe EntityType, atributos, formas, habilidades, movimento e apresentação; adaptar consumidores que exigem Lupus; e testar unicidade, recall, equipamento e evolução em multiplayer/servidor dedicado. Qualquer alteração do codec ou migração exige um trabalho separado e testes de mundos existentes.

Não executar propostas geradas, substituir Lupus, mudar o registro canônico ou copiar código/assets externos. A versão Cataclysm encontrada é de Minecraft 1.21.1, diferente do alvo 1.21.11. A compatibilidade de APIs não foi presumida nem validada executando o mod.
