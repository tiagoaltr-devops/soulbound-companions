# Soulbound Companions — Manual do jogador

Minecraft Java **1.21.11** · NeoForge **21.11.48** · Mod ID: `soulbound`.

Este manual descreve o código atual, incluindo a Fase 6.10.4. Receitas, custos e
intervalos abaixo são os padrões do projeto; configurações do servidor e datapacks
podem substituí-los. Tempos em segundos pressupõem 20 ticks por segundo.
Implementado não significa homologado em partida: veja as limitações no fim.

## 1. Começando: seu vínculo e o Ovo da Alma

No primeiro login, você recebe um **Ovo da Alma** vinculado permanentemente ao
seu UUID. Se o inventário estiver cheio, libere um espaço e aguarde a entrega.
Reconectar ou trocar o nome do jogador não cria outro vínculo.

Para invocar o Lupus Baby, use o ovo com botão direito em um local livre sobre
um bloco de apoio sólido. O nascimento inicial é gratuito. O ovo não é consumido.
Existe somente um companheiro canônico por jogador, mesmo quando ele está em
outra dimensão ou em um chunk descarregado.

Se perder o item, execute:

```text
/soulbound recover
```

Isso recupera o item, não recria nem ressuscita o companheiro. Com inventário
cheio, a recuperação fica pendente. Drops comuns e transferências para containers
são protegidos; o item de outro proprietário não concede controle sobre o vínculo.

| Estado do companheiro | Aparência do item | O que permite |
| --- | --- | --- |
| READY | Ovo da Alma | Primeira invocação |
| SUMMONING | Ovo da Alma | Aguardar a reserva de invocação; não invocar novamente |
| ACTIVE | Grimório da Alma | Comandos do companheiro |
| DEAD | Ovo Fragmentado | Preparar restauração; Follow/Stay bloqueados |
| MISSING | Grimório Inativo | Revisão administrativa; sem recriação automática |

São aparências do **mesmo item**, `soulbound:companion_egg`, e não itens separados.
A tooltip informa proprietário, espécie, estágio, estado e modo.

## 2. Grimório, movimento e sobrevivência

### Painel de status na tela principal

O canto superior esquerdo mostra Lupus, forma ativa, nível, coração e barra
vermelha de HP, além de valores/barra ciano de XP. Os dados vêm do servidor e
acompanham dano, cura, regeneração, talentos, transformação e level-up.

Morto: 0 HP, mantendo nível/XP. Não invocado ou descarregado: mensagem própria,
sem estimar vida desconhecida. No nível 30, exibe MAX e barra de XP completa.
Após ressuscitar, o painel aguarda a invocação para mostrar HP real novamente.

O painel respeita a escala de GUI, se desloca abaixo de boss bars quando necessário
e fica oculto em menus, F1, F3 e Tab. Não substitui a vida ou o XP do jogador.
Inspeção visual pendente: [complemento de HUD da Fase 6.6](FASE_6_6_HUD.md).

- **Botão direito com o Grimório:** abre o hub do companheiro.
- **Abas do Grimório:** visão geral, inventário, talentos, habilidades, status, comandos e vínculo.
- **Inventário e talentos integrados:** os slots e a árvore funcionam dentro do
  Grimório. Selecione um nó para consultar detalhes e investir pontos. Trocar de
  aba preserva o cursor; na aba Inventário, teclas numéricas mantêm a função de
  hotbar e a navegação usa a sidebar. Movimentar itens exige companheiro a até oito
  blocos. Veja a [integração nativa](FASE_6_10_4_1_INTEGRACAO.md).
- **Botão direito diretamente no Lupus:** o proprietário alterna Seguir/Ficar.
- **Seguir:** o Lupus acompanha o dono por navegação aérea, ajustando a altura.
- **Ficar:** paira próximo ao ponto definido. Fuga emergencial pode afastá-lo
  temporariamente; depois, retorna ao ponto, preservando o modo.

O Baby é passivo: não inicia ataques contra jogadores ou mobs. Ele possui
**33,6 pontos de vida base**, asas animadas e movimento flutuante.

### Ataque direcionado

