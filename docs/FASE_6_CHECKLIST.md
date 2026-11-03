# Fase 6 — Checklist de homologação

## Complemento 6.6 — HUD de vida e experiência

Painel de status, snapshots autoritativos e estados especiais implementados.
Build: 566 verificações aprovadas. [Roteiro visual pendente](FASE_6_6_HUD.md).

## Atualização 6.5

Progressão, 14 talentos Lupus, especializações e Redistribuição implementados.
Build com 506 verificações e 45 checagens nativas no servidor isolado aprovados.
[Detalhes e homologação manual pendente](FASE_6_5_PROGRESSAO.md).

## Complemento 6.4.1 — Ressurreição

DEAD confirmado no SavedData do mundo New World (2). Recuperação em Baby preserva
Junior e progressão; 32 testes nativos de combate/vida/recuperação aprovados.
[Diagnóstico e roteiro manual](FASE_6_4_1_RESSURREICAO.md).

## Atualização 6.4.1

Combate e ultimate verificados em servidor isolado: 18 verificações nativas.
Modo Seguir/Combate explícito, atalho no menu, autorização sem Bastão na mão e
HUD/erros específicos. [Resultados e homologação visual pendente](FASE_6_4_1_COMBATE.md).

## Atualização 6.4

Formas Baby/Junior, infusão, Núcleo Junior, modelo humanoide, combate e Ruptura
Ancestral implementados. Build: 390 verificações aprovadas. Homologação manual
pendente no [roteiro da Fase 6.4](FASE_6_4_JUNIOR.md).

## Atualização 6.3.1

Ativação de 15 segundos, HUD contextual, apresentação das oferendas e efeitos
implementados; build com 330 verificações aprovado. Homologação visual/rede pendente:
[roteiro da Fase 6.3.1](FASE_6_3_1_ATIVACAO.md).

## Atualização 6.1.3

Ressonância, acesso à ruína natural, prova e Centelha homologados pelo jogador.
Ajustes novos: raio de 5.000 e remoção do sino somente da praça substituída em
worldgen novo. Build aprovado com 288 verificações. Homologação dos ajustes e
da Forja: [checklist curto da Fase 6.3](FASE_6_1_3_FORJA.md).

## Atualização 6.1.2

- [x] Build incremental com 264 verificações aprovadas.
- [x] Duas rodadas: 164 → 266 candidatas sintéticas selecionadas, sem perder as anteriores.
- [x] Testes de separação, ausência de duplicatas e limite de 500 blocos do spawn.
- [x] Cooldown zero aceita repetição e ignora espera antiga; valores positivos preservados.
- [!] Homologar frequência em chunks novos e repetição imediata da prova no jogo.

Resumo em [FASE_6_1_2_FREQUENCIA.md](FASE_6_1_2_FREQUENCIA.md).

## Atualização 6.1.1

- [x] Build limpo com 256 verificações: 221 anteriores e 35 de geração/ressonância.
- [x] Origem natural e voxel físico; rejeição de pedestal artificial e start sem vila.
- [x] Migração de registros/peças anteriores preservando conclusões.
- [x] Seleção por proximidade, exclusão de concluídas/inválidas e cooldown persistente.
- [x] Quatro vilas selecionadas e quatro pedestais reais na amostra final de seed fixa.
- [x] Reconstrução por leitura de disco com destinos descarregados e sem interação inicial.
- [x] Navegação real com jogador simulado: ida/retorno Follow e Stay, UUID e âncora,
  cancelamento sem dono e compatibilidade com a finalização do Recall.
- [x] Retomada do checkpoint preservado, sem refazer a geração ou a navegação já aprovadas.
- [x] Servidor anterior salvo e encerrado; log final do ensaio nativo confirmou 286 verificações.
- [x] Build final limpo da retomada: 256 verificações aprovadas.
- [x] Cliente da retomada carregou recursos e foi encerrado após a checagem.
- [x] JAR final contém a funcionalidade e exclui classes/configuração de testes.
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
