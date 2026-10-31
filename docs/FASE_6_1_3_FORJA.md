# Fase 6.1.3 — Ressonância e homologação da Forja

## Ajustes e validação

- Ressonância: alcance horizontal padrão/máximo de **5.000 blocos**. A configuração
  local `run/config/soulbound-server.toml` foi atualizada; instalações com valor antigo
  devem definir `resonanceRadius = 5000` e reiniciar.
- A busca enumera candidatos nativos de vilas pela seed e consulta assincronamente
  seus starts e os blocos/credenciais dos pedestais no disco. Não depende só do índice,
  não carrega nem gera chunks. Mantém dois lançamentos por tick, duas leituras por
  busca e quatro buscas simultâneas; o teto passou de 256 para 512 fontes.
- Ruínas confirmadas, inclusive em chunks descarregados e nunca visitados pelo
  jogador, são comparadas pela distância horizontal e pelas conclusões do proprietário.
  Um candidato sem estrutura física confirmada não é anunciado como ruína.
  **Limitação:** chunks nunca gerados não fornecem prova física e não são destinos.
  Datapacks com vilas excepcionalmente densas podem atingir o teto; nesse caso a
  seleção continua restrita à cobertura confirmada, sem prometer o raio completo.
- Os sinos são identificados no template rotacionado da praça escolhida. Suas posições
  exatas ficam na peça; somente blocos ainda do tipo sino nessas posições são removidos,
  durante o worldgen novo e no chunk processado. A caixa da peça inclui esses pontos,
  mantendo o centro do pedestal explícito para preservar UUID e descoberta no disco.
  Peças antigas não recebem posições de remoção. Não há varredura de vilas existentes.
- Seleção da praça, frequência, spawn mínimo de 500, separação de 1.000, cooldown
  zero das provas, movimento do Lupus, Identity e Soul Recall foram preservados.

Homologação informada pelo jogador: Ressonância, localização/acesso à ruína natural,
conclusão da prova e recebimento da Centelha Ancestral. Não se estende aos ajustes novos.

`gradlew.bat build`: **BUILD SUCCESSFUL**, 288 verificações: 52 Ressonância,
102 altar e 134 anteriores. O teste novo inicialmente tentou inicializar Minecraft
fora do FML; foi corrigido separando a geometria pura. Log final: `build/phase613-build.log`.
As verificações abrangem fronteira de alcance, conclusão por dono, identidade NBT,
limites/posições dos sinos, receitas dos componentes, quatro orientações, ativação
inicial sem falha e persistência de proprietário/estado ativo. A Forja existente foi
revisada sem reimplementação ou alteração das receitas/probabilidades.
Não houve teste novo de worldgen, cliente ou servidor. Remoção real do sino,
crafting com cliques, menu e multiplayer permanecem manuais. Fase 6.4 não foi iniciada.

## Checklist curto — Fase 6.3

Use bancada 3×3. Receitas abaixo são sem formato; ingredientes repetidos devem
ocupar slots separados. Use a Centelha recebida na prova: `/give` não emite seu token.

| Fabricar | Ingredientes por receita | Resultado |
| --- | --- | --- |
| Forja da Alma | 1 Centelha Ancestral + 6 obsidianas choronas + 1 lingote de netherita + 1 diamante | 1 Forja |
| Altar de Oferenda | 2 obsidianas + 1 lingote de ouro + 1 fragmento de ametista | 1 altar; repetir 3 vezes |
| Pedra Ritualística | 2 tijolos de pedra + 1 fragmento de ametista + 1 lápis-lazúli | 4 pedras |
| Plataforma de Sacrifício | 2 obsidianas + 1 bloco de lápis-lazúli + 1 sculk | 1 plataforma |

- [ ] Fabricar e colocar a Forja `soulbound:soul_forge`. Não usar Forja vazia de `/give`.
- [ ] Montar no mesmo nível horizontal (pontos livres):

```text
..O..
..P..
OPFPO
..P..
..X..
```

`F`: Forja; `O`: três Altares de Oferenda; `P`: quatro Pedras Ritualísticas;
`X`: Plataforma de Sacrifício (`soulbound:xp_platform`).

- [ ] Clicar com botão direito em cada altar com a stack na mão principal:
  oposto à plataforma = **2 diamantes**; esquerda = **8 fragmentos de ametista**;
  direita = **1 fragmento de eco**, olhando da plataforma para a Forja.
  Outro clique retira a oferenda; mantenha espaço livre no inventário.
- [ ] Clicar na Forja e verificar o menu **“Sem proprietário — ativação inicial”**,
  com as três oferendas. Deixar o núcleo vazio; primeira ativação não exige essência
  nem sacrifício de XP. Confirmar **“Confirmar ativação / risco de reativação”**.
- [ ] Esperar a mensagem **“A Forja da Alma foi ativada.”**, estado **“Ativa”** e
  nome do ativador no título. As oferendas são consumidas e não há sorteio de falha inicial.
- [ ] Executar `/soulbound altar inspect` perto da Forja: conferir `ACTIVE` e
  `owner=<UUID do ativador>`. Reiniciar normalmente e conferir o mesmo proprietário.
- [ ] Com outro jogador, verificar que ele não quebra/reativa a Forja do dono.
- [ ] Conferir as outras três rotações em estruturas de teste válidas; retirar uma
  pedra deve impedir ativação. Não usar recuperação de Forja ativa para repetir
  primeira ativação: recolocação é reativação e mantém o desgaste.
- [ ] Para os ajustes de worldgen, conferir uma praça nova com ruína sem seu sino,
  preservando casas, caminhos e sinos de outras peças. Vilas antigas ficam intactas.
- [ ] Confirmar direção/retorno do Lupus para uma ruína confirmada no raio de 5.000,
  inclusive descarregada, e exclusão da ruína já concluída pelo mesmo jogador.

Estado da Fase 6.3: revisão e verificações automatizadas concluídas;
homologação manual acima pendente. Não iniciar rituais/evolução neste checklist.