Use **G** sobre uma entidade viva para ordenar o ataque; repita sobre o mesmo alvo
ou use **V** para cancelar. Os atalhos são remapeáveis nos Controles. R continua
reservado à ultimate. Baby não ataca. A ordem permite combate temporário em Ficar
e Passivo, preservando a preferência e o ponto de Ficar. Expira por padrão após
30 segundos; cancelamento, perigo defensivo ou alvo inválido também encerram a ordem.
Jogadores dependem de PvP habilitado na dimensão e das regras de equipes/proteção.
Pacíficos e jogadores não designados continuam excluídos dos ataques automáticos
e das áreas do mod. Detalhes: [Fase 6.10.4](FASE_6_10_4_GRIMORIO.md).

### Alerta de ameaças e fuga

O companheiro detecta hostis em um raio padrão de **12 blocos**, priorizando
ameaças ao dono quando essa informação está disponível. Olha para a ameaça e
emite um alerta, com intervalo mínimo padrão de **5 segundos**. Perigo muito
próximo provoca fuga temporária, sem combate ou alteração da IA do inimigo.

### Instinto Mineral

Busca minérios em raio padrão de **12 blocos**, com intervalo mínimo de
**10 segundos** entre inícios de buscas. A varredura é distribuída entre ticks
(128 posições por tick, por companheiro, por padrão) e não carrega outros chunks.

Detecta diamante, esmeralda e ouro, inclusive variantes de ardósia, além de ouro
do Nether e detritos ancestrais. Prioriza os mais raros e sinaliza por som,
partículas e animação, sem informar coordenadas exatas.

### Chamado da Alma — Soul Recall

No menu do Bastão, use o botão de Recall para trazer a entidade canônica para
uma posição segura próxima de você. Pode recuperar o companheiro de outra dimensão
ou tentar carregar de forma limitada seu último chunk conhecido.

- Não custa XP; cooldown de **30 segundos** após sucesso.
- Ao chegar, o companheiro fica em **Seguir**.
- Não cria outra entidade, não revive mortos e não corrige MISSING.
- Pode ser recusado por destino inseguro, localização desconhecida ou entidade
  montada/transportando passageiros. Aguarde a mensagem antes de repetir.

## 3. Ressonância Ancestral e Ruínas

Abra o menu do Bastão e escolha **Localizar Ruína** com o Lupus carregado e perto
de você, no Overworld. A habilidade está disponível desde Baby.

A busca procura a ruína natural elegível mais próxima em até **5.000 blocos
horizontais**, excluindo as já concluídas por você, Forjas artificiais e pedestais
sem origem natural confirmada. O cooldown padrão é de **30 segundos**.

O Lupus se desloca um curto trecho na direção encontrada, faz uma pausa e retorna.
Ele indica a direção; não percorre sozinho os milhares de blocos até a ruína.

**Limite importante:** a busca confirma estruturas em chunks já gerados, mesmo
descarregados ou nunca visitados por você. Não confirma ruínas em terreno ainda
não gerado e não gera chunks para procurá-las. Há limites de processamento por busca.

Ruínas novas integram praças decorativas de vilas, no máximo uma por vila, com **50% de chance por vila**, sem distância mínima padrão do spawn ou entre ruínas. A geração atual deve ser testada em mundo Normal, não Superflat.
Chunks antigos não recebem ruínas retroativamente. O sino da praça substituída é
removido apenas na geração nova; sinos de outras peças não são alvo da remoção.

## 4. Provação das Três Almas

Invoque seu companheiro e interaja com o pedestal natural. Clique em
**Iniciar prova solo / Begin solo trial**. Seu vínculo precisa estar ACTIVE.
Outros jogadores não espectadores dentro do perímetro impedem ou interrompem a prova.

1. **Sabedoria:** responda à pista exibida, uma por vez, clicando no símbolo
   correspondente. Não é necessário memorizar uma sequência exibida inteira.
   Há de 3 a 7 respostas, conforme a dificuldade acumulada da ruína.
2. **Coragem:** derrote os Guardiões Espirituais. São de 2 a 12 no total,
   com até 4 simultâneos por padrão. Eles atacam o desafiante e não concedem loot/XP.
3. **Vínculo:** mantenha seu Lupus canônico a até **3 blocos do centro** durante
   a canalização, de **5 a 25 segundos**, conforme a dificuldade.

### Referência das pistas

