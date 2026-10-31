# Fase 6 — Checklist de homologação

## Atualização 6.1.1

- [x] Build limpo com 256 verificações: 221 anteriores e 35 de geração/ressonância.
- [x] Origem natural e voxel físico; rejeição de pedestal artificial e start sem vila.
- [x] Migração de registros/peças anteriores preservando conclusões.
- [x] Seleção por proximidade, exclusão de concluídas/inválidas e cooldown persistente.
- [x] Quatro vilas selecionadas e quatro pedestais reais na amostra final de seed fixa.
- [x] Reconstrução por leitura de disco com destinos descarregados e sem interação inicial.
- [x] Navegação real com jogador simulado: ida/retorno Follow e Stay, UUID e âncora,
  cancelamento sem dono e compatibilidade com a finalização do Recall.
- [!] Homologar cliques de Localizar Ruína, aparência, obstáculos e ameaças com pessoas.
- [!] Homologar disponibilidade em Junior/Pleno/God e multiplayer com conclusões distintas.
- [!] Validar parâmetros não padrão e integração visual de todas as variantes de vila.

Detalhes e limites em [FASE_6_1_1_RESSONANCIA.md](FASE_6_1_1_RESSONANCIA.md).

Legenda: `[x]` implementado e validado no escopo descrito; `[!]` implementado,
mas exige homologação manual; `[ ]` pendência real. Nenhum `[!]` significa teste
de jogabilidade aprovado. Consulte os limites em [FASE_6_IMPLEMENTACAO.md](FASE_6_IMPLEMENTACAO.md).

## Validação executada

- [x] Inspecionar AGENTS, Gradle, identidade, entidades, itens, menus e APIs locais.
- [x] Build inicial com 134 verificações existentes.
- [x] Checkpoints de compilação e correção da API incompatível de profile cache.
- [x] Build limpo integrado com 221 verificações, incluindo 87 novas do altar.
- [x] Políticas de dificuldade, riscos e quatro rotações testadas automaticamente.
- [x] Serialização de reservas, tokens, desgaste, transferências e diário de aplicação.
- [x] Leitura/escrita NBT comprimida em disco para o registro do altar.
- [x] Verificação de preservação de owner/bond/companion UUID e Recall na evolução.
- [x] Servidor dedicado chegou a Done em mundo isolado, sem falha de classe cliente.
- [x] Cliente carregou modelos, renderers e atlas de itens/blocos.
- [x] Geração natural: três pedestais observados em nove candidatos de vila.
- [x] Os três pedestais persistiram após salvar, reiniciar e recarregar seus chunks.
- [x] Distância entre os três centros amostrados superior a 8.000 blocos.

## Ruínas

- [!] Explorar novas seeds/biomas; confirmar raridade e associação com vilas.
- [!] Conferir barreira de 2.000 blocos com spawn deslocado antes da geração.
- [!] Verificar separação em exploração extensa e após trocar de dimensão.
- [!] Conferir que chunks antigos não são reformados e que não surge Forja natural.
- [!] Avaliar sala, terreno e possíveis sobreposições com peças da vila.
- [!] Interagir, reiniciar e conferir o mesmo ruinId, cooldown e dificuldade.

## Provas — um e dois jogadores

- [!] Primeiro jogador inicia com companheiro ACTIVE; sem companheiro é recusado.
- [!] Segundo jogador próximo bloqueia início; espectador não interfere.
- [!] Errar símbolo cancela; acertar sequência avança e toca feedback.
- [!] Vencer guardiões; conferir limite simultâneo, vida/dano e ausência de loot/XP.
- [!] Confirmar que guardiões não ferem terceiros, aldeões ou o Baby.
- [!] Levar a entidade canônica ao centro; afastá-la zera canalização.
- [!] Outro proprietário/companheiro não satisfaz a prova do vínculo.
- [!] Intrusão, morte, logout e saída cancelam e limpam guardiões.
- [!] Salvar/reiniciar durante cada etapa; conferir liberação e cooldown de 30 minutos.
- [!] Fracasso não incrementa dificuldade; cada sucesso de outro UUID incrementa.
- [!] Mesmo UUID não recebe segunda Centelha na mesma ruína; outra ruína permite.
- [!] Inventário cheio mantém recompensa pendente e entrega uma vez ao liberar espaço.

