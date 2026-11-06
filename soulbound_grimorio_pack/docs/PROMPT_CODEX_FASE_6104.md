# CODEX — Soulbound Companions — Fase 6.10.4: Grimório da Alma e Ataque Direcionado

## Ambiente e contexto
Minecraft Java 1.21.11, NeoForge 21.11.48, Java 21; mod id `soulbound`, package `br.com.soulbound`. Fases 6.9 e 6.10 implementadas; preservar identidade canônica, inventário de nove slots, atributos, talentos, habilidades, rituais, God, modos e persistência. **Não criar um novo sistema paralelo de progressão ou inventário.**

## Insumos visuais
O pacote anexo inclui texturas PNG em `assets/soulbound/textures/gui/grimoire/` e o item `assets/soulbound/textures/item/soul_grimoire.png`. São assets de partida para um tema RPG escuro, roxo/dourado. A prévia de layout é apenas referência visual; **não usar a imagem de conceito como GUI pronta**. A GUI deve ser composta de componentes funcionais, escaláveis e clicáveis. As texturas de painéis têm dimensões variadas; implementar renderização nine-slice para que cantos e bordas não sejam deformados. Usar `RenderType`/GUI rendering compatível com a versão do NeoForge. Evitar hardcode de valores de gameplay mostrados em mockups.

## 0. Auditoria antes de editar
1. Examinar classes do Soul Staff/Bastão da Alma, fluxo ovo→item, registro de itens, receitas, menus, telas, pacotes de rede, atalhos, HUD e persistência.
2. Localizar sistemas reais de inventário, atributos, talentos, habilidades, status, modo e Recall. Identificar pontos de integração sem duplicar lógica.
3. Localizar comandos de combate e critérios atuais de seleção automática de alvos, proteção PvP/equipes e permissões.
4. Inspecionar mudanças locais do Git e preservar alterações não relacionadas. Relatar o plano e executar em etapas; não fazer commit/push.

## 1. Item Grimório da Alma
- Após a primeira invocação pelo ovo vinculado, substituir a entrega do Bastão da Alma por **um Grimório da Alma** vinculado ao mesmo proprietário e identidade canônica.
- Preservar mecanismo de recuperação do item e impedir que outro jogador controle o companheiro com o grimório obtido por troca, drop, cópia ou comando.
- Considerar mundos legados com Soul Staff: criar migração segura e idempotente do item antigo para o novo (sem duplicação), mantendo vínculo e progresso. Não apagar itens em inventários/containers sem tratamento explícito; documentar estratégia e casos limites.
- Item deve usar a textura `soul_grimoire.png` com model JSON compatível. Se o modelo de item ou item registration atual usar outro caminho, ajustar com cuidado.
- Clique direito com o Grimório abre o menu principal do companheiro; não exigir proximidade para telas informativas, mas **preservar validações de distância, vida e propriedade para operações físicas de inventário**, conforme regras já existentes.
- Permitir recuperar Grimório perdido via mecanismo existente de recuperação, sem conceder segundo companheiro.

## 2. GUI principal: hub RPG moderno
Implementar tela principal com navegação lateral e painel central, tema dark fantasy, roxo/ciano/dourado, escalável a resoluções e GUI scales diferentes. Abas:
1. **Visão Geral:** espécie, nome, forma, nível, XP, HP, modo, status, renderização do companheiro se viável sem risco de crash; fallback a ícone.
2. **Inventário:** abrir/reutilizar o menu servidor existente de nove slots (4 armaduras, 1 arma, 4 alimentos), preservando lógica de clique, shift-click, permissões e distância.
3. **Build/Talentos:** apresentar e integrar árvore, pontos disponíveis, pré-requisitos e aplicação server-authoritative; não criar pontos ou efeitos novos.
4. **Habilidades:** listar habilidades desbloqueadas, cooldowns e descrição, usando dados reais do mod.
5. **Status:** atributos base, modificadores, regeneração e bônus efetivos, sem valores fictícios.
6. **Comandos:** Seguir/Combate, Ficar, Passivo, Recall e estado de alvo direcionado; reutilizar regras existentes.
7. **Vínculo:** apresentar informações realmente persistidas (dono, data/forma/histórico quando existentes); informações ainda não implementadas devem ser explicitamente identificadas como indisponíveis, nunca inventadas.
- Todos os botões devem ter ação funcional; teclas de atalho e tooltips; textos traduzíveis PT-BR via `lang/pt_br.json` e fallback `en_us.json`.
- Sincronização servidor→cliente para informações mutáveis, sem confiar em valores enviados pelo cliente.
- Não quebrar HUD de combate existente; nova GUI é um hub de gestão, não uma HUD permanente invasiva.
- Validar tela com escalas 2, 3, 4 e resoluções menores; não cortar abas nem botões.