| Pista em português | Símbolo | Nome em inglês |
| --- | --- | --- |
| A luz que desperta ao amanhecer | Sol | Sun |
| Guardiã prateada da noite | Lua | Moon |
| Devora madeira e aquece a pedra | Chama | Flame |
| Corre sem pernas e molda as margens | Onda | Wave |
| Segura a árvore sob nossos pés | Raiz | Root |
| Carrega o viajante pelos céus | Asa | Wing |
| O vínculo que permanece além da vida | Alma | Soul |

Resposta errada, morte, desconexão, intrusão de outro jogador, saída do perímetro
horizontal de 48 blocos ou duração superior a 10 minutos cancelam a tentativa.
Afastar o companheiro na etapa Vínculo reinicia a canalização.

O sucesso entrega uma **Centelha Ancestral** e aumenta a dificuldade dessa ruína.
Cada jogador pode vencer e receber a Centelha **uma única vez por ruína**.
Inventário cheio deixa a recompensa pendente. O cooldown de repetição após falha
está temporariamente em **zero por padrão**, sem liberar nova recompensa após vencer.

## 5. Todas as receitas

Use uma **bancada 3×3**. As receitas são sem formato: a posição não importa,
mas ingredientes repetidos devem ocupar espaços separados. Cada execução consome
uma unidade de cada ingrediente/slot indicado.

| Resultado | Ingredientes | Quantidade produzida |
| --- | --- | --- |
| Forja da Alma | 1 Centelha Ancestral válida + 6 obsidianas choronas + 1 lingote de netherita + 1 diamante | 1 |
| Altar de Oferenda | 2 obsidianas + 1 lingote de ouro + 1 fragmento de ametista | 1 |
| Pedra Ritualística | 2 tijolos de pedra + 1 fragmento de ametista + 1 lápis-lazúli | 4 |
| Plataforma de Sacrifício | 2 obsidianas + 1 bloco de lápis-lazúli + 1 sculk | 1 |
| Essência de Reativação | 1 fragmento de eco + 1 lágrima de ghast + 1 fragmento de ametista + 1 fragmento de netherita | 1 |
| Core do Despertar | 4 obsidianas + 1 diamante | 1 |
| Core da Ascensão | 1 bloco de netherita + 1 fragmento de eco + 1 diamante | 1 |
| Core Divino | 1 Essência Dracônica + 1 estrela do Nether + 1 fragmento de ametista | 1 |
| Core da Restauração | 1 lírio-do-vale + 4 diamantes | 1 |
| Aplicação de Core | Seu Ovo/Bastão vinculado + 1 Core Carregado válido | Mesmo item de vínculo, com aplicação validada pelo servidor |

**Não possuem receita comum:** Ovo da Alma (entrega/recuperação), Bastão (aparência
do ovo), Centelha (provação), Essência Dracônica (dragão) e Core Carregado (ritual).

Mortes efetivas de Ender Dragons concedem **2 Essências Dracônicas**. Dragões
ressurgidos pelo ritual vanilla podem conceder novamente, pois têm outro UUID.

Uma Centelha ou um Core Carregado obtidos vazios pelo Criativo ou `/give` não
recebem automaticamente um token válido. Renomear ou copiar a aparência não
substitui sua obtenção pelo fluxo real.

## 6. Construção e primeira ativação da Forja

Fabrique **1 Forja, 3 Altares, 4 Pedras e 1 Plataforma**. Coloque todos no mesmo
nível horizontal, neste desenho 5×5, aceitando as quatro rotações:

```text
..O..
..P..
OPFPO
..P..
..X..
```

`F` = Forja; `O` = Altar de Oferenda; `P` = Pedra Ritualística;
`X` = Plataforma de Sacrifício. Os pontos não exigem componentes.

Fique na plataforma e olhe para a Forja para identificar os altares:

| Altar | Oferenda de ativação |
| --- | --- |
| Oposto à plataforma | 2 diamantes |
| À sua esquerda | 8 fragmentos de ametista |
| À sua direita | 1 fragmento de eco |

Para depositar, clique no altar com a stack na mão principal. Um novo clique
retira o conteúdo, se houver espaço no inventário. O item aparece flutuando sobre
o bloco; essa representação não pode ser coletada.

Abra a Forja e clique em **Ativar Forja**. Se faltar algo, leia o erro na janela.
A primeira ativação dura **15 segundos**, não cobra XP e não falha. As oferendas
ficam reservadas, sem permitir retirada ou uma segunda ativação. Quantidades
excedentes permanecem nos altares.