## Forja

- [!] Fabricar com Centelha oficial e testar colocação/cópias do mesmo token.
- [!] Ativar nas quatro rotações com oferendas; confirmar dono como primeiro ativador.
- [!] Tentar ativar com componentes/materiais faltando; nada deve ser consumido.
- [!] Testar retirada dos materiais, inventário cheio e reconexão durante transferências.
- [!] Conferir materiais, XP e probabilidades apresentados no menu.
- [!] Quebra do dono recupera mesmo forgeId; quebra de terceiro é negada.
- [!] Explosão, pistão e bosses não removem a Forja pelos meios protegidos.
- [!] Recolocar preserva dono/desgaste e exige essência + materiais originais.
- [!] Forçar falha comum/critica por configuração em mundo de teste; conferir contador.
- [!] Reiniciar após tentativa; contador e resultado não podem ser sorteados novamente.
- [!] DESTROYED não é recolocável; uma nova Centelha cria outra identidade legítima.

## Rituais e aplicação

- [!] Despertar cobra 30 níveis do dono/35 do visitante.
- [!] Ascensão cobra 45/50; Divino 60/65; Restauração 25/30.
- [!] Confirmar níveis insuficientes sem consumo; uma canalização por Forja.
- [!] Confirmar três oferendas corretas e Core bruto; rejeitar materiais/tipos errados.
- [!] Completar 15 segundos na plataforma e receber Core Carregado oficial.
- [!] Sair, morrer, desconectar ou remover componente: devolver materiais, não XP.
- [!] Reiniciar durante canalização: recuperar materiais nos espaços de origem.
- [!] Testar recuperação de depósito/retirada interrompidos, inclusive inventário cheio.
- [!] Negociar Core; aplicar na bancada somente ao vínculo e estágio compatíveis.
- [!] Testar cliques normais e shift-click; Core recusado retorna, aceito não retorna.
- [!] Core visualmente adulterado/sem token/spent não altera estado.
- [!] Bastão alheio não autoriza aplicação; baús continuam sem aceitar o Bastão.
- [!] Baby→Junior→Pleno preserva os três UUIDs e muda atributos/escala.
- [!] Divino desbloqueia God uma vez; novas ativações não exigem outro Core.
- [!] God dura 60 segundos de ticks e aplica cooldown de 15 minutos desde a ativação.
- [!] Matar e ressuscitar: DEAD→SUMMONING→ACTIVE, mesmo UUID, Baby, progresso zero.
- [!] MISSING e ACTIVE não aceitam Restauração; não nasce segunda entidade.
- [!] Repetir morte vanilla do Ender Dragon após respawn e conferir essências renováveis.
- [!] Substituir receitas/oferendas por datapack, usar /reload e verificar validação.

## Regressão

- [!] Primeiro login e reconexão: um vínculo, entrega do ovo e nascimento gratuito.
- [!] Follow/Stay, voo, cavernas/florestas, fuga, alerta e Instinto Mineral.
- [!] Bastão, recuperação do item e Soul Recall sem custo de XP, inclusive entre dimensões.
- [!] Baby permanece sem ataques; terceiros não comandam o companheiro.
- [!] Persistência e recuperação em servidor dedicado com dois jogadores reais.

## Pendências/limites explícitos

- [ ] Homologação completa multiplayer e dos cliques nativos de crafting/menus.
- [ ] Testes de crash abrupto em cada fronteira de transação.
- [ ] Arte final Junior/Pleno/God e do guardião; usam placeholders declarados.
- [ ] Integração do placement próprio com /locate vanilla.
- [ ] Geometria mais refinada para terreno/vilas; validar sobreposição visual.
- [ ] Configuração dos padrões internos das duas receitas especiais.
- [ ] Automação e renderização dos conteúdos dos altares, fora do armazenamento atual.
- [ ] Recuperação automática de entidade ausente após crash ambíguo de inserção;
  atualmente exige revisão administrativa para preservar a reserva contra duplicação.