## 3. Ataque direcionado — comportamento exato
- Criar keybind **configurável**, sugestão `R` apenas se livre; detectar/evitar conflito com a habilidade ultimate já existente e oferecer remapeamento no menu Controles. Preferir outra tecla default se R estiver ocupada.
- No pressionamento, cliente faz raycast da entidade viva sob a mira, com alcance configurável inicialmente moderado (sugestão 24 blocos, validar alcance efetivo no servidor). Enviar apenas identificação/intent de alvo ao servidor.
- Servidor revalida jogador, vínculo, entidade canônica viva, dimensão, alcance, linha de visão quando apropriada, entidade alvo viva, regras PvP, equipes, proteção disponível e permissões. Nunca confiar no raycast do cliente como autorização.
- Se companheiro estiver em Ficar ou Passivo, **ativar temporariamente o modo de ataque para cumprir a ordem**, armazenando modo anterior e retornando a ele após conclusão/cancelamento/invalidade. Não sobrescrever definitivamente preferência do jogador.
- O companheiro foca o alvo designado, com prioridade superior à seleção automática, respeitando navegação legítima e as regras de defesa/sobrevivência. Não teleportar para atravessar paredes nem permitir ataques através de obstáculos.
- **Mobs pacíficos e jogadores só podem ser alvos ofensivos mediante este comando explícito**, nunca por aquisição automática de alvo, dano incidental intencional ou retaliação autônoma contra esses tipos. Revisar ataques em área/ultimates para evitar dano colateral indevido em passivos/jogadores sem autorização; não interferir em regras legítimas do Minecraft sem avaliação.
- Jogadores só podem ser atacados quando PvP estiver habilitado e o alvo for legal conforme regras vanilla, times e eventuais hooks de proteção disponíveis. Não contornar proteções de outros mods/plugins.
- Não permitir selecionar proprietário, próprio companheiro, outros companheiros aliados/protegidos ou entidades invulneráveis. Respeitar equipes e regras de friendly fire.
- Repressionar tecla sobre mesmo alvo pode cancelar ordem; tecla de cancelamento ou ação no Grimório também deve cancelar. Se alvo morrer, trocar de dimensão, ficar inválido, fora de alcance de perseguição razoável ou se jogador perder vínculo, limpar foco e restaurar modo anterior. Definir timeout de perseguição configurável para evitar loops infinitos.
- Persistir apenas o que for necessário; evitar restaurar um alvo obsoleto após reinício. Não criar duplicatas ou referências fortes persistentes a entidades removidas.
- Exibir retícula/indicador discreto de alvo selecionado (`target_reticle.png`), nome do alvo e feedback claro em falha (fora do alcance, PvP desligado, alvo inválido etc.).

## 4. Arquitetura futura
- Separar `TargetCommandService`/estado de foco do controlador específico de Lupus, usando interface ou serviço genérico que permita reutilização por futuras espécies.
- Não criar registro multi-espécies completo nesta fase; apenas evitar dependências desnecessárias de classes Lupus nas APIs novas.
- O item Grimório não deve armazenar cópia autoritativa de inventário, progressão ou vínculo. Persistência canônica continua única fonte de verdade.

## 5. Testes e segurança
- Build `./gradlew.bat build --console=plain` no Windows; executar fixtures dedicadas existentes quando aplicáveis, conferindo logs além do exit code.
- Testar: primeira invocação entrega um Grimório; recuperação; migração de Soul Staff legado; menus informativos remotos; inventário dentro/fora de alcance; proprietário versus terceiro; atributos/talentos não duplicados; controles e conflito de tecla; alvo hostil; alvo pacífico; jogador com PvP on/off; jogador aliado; alternância de Passivo/Ficar e restauração; morte/cancelamento/teleporte/dimensão; retícula; reinício; dois clientes reais (se não houver, registrar como pendência, nunca alegar que FakePlayer os substitui).
- Testar que ataques automáticos e habilidades em área não atingem jogadores/passivos não designados; verificar permissões de dano no servidor e impossibilidade de spoof de pacotes.
- Preservar regressões de HP/God, inventário, IA defensiva, talentos, rituais e vínculo.

## 6. Entrega obrigatória
- Relatório por item/GUI/ataque, arquivos alterados, migração, escolhas de keybind, regras de PvP e limitações.
- Evidências do build, testes e local do JAR.
- Até 12 casos de homologação manual organizados em Grimório, UI e ataque direcionado.
- Não declarar homologação manual sem teste no jogo; não publicar JAR, não fazer commit/push automaticamente.