Você pode fechar e reabrir a janela sem reiniciar o prazo. Ao concluir, há som,
partículas e a mensagem **“A Forja da Alma foi despertada!”**. O ativador passa a
ser o proprietário por UUID, e o menu completo é liberado.

Se a estrutura for alterada durante a canalização, a reserva é devolvida aos
espaços canônicos. Reiniciar preserva a operação; a conclusão aguarda os chunks
necessários disponíveis, sem carregá-los à força.

### Recuperação e reativação

Somente o dono recupera uma Forja ativada por quebra comum. É necessário espaço
livre, núcleo retirado e nenhuma operação em andamento. O item preserva identidade
e desgaste, e volta **inativo** quando recolocado. Fabricar ou colocar não define
o dono; a primeira ativação é que o define.

Para reativar, prepare as mesmas três oferendas, segure uma **Essência de Reativação**
na mão principal e clique em **Reativar Forja**. Uma essência já armazenada também
pode ser usada. A canalização dura 15 segundos e mantém os riscos abaixo:

| Tentativa de reativação | Falha total | Destruição total |
| --- | --- | --- |
| 1ª | 1% | 0,25% |
| 2ª | 20% | 5% |
| 3ª | 40% | 10% |
| 4ª | 60% | 15% |
| 5ª e seguintes | 80% | 20% |

Em uma falha, 75% dos resultados deixam a Forja inativa e 25% a destroem
definitivamente. Os materiais são consumidos e o contador avança em cada tentativa
concluída, inclusive fracassada. Não há explosão destrutiva. Uma identidade destruída
não pode ser reutilizada para colocar outra Forja.

## 7. Rituais, Cores e evolução

Fabricar um Core produz a versão **bruta**. Antes de aplicá-lo ao vínculo,
carregue-o na Forja ativa:

1. Segure o Core bruto e use **Inserir da mão / retirar núcleo** no menu da Forja.
2. Deposite as oferendas da tabela, respeitando a ordem relativa à plataforma.
3. Fique sobre a Plataforma de Sacrifício e abra o menu da Forja.
4. Confirme o ritual e o sacrifício de níveis. Permaneça na plataforma até concluir.
5. Receba o Core Carregado e combine-o com seu Ovo/Bastão na bancada 3×3.

| Ritual | Altar oposto | Altar esquerdo | Altar direito | Níveis do dono / visitante | Aplicação |
| --- | --- | --- | --- | --- | --- |
| Despertar | 2 diamantes | 8 fragmentos de ametista | 16 lápis-lazúlis | 30 / 35 | Baby ACTIVE → Núcleo Junior; aplicação desbloqueia a forma |
| Ascensão | 1 lingote de netherita | 2 fragmentos de eco | 1 bloco de diamante | 45 / 50 | Junior desbloqueado, vínculo ACTIVE, nível 15 → Núcleo Pleno |
| Divino | 1 Essência Dracônica | 1 estrela do Nether | 4 cristais do End | 60 / 65 | Pleno desbloqueado, vínculo ACTIVE e nível 25 → Núcleo God |
| Restauração | 4 diamantes | 1 lírio-do-vale | 1 lágrima de ghast | 25 / 30 | DEAD → renascimento Baby |

Baby → Junior dura **20 segundos**; Junior → Pleno dura **30 segundos** e a infusão God dura **45 segundos**. Os demais rituais mantêm
o padrão de **15 segundos**. São cobrados **níveis inteiros**, não pontos
brutos de experiência. Visitantes usam o próprio vínculo e pagam a sobretaxa indicada.
Os **Núcleos Junior, Pleno e God** são validados pelo proprietário e vínculo da infusão. Outros
Cores Carregados mantêm a negociação anterior. Todos são de uso único.

Sair da plataforma, morrer, desconectar ou invalidar estrutura/vínculo cancela o
ritual. Core bruto e oferendas voltam aos espaços canônicos, mas **XP não é devolvido**.
Reservas interrompidas por reinício são recuperadas. Nenhum Core Carregado é emitido
por uma tentativa cancelada.

### Estágios e God

| Forma | Vida máxima | Escala relativa | Natureza |
| --- | --- | --- | --- |
| Baby | 33,6 | 1 | Permanente |
| Junior | 33,6 | 1 | Forma desbloqueada permanentemente; visual de pequeno dragão Irushia |
| Pleno | 33,6 | 1 | Visual Brambleback, criatura robusta com golpes de braços |
| God Prime | 67,2 | 1 | Temporária, modelo próprio; exige desbloqueio |

