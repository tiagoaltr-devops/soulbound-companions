# Soulbound Companions — Manual do jogador

Minecraft Java **1.21.11** · NeoForge **21.11.48** · Mod ID: `soulbound`.

Este manual descreve o código atual, incluindo a Fase 6.3.1. Receitas, custos e
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
| ACTIVE | Bastão da Alma | Comandos do companheiro |
| DEAD | Ovo Fragmentado | Preparar restauração; Follow/Stay bloqueados |
| MISSING | Bastão Inativo | Revisão administrativa; sem recriação automática |

São aparências do **mesmo item**, `soulbound:companion_egg`, e não itens separados.
A tooltip informa proprietário, espécie, estágio, estado e modo.

## 2. Bastão, movimento e sobrevivência

- **Botão direito com o Bastão:** alterna Seguir e Ficar.
- **Shift + botão direito:** abre o menu de comandos.
- **Botão direito diretamente no Lupus:** o proprietário alterna Seguir/Ficar.
- **Seguir:** o Lupus acompanha o dono por navegação aérea, ajustando a altura.
- **Ficar:** paira próximo ao ponto definido. Fuga emergencial pode afastá-lo
  temporariamente; depois, retorna ao ponto, preservando o modo.

O Lupus é passivo: não inicia ataques contra jogadores ou mobs. O Baby possui
**12 pontos de vida**, asas animadas e movimento flutuante.

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

Ruínas novas integram praças decorativas de vilas, no máximo uma por vila, a pelo
menos **500 blocos do spawn** e **1.000 blocos entre ruínas**. São raras: nem toda
vila possui uma. A geração atual deve ser testada em mundo Normal, não Superflat.
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
| Despertar | 2 diamantes | 8 fragmentos de ametista | 16 lápis-lazúlis | 30 / 35 | Baby ACTIVE → Junior |
| Ascensão | 1 lingote de netherita | 2 fragmentos de eco | 1 bloco de diamante | 45 / 50 | Junior ACTIVE → Pleno |
| Divino | 1 Essência Dracônica | 1 estrela do Nether | 4 cristais do End | 60 / 65 | Pleno ACTIVE → desbloqueia God uma vez |
| Restauração | 4 diamantes | 1 lírio-do-vale | 1 lágrima de ghast | 25 / 30 | DEAD → renascimento Baby |

A duração padrão é de **15 segundos**. São cobrados **níveis inteiros**, não pontos
brutos de experiência. Visitantes usam o próprio vínculo e pagam a sobretaxa indicada.
O Core Carregado pode ser negociado; o destinatário precisa cumprir os requisitos
de seu próprio vínculo. O token é de uso único.

Sair da plataforma, morrer, desconectar ou invalidar estrutura/vínculo cancela o
ritual. Core bruto e oferendas voltam aos espaços canônicos, mas **XP não é devolvido**.
Reservas interrompidas por reinício são recuperadas. Nenhum Core Carregado é emitido
por uma tentativa cancelada.

### Estágios e God

| Forma | Vida máxima | Escala relativa | Natureza |
| --- | --- | --- | --- |
| Baby | 12 | 1 | Permanente |
| Junior | 24 | 1,2 | Permanente |
| Pleno | 40 | 1,45 | Permanente |
| God | 80 | 1,7 | Temporária, desbloqueada para Pleno |

Junior, Pleno e God ainda reutilizam o visual Baby escalado. Todos continuam
passivos nesta implementação; não há sistema de combate avançado.

Após aplicar o Core Divino, use o botão God no Bastão ou `/soulbound god`.
A forma dura **60 segundos**, com cooldown de **15 minutos a partir da ativação**,
controlado pelo relógio persistente do servidor.

### Restauração

Exige companheiro DEAD e ressurreição necessária. Reutiliza os mesmos UUIDs de
proprietário, vínculo e companheiro. O retorno é Baby em Seguir, progresso zero,
capacidade/cooldown God reiniciados, preservando o histórico de ressurreições.
MISSING não autoriza recriação automática. Recuperar apenas o ovo nunca substitui
o ritual de Restauração.

## 8. Comandos e identificadores úteis

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

## 9. Problemas comuns e estado de validação

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
