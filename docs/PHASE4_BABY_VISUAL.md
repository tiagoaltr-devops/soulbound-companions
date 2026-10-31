# Fase 4 — Identidade visual e movimentação Baby

Minecraft Java 1.21.11, NeoForge 21.11.48 e Java 21. Nenhuma versão de dependência
ou arquivo do wrapper foi alterado. As instruções do AGENTS.md foram lidas; o
pedido desta fase amplia a apresentação e a movimentação do Baby existente.

## Modelo e textura

`BabyCompanionModel` é um `EntityModel<BabyCompanionRenderState>` próprio, criado
por `LayerDefinition`, com cabeça 7×6×6, corpo 4×5×3, dois braços 2×4×2, duas asas
1×6×7 e duas pequenas pontas na cabeça. O conjunto tem aproximadamente 14 pixels
de altura. A escala Baby já está na geometria; `getAgeScale()` retorna 1 para não
reduzir o modelo/hitbox novamente. A entidade permanece no estágio BABY e continua
com 12 pontos de vida. A hitbox nominal é 0,45 × 0,90 blocos, sem incluir as asas.

A base geométrica, o render state e as animações ficam no pacote cliente e não
dependem da espécie. Somente `LupusCompanionRenderer` seleciona a textura do Lupus.
Isso permite reutilização futura da base, sem registrar Drakos ou Terran agora.

Textura original: `src/main/resources/assets/soulbound/textures/entity/lupus_baby.png`.
É um PNG RGBA de 64×64, sem filtragem desenhada ou dependências externas, com
paleta azul-claro, olhos verde-água com reflexos claros, pontas/cunhos azul-escuro
e marcas de meia-lua que identificam o Lupus. As asas têm recortes transparentes
e padrões mágicos em pixels. Não há camada emissiva: o brilho dos olhos é uma
escolha de cores, sujeita à iluminação do ambiente.