Junior usa o visual Baby Dragon Irushia, de kyoukster (CC BY 4.0), com navegação
terrestre e combate preservados. As asas são visuais e não concedem voo.
Detalhes e créditos: [visual Junior Irushia](JUNIOR_IRUSHIA.md).
Pleno usa o visual Brambleback, de monkus (CC BY-SA 4.0), preservando seu combate.
Detalhes e créditos: [visual Pleno Brambleback](PLENO_BRAMBLEBACK.md).
God Prime possui modelo próprio, barreira espiritual e combate supremo temporário.

Após a infusão, combine **Núcleo Junior + seu Bastão** na bancada. O Lupus permanece
Baby: o resultado desbloqueia Junior, sem transformação automática. No menu do
Bastão, selecione Baby, Junior ou Pleno desbloqueados; há intervalo de **3 segundos** entre trocas e
validação de espaço para o novo corpo. UUIDs e vínculo não mudam.

Modos no menu: **Seguir/Combate** acompanha e permite ataques apenas em Junior;
**Ficar** e **Seguir (passivo)** bloqueiam ataques e ultimate. O Baby nunca ataca.
Junior procura hostis próximos ao dono e não ataca jogadores, aliados, outros
companheiros ou Guardiões da provação.

Com Junior ativo em Seguir/Combate, pressione **R** para
**Ruptura Ancestral** (tecla alterável nos controles). A onda afeta hostis válidos
em até **5 blocos**, com linha de visão, sem explosão/destruição. Cooldown de
**60 segundos**, persistente e exibido no menu. Dano padrão: **4** no ataque básico
e **8** na ultimate; intervalo básico de **20 ticks**. Esses danos e intervalo
podem ser configurados pelo servidor.

Desde a Fase 6.4.1, a ultimate não exige o Bastão na mão: a identidade é validada
diretamente no servidor. A tecla também funciona no menu do companheiro; outras
telas, como o chat, não acionam o atalho. A HUD mostra a tecla configurada, estado
pronto/bloqueado ou cooldown. Se houver conflito com outro mod, remapeie nos controles.
O botão **Seguir / Combate** habilita combate no Junior, Pleno e God; **Seguir (passivo)** não.
Trocar de forma preserva o modo anterior. Não existe modo Berserker nesta fase.

Após aplicar o Core Divino, use o botão God no Bastão ou `/soulbound god`.
A forma dura **60 segundos**, com cooldown de **5 minutos a partir da ativação**,
controlado pelo relógio persistente do servidor.

### Restauração

Exige companheiro DEAD e ressurreição necessária. Reutiliza os mesmos UUIDs de
proprietário, vínculo e companheiro. A Forja ativa mostra **Ressuscitar Companheiro**
para o dono do vínculo morto, mesmo sem entidade ativa no mundo.

Fabrique o **Core da Restauração** (1 lírio-do-vale + 4 diamantes), insira-o na
Forja e deposite **4 diamantes / 1 lírio-do-vale / 1 lágrima de ghast** nos altares
oposto/esquerdo/direito. Fique na plataforma e confirme a Ressurreição: custo padrão
de **25 níveis para o dono da Forja ou 30 para visitante**, duração padrão de
**15 segundos**, com progresso na janela. Materiais do crafting e oferendas são custos
separados. Cancelamento segue a regra normal: materiais retornam, XP não.

Ao concluir, o vínculo fica **READY**. Use seu ovo em um bloco válido para invocar
o Baby em Seguir (passivo), sem criar outro vínculo. Estágio máximo, arquétipo,
formas desbloqueadas, progresso e cooldowns permanentes são preservados, incluindo
Junior. Depois, selecione Junior na HUD respeitando o cooldown de transformação.
A forma temporária God em andamento é encerrada, sem apagar seu desbloqueio.
Não é necessário combinar outro Core Carregado após esse ritual direto da Forja.
Um Core da Restauração Carregado legado ainda pode ser aplicado pela receita antiga,
liberando a mesma recuperação para READY, sem apagar as formas.
MISSING não autoriza recriação automática. Recuperar apenas o ovo nunca substitui
o ritual de Restauração.

