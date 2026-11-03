# Lupus Pleno — Brambleback

Visual adaptado de **Brambleback**, de **monkus**:
https://blockbenchworkshop.com/model/monkus/brambleback

A página declara [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).
Créditos, origem e mudanças acompanham o JAR em
`assets/soulbound/credits/brambleback.txt`. Geometria adaptada, animações do modelo,
importador e textura associada estão sob essa licença específica, como exceção à
licença geral do projeto. Não se atribui licença própria à arte de terceiros.

## Integração

- 58 cubos visuais e 25 ossos convertidos para geometria Java nativa.
- Preservados pivôs, rotações, dilatação e espelhamento UV. O arquivo é do formato
  genérico antigo, com ossos definidos diretamente no outliner.
- PNG original 128×128 extraído sem alteração; UV lógico 64×64, conforme a textura
  principal. O `resolution` global 1×1 corresponde ao auxiliar e não é usado.
- Grupo/cubo `hitbox` e textura auxiliar excluídos apenas da renderização. A hitbox
  real da entidade não foi alterada. O arquivo `.bbmodel` original permanece intacto.
- Novo modelo selecionado somente em Pleno. Baby, Junior Irushia e God preservados.
  O antigo modelo Pleno permanece nos fontes, sem seleção pelo renderer.
- Animações procedurais de repouso, caminhada, golpes alternados, golpe pesado,
  investida, preparação, ultimate e ritual usam os estados existentes. Os clipes
  `run`/`attack` do arquivo não são reproduzidos diretamente.
- O machado antigo não integra este visual: os braços representam os golpes.
  Equipamentos continuam invisíveis. Dano, cooldowns, atributos, identidade,
  navegação, progressão e rituais permanecem com as mesmas regras.

## Arquivos

Adicionados: `client/BramblebackPlenoGeometry.java`, `client/BramblebackPlenoModel.java`,
`textures/entity/lupus_pleno_brambleback.png`, `credits/brambleback.txt`,
`tools/import-brambleback-pleno.ps1`, `client/BramblebackPlenoChecks.java` e este guia.
Alterados: renderer Lupus, registro cliente de layers, `BabyVisualChecks`,
verificação do JAR no Gradle e manual do jogador.

Para regenerar o visual a partir do arquivo na raiz:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/import-brambleback-pleno.ps1
.\gradlew.bat build --console=plain
```

## Testes

Verificações automatizadas: bake real do modelo, quantidade de partes, exclusão do
auxiliar, limites UV, poses finitas, reset por frame, dimensões do PNG e créditos.
Log do build: `build/brambleback-pleno-build.log`.

Resultado executado: **BUILD SUCCESSFUL**, com **15.513 verificações do modelo
Brambleback** aprovadas e as suítes anteriores preservadas. Textura conferida como
idêntica ao PNG embutido; geometria, PNG e créditos confirmados no JAR
`build/libs/soulbound-1.0.0.jar`. Não foi executada homologação visual em jogo.

Homologação visual pendente:

1. Execute `runClient` e selecione Pleno no Bastão em um vínculo que o desbloqueou.
2. Confira frente/laterais/costas, folhas, rosto, braços e pernas.
3. Teste repouso, caminhada, Seguir, Ficar, combo, investida e ultimate.
4. Confira proporções, clipping e alinhamento com F3+B; não houve alteração da hitbox.
5. Alterne Baby/Junior/Pleno/God e confira que somente Pleno usa Brambleback.

Não há homologação de aparência ou jogabilidade sem inspeção real no Minecraft.
