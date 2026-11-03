# Lupus Junior — visual Irushia

A forma Junior usa agora o modelo **Baby Dragon Irushia**, de **kyoukster**:
https://blockbenchworkshop.com/model/kyoukster/baby-dragon-irushia

A página declara licença [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
Crédito, origem, licença e alterações estão incluídos no JAR em
`assets/soulbound/credits/irushia.txt`.

## Integração

- Importados 39 cubos e 38 ossos, incluindo pivôs, rotações, espelhamento UV e
  dilatação das peças. Textura original 64×64 extraída sem alteração do PNG embutido.
- Conversão para `LayerDefinition` nativo, sem GeckoLib ou dependências novas.
- O renderer seleciona o novo modelo/textura somente em Junior. O antigo modelo
  guerreiro permanece nos fontes, mas não é selecionado para renderizar Junior.
- Animações procedurais novas usam o rig importado para repouso, caminhada, braços,
  asas, cauda, ataque, ultimate, transformação e ritual. A animação `idle` embutida
  no Blockbench não foi convertida: expressões e curvas do editor não são executadas.
- Não há mudança de espécie, nome, UUID, atributos, hitbox, navegação terrestre,
  progressão, equipamento, arma funcional, dano, duração ou cooldown. O dragão
  movimenta as asas visualmente; isso não concede voo à forma Junior.
- Baby, Pleno e God mantêm seus modelos e animações.

O arquivo `.bbmodel` da raiz permanece intacto e fora do JAR. Para regenerar apenas
a geometria Java e extrair novamente sua textura:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/import-irushia-junior.ps1
.\gradlew.bat build --console=plain
```

O importador é específico para a estrutura/UV de caixas desse arquivo; não é um
carregador genérico de qualquer projeto Blockbench.

## Validação

As verificações automatizadas fazem bake do modelo real, conferem a quantidade de
ossos/cubos, limites UV, animações finitas, reset de pose, PNG e atribuição.
O build executa essas verificações junto da suíte existente.

Resultado executado: `build --console=plain` **BUILD SUCCESSFUL**; 7.496 verificações
do modelo Irushia aprovadas, além das suítes anteriores. Log:
`build/irushia-junior-build.log`. JAR: `build/libs/soulbound-1.0.0.jar`.

Arquivos adicionados: `IrushiaJuniorGeometry.java`, `IrushiaJuniorModel.java`,
`IrushiaJuniorChecks.java`, importador PowerShell, PNG, créditos e este guia.
Alterados: renderer Lupus, registro cliente de layers, `BabyVisualChecks`,
verificação de recursos do JAR no `build.gradle` e manual do jogador.

Homologação em jogo pendente:

1. Execute `runClient` e selecione Junior no Bastão, em vínculo que já desbloqueou a forma.
2. Confira frente, costas e laterais, olhos, orelhas, asas e cauda; observe clipping.
3. Teste repouso, Seguir, Ficar, caminhada, ataques, ultimate e ritual.
4. Alterne Baby/Junior/Pleno/God; somente Junior deve usar o dragão.
5. Confira a correspondência visual com a hitbox existente usando F3+B.

Não foi declarada aprovação visual ou de jogabilidade por esses testes automatizados.