## 8. Progressão RPG e talentos — Fase 6.5

O vínculo tem **XP próprio**, separado dos níveis do jogador. Começa no nível 1,
com 0 XP; vai até 30 e ganha 1 ponto de talento por nível a partir do 2 (máximo 29).
A necessidade para L → L+1 é `50 + 20*(L-1) + 5*(L-1)²`. Troca de forma, morte,
ressurreição e Recall preservam XP, nível e talentos.

Fontes: participação efetiva em mortes de hostis naturais (8 XP comum, 25 perigoso
com vida máxima ≥40, 200 Ender Dragon/Wither), descobertas confirmadas (30 XP) e
novas conclusões de prova ancestral (100 XP). Descobertas/provas não se repetem por
vínculo. Participação em combate tem janela de 15 segundos. Comandos, ovos,
spawners, mobs sem IA e origens desconhecidas não concedem XP de combate.

Clique no botão de nível/XP/pontos do menu do Bastão para abrir **Talentos**.
Use as abas Principal, Baby e Junior e as setas para navegar; o cartão informa
efeito, pré-requisitos, máximo e motivo do bloqueio. Todos os ramos gastam o mesmo
saldo. Investir em Junior exige desbloqueá-lo, mas não estar nessa forma no momento.

| Ramo | Talentos e efeitos por ponto |
| --- | --- |
| Principal | Vitalidade +4% vida (5); Força +3% dano em forma ofensiva (5); Sintonia +5% XP (3); Pele +3% redução de dano (3); Regeneração +0,1 HP/s fora de combate (3) |
| Baby | Olhar +2 blocos de busca mineral, configurável, limite 32 (3); Ressonância com mais marcadores de direção (3); Aura Resistência I ao dono em raio 6/8/10, sem acumular (3); Vínculo +5% XP de descobertas (3) |
| Junior | Golpe +4% dano básico (5); Predador +3% velocidade de ataque (3); Ruptura +8% dano especial (3); Sintonia -4% cooldown da ultimate (3); Fúria +10% dano por 5 s após eliminação elegível, intervalo interno 20 s (1) |

Os números entre parênteses são os máximos de pontos. Bônus de XP preservam as
frações. Regeneração exige 5 segundos sem combate/dano. Os talentos de uma forma
ficam inativos ao selecionar outra. Eles não liberam ataques em Baby/Ficar/Passivo.

**Redistribuir talentos:** na Forja ativa, retire o núcleo, deposite **8 fragmentos
de ametista no altar oposto**, fique na plataforma e clique no botão ao lado de
Trocar núcleo. Custa **10 níveis do jogador** e dura **10 segundos**, sem chance
de falha. Devolve todos os pontos conquistados, sem apagar nível, XP, formas ou
progressão ritualística. Cancelamento devolve materiais, mas não XP do jogador.
Não confunda o nível RPG do companheiro com os níveis de experiência cobrados na Forja.

Requisitos completos e roteiro de homologação: [Fase 6.5](FASE_6_5_PROGRESSAO.md).

## 9. Comandos e identificadores úteis

Comandos administrativos exigem as permissões apropriadas. `/give` não fabrica
tokens oficiais de Centelha, Forja ou Core Carregado.

```text
/soulbound recover
/soulbound god
/soulbound altar inspect
/give @s soulbound:companion_egg
/give @s soulbound:offering_altar 3
/give @s soulbound:ritual_stone 4
/give @s soulbound:xp_platform
/data get entity @e[type=soulbound:lupus_companion,sort=nearest,limit=1]
```

`recover` recupera seu item; `altar inspect`, perto da Forja, mostra estado,
proprietário por UUID e tentativas. O ovo vazio de `/give` é normalizado para seu
vínculo existente, sem criar outro companheiro.

| Item | ID |
| --- | --- |
| Ovo / Bastão | `soulbound:companion_egg` |
| Centelha Ancestral | `soulbound:ancestral_spark` |
| Forja da Alma | `soulbound:soul_forge` |
| Altar de Oferenda | `soulbound:offering_altar` |
| Pedra Ritualística | `soulbound:ritual_stone` |
| Plataforma de Sacrifício | `soulbound:xp_platform` |
| Essência de Reativação | `soulbound:reactivation_essence` |
| Essência Dracônica | `soulbound:draconic_essence` |
| Core do Despertar | `soulbound:awakening_core` |
| Core da Ascensão | `soulbound:ascension_core` |
| Core Divino | `soulbound:divine_core` |
| Core da Restauração | `soulbound:restoration_core` |
| Core Carregado, tipo validado pelo servidor | `soulbound:charged_core` |