A arte foi desenhada pixel a pixel por `tools/New-LupusBabyTexture.ps1`, sem ler
imagens externas. Para regenerar no Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/New-LupusBabyTexture.ps1
```

O script faz parte do projeto e usa System.Drawing; o mod distribuído precisa
apenas do PNG gerado, não do PowerShell. Nenhum arquivo da skin de terceiros ou
da textura vanilla do Allay foi copiado. A página indicada pelo usuário retornou
403 na consulta; o Allay vanilla foi estudado apenas como referência técnica de
proporções e voo. A textura original segue a licença já declarada pelo projeto.

### Mapeamento UV

Coordenadas em pixels; os limites abaixo são inclusivos.

| Parte | Origem UV | Área do atlas |
| --- | --- | --- |
| Cabeça | 0, 0 | U 0–25, V 0–11 |
| Corpo | 0, 16 | U 0–13, V 16–23 |
| Braço esquerdo | 16, 16 | U 16–23, V 16–21 |
| Braço direito | 24, 16 | U 24–31, V 16–21 |
| Ponta esquerda da cabeça | 40, 0 | U 40–47, V 0–4 |
| Ponta direita da cabeça | 48, 0 | U 48–55, V 0–4 |
| Asa esquerda | 0, 32 | U 0–15, V 32–44 |
| Asa direita | 20, 32 | U 20–35, V 32–44 |

Cada caixa usa a disposição padrão do CubeListBuilder: topo e base na primeira
faixa de profundidade; laterais, frente e costas na faixa seguinte. A frente da
cabeça ocupa U 6–12 e V 6–11. Os olhos estão em U 7–8 e U 10–11, V 7–9. Para
substituir a arte, mantenha 64×64, transparência e essas regiões, salve no mesmo
caminho e recarregue os recursos com F3+T. Não é necessário alterar a geometria.

## Navegação e animação

As assinaturas foram conferidas nas fontes locais de 1.21.11. A entidade usa
`FlyingPathNavigation`, `FlyingMoveControl(this, 20, true)` e `travelFlying`, como
o caminho de voo da versão atual do Allay, mantendo suas próprias metas e sem
herdar coleta, duplicação ou IA de combate do Allay. A gravidade é desativada,
inclusive ao carregar uma entidade antiga. Os cálculos normais de dano de queda
são ignorados; dano de fogo, sufocamento, lava e outros perigos continuam possíveis.

Follow procura posições livres perto da altura do tronco do dono, incluindo
ajustes laterais/verticais quando a posição preferida está bloqueada. O pathfinder
de voo trata os obstáculos no trajeto; o controlador mantém aceleração, atrito e
orientação vanilla. A posição física é controlada pelo servidor, com interpolação
normal do cliente. Não há teleporte para acompanhar o jogador.

Ficar preserva o campo `StayAnchor` existente. O ponto de pairar é calculado
0,45 bloco acima dessa âncora para dar espaço ao corpo. O servidor corrige pequenos
desvios locais; retornos maiores usam pathfinding. Fuga e recuo podem interromper
Ficar temporariamente e, depois, a entidade tenta retornar ao ponto. A navegação
é parada e o impulso residual é zerado ao encerrar o movimento.

Prioridade das metas:

1. Fuga de ameaça próxima, agora escolhendo destinos aéreos alcançáveis.
2. Recuo curto de até 1,5 bloco por alerta, se existir caminho seguro candidato.
3. Retorno ao ponto de Ficar.
4. Follow tridimensional do proprietário.
5. Pairar no local, com pequena decolagem quando apoiado no chão.
6. Olhar ocioso.

| Animação | Implementação |
| --- | --- |
| Idle | Oscilação visual suave e batimento lento das asas |
| Flying/Following | Asas com frequência/amplitude maiores segundo o deslocamento e inclinação do corpo; orientação vanilla interpolada |
| Stay | Pairar na âncora, sem a antiga pose sentada de lobo |
| Threat Alert | Olhar para o hostil, leve reação da cabeça/braços e recuo curto com navegação; fuga continua prioritária |
| Ore Detection | Elevação visual curta, balanço do corpo e braços erguidos, além do som/partículas existentes |

`THREAT_ALERT_END` e `MINERAL_DISCOVERY_END` são prazos temporários enviados por
`SynchedEntityData` a partir do servidor. O cliente calcula a pose, sem realizar
buscas ou decidir movimentos. Esses sinais não são campos da identidade nem
mudam os dados persistentes do vínculo. As poses são reiniciadas antes de cada
animação para evitar acumulação de transformações.

## Integração e limites

O tipo continua `soulbound:lupus_companion`; o item continua
`soulbound:companion_egg`. OwnerUUID, SoulboundBond, UUID da entidade, estágios,
Follow/Stay, os estados da identidade, o registro SavedData e `/soulbound recover`
não foram substituídos. Nenhum arquivo do pacote de identidade foi alterado nesta
fase. Alertas, minérios, ausência de ataques e validação do proprietário permanecem.

Limitações conhecidas:

- Caminhos dependem dos chunks disponíveis, espaço livre e alcance da navegação.
  Uma sala fechada, porta fechada ou corredor baixo pode impedir o acompanhamento.
  O Baby não abre portas nem atravessa paredes.
- Não há recall automático, voo entre dimensões ou carregamento de chunks para
  recuperar um companheiro distante. A busca mineral continua limitada a chunks
  carregados e ao orçamento configurado na Fase 2.
- A oscilação fina e a elevação de descoberta são visuais; a hitbox não oscila
  junto. As asas não participam das colisões.
- O recuo é tentado uma vez por alerta e pode não ocorrer se nenhum destino
  candidato for alcançável. A fuga não garante sobrevivência ou uma rota livre
  de todos os hostis. Em água/lava, o voo usa o tratamento de fluidos vanilla.
- Um ponto antigo de Ficar que esteja obstruído pode impedir o retorno. Pode-se
  emitir novamente Ficar em local livre para definir outra âncora.
- Os eventos visuais são temporários: sair/reabrir não reproduz uma animação
  antiga. Ajustes finos de velocidade, asa e bob precisam de avaliação em jogo.

## Arquivos desta fase

Criados sob `src/main/java/br/com/soulbound/`:

- `client/BabyCompanionModel.java`.
- `client/BabyCompanionRenderState.java`.
- `entity/ai/BabyFlightTargets.java`.
- `entity/ai/BabyHoverGoal.java`.
- `entity/ai/ThreatRecoilGoal.java`.

Modificados sob o mesmo diretório:

- `SoulboundCompanionsClient.java`: registro da LayerDefinition somente no cliente.
- `client/LupusCompanionRenderer.java`: modelo e textura próprios.
- `entity/LupusCompanionEntity.java`: controle de voo e sinais de animação.
- `entity/ai/FollowCompanionOwnerGoal.java`: acompanhamento 3D.
- `entity/ai/BabyFleeThreatGoal.java`: candidatos de fuga aéreos.
- `entity/ai/ReturnToStayGoal.java`: retorno ao ponto de pairar.
- `companion/ThreatSensor.java`: sinal visual do alerta existente.
- `companion/MineralInstinct.java`: sinal visual da descoberta existente.
- `registry/ModEntities.java`: dimensões físicas do novo corpo.

Criados também:

- `src/main/resources/assets/soulbound/textures/entity/lupus_baby.png`.
- `tools/New-LupusBabyTexture.ps1`.
- `src/test/java/br/com/soulbound/client/BabyVisualChecks.java`.
- Este guia.

Modificados também: `build.gradle`, para executar os checks visuais sem novas
dependências, e `README.md`, para atualizar a apresentação e o fluxo de testes.
Alterações das fases anteriores já presentes no workspace foram preservadas.

## Validação e roteiro manual

`./gradlew.bat clean build`: **BUILD SUCCESSFUL**, com **24 verificações de
identidade** e **20 verificações visuais automatizadas**. Os checks visuais fazem
bake do modelo real sem OpenGL, exercitam poses, limites de olhar e reset das
transformações e abrem o PNG empacotado para conferir dimensões, alpha e paleta.
Eles não aprovam aparência em jogo ou navegação. O servidor dedicado foi iniciado
até `Done`, sem erro de classes de cliente; foi encerrado após a checagem.

Testes manuais ainda pendentes:

1. Execute `./gradlew.bat runClient`. Entre num mundo de teste, receba ou recupere
   o Ovo da Alma e invoque uma única vez. Confira a criatura, textura, olhos,
   proporções e asas por frente, costas e laterais, durante o dia e à noite.
2. Confira idle e Ficar por alguns minutos: deve pairar sem cair, sem deriva
   contínua e sem acumular rotação. Use F3+B para comparar modelo e hitbox.
3. Em Follow, caminhe, corra, suba escadas/lajes e voe no Criativo. Teste subida
   vertical e descida, mantendo distâncias dentro do alcance da navegação.
4. Atravesse vegetação, contorne paredes, passe por portas abertas e teste teto
   baixo. Confirme colisões e que não atravessa portas fechadas ou sólidos.
5. Mantenha Ficar, afaste-se e volte: o Lupus deve conservar a região da âncora.
   Provoque recuo/fuga e remova o hostil; confira o retorno aéreo e o modo salvo.
6. Invoque hostis entre 6–12 blocos: confira som, olhar e recuo quando houver
   espaço. Aproxime um hostil a menos de 4 blocos: confira fuga sem ataques.
7. Ponha minério raro a até 12 blocos e aguarde uma busca completa. Confira
   partículas/som e a reação de descoberta, sem coordenadas ou marcação do minério.
8. Teste alturas elevadas e pouso contra plataformas: o voo normal não deve
   causar dano de queda. Confira separadamente que outros danos continuam ativos.
9. Salve/reabra uma entidade das fases anteriores. Compare UUID, OwnerUUID,
   SoulboundBond, modo e âncora, além da tooltip e `/soulbound recover`.
10. Em servidor dedicado, conecte dois jogadores e observe o mesmo Lupus em voo.
    Confira sincronização, comandos exclusivos do dono, ausência de entidades
    duplicadas e os mesmos sinais visuais para quem acompanha a cena.

Não foram declarados aprovados testes de jogabilidade, aparência em cena ou
multiplayer desta fase sem sua execução real.