## 10. Problemas comuns e estado de validação

- **Forja aparece e some ao colocar:** confirme que foi fabricada com Centelha
  válida; versões vazias do Criativo/comandos e cópias já colocadas são recusadas.
- **Ativação recusada:** confira mesmo nível, quatro pedras, plataforma e altares
  na orientação correta. Leia na janela qual material/quantidade está faltando.
- **Ressonância sem destino:** pode não haver ruína física confirmada no raio,
  a ruína pode já estar concluída por você ou o terreno ainda não foi gerado.
- **Inventário cheio:** libere espaço para entregas e retiradas pendentes.
- **Companheiro indisponível:** leia o estado do vínculo; não tente resolver
  copiando ovos ou usando `/summon` sem identidade.

O jogador já informou homologação de localização/provação/Centelha, fabricação e
posicionamento dos componentes, reconhecimento do multibloco, menu e depósito de
oferendas. A Fase 6.3.1 passou no build e verificações automatizadas, mas animações,
sons, sincronização em partida e os novos fluxos de ativação/reativação ainda
precisam de homologação manual. Rituais, aplicação de Cores e multiplayer também
devem ser testados em mundo de cópia; este manual não declara aprovação desses testes.

Roteiros: [ativação e apresentação da Forja](FASE_6_3_1_ATIVACAO.md) e
[checklist geral](FASE_6_CHECKLIST.md).


### Pleno — Fase 6.7

Combine Núcleo Pleno + seu Bastão na bancada após a Ascensão. O desbloqueio não altera a forma ativa. Em Seguir/Combate: combo 100/100/150%, Investida 130% (12 s), Abalo 80% (raio 4, 18 s). R prepara Cataclismo por 1,5 s, raio 6 e 250% do dano básico, com cooldown próprio de 90 s reservado no início. Cinco talentos de até três ranks compartilham os pontos existentes. Consulte [detalhes e testes manuais](FASE_6_7_PLENO.md).

### Homologação de XP — Correção 6.7.1

Operadores nível 2 podem usar /soulbound debug setlevel 15 e /soulbound debug addxp 110 no próprio vínculo. Setlevel zera o XP do nível e recusa talentos incompatíveis. Para testar combate com mobs de comando/ovos, consulte a opção de desenvolvimento e o roteiro em [Correção 6.7.1](CORRECAO_6_7_1_XP.md).

### God Prime — Fase 6.8

Após a infusão Divina, combine Núcleo God + seu Bastão na bancada. Com Pleno ativo, selecione God no menu: 60 s, cooldown de 5 min desde a ativação. O tempo usa ticks persistidos do servidor e pausa com ele desligado. God recebe uma barreira única de 30% da vida máxima efetiva; não cura HP. Ao terminar, retorna Pleno sem escudo. R executa Apocalipse Ancestral (preparo 2 s, raio 8, 450%, cooldown 120 s). Os cinco talentos God compartilham os pontos existentes. Veja [regras, persistência e checklist](FASE_6_8_GOD_PRIME.md).

### Sobrevivência e equipamentos — Fase 6.9

Baby, Junior e Pleno compartilham 33,6 HP-base; God mantém 67,2. Após oito segundos sem combate, regeneram a cada dois segundos 0,5/1/1,5/2 HP, mais o talento. Trocas normais e aumentos de máximo não curam. O retorno God usa o fator da ativação para impedir cura ao trocar equipamentos durante a transformação.

No Bastão, use Equipamentos a até oito blocos do companheiro: quatro armaduras, uma arma e quatro alimentos. Itens e encantamentos compatíveis funcionam sem aparecer no modelo e permanecem no vínculo após morte/ressurreição. Alimente com botão direito: a cura usa a saturação do componente alimentar, sem efeitos especiais ou recarga do escudo. Em defesa, entra com HP até 50%, recua em Seguir/Combate, come no máximo a cada cinco segundos e só retorna à ofensiva aos 65%. Ficar e Passivo mantêm suas restrições.

Leia [compatibilidade, persistência e testes](FASE_6_9_SOBREVIVENCIA.md).
