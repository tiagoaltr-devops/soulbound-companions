# Correção 6.7.2 — Junior e Pleno

## Diagnóstico

Os PNGs antigos tinham as dimensões esperadas (Junior 64×64, Pleno 128×128).
Não foi identificada troca de textura/modelo pelo renderer, escala duplicada ou
UV fora do atlas. O resultado relatado decorre principalmente de caixas longas
sem articulações, falta de cintura/joelhos/botas, texturas em grade repetida e
pouca diferenciação de materiais. Alguns UVs eram compartilhados entre peças
simétricas, o que por si só não constitui erro. O machado Pleno era uma placa
retangular, animada separadamente dos braços: a pegada não acompanhava o cabo.

## Correções visuais

- Junior: cabeça menor, peitoral leve, cintura estreita, ombreiras discretas,
  cotovelos e joelhos articulados, manoplas e botas. A lâmina espiritual continua
  integrada à mão direita, através da hierarquia do antebraço.
- Pleno: peitoral reforçado, cintura e cinturão, ombreiras volumosas, braços
  robustos articulados, joelheiras, botas e biqueiras em relevo. Cabeça de sete
  unidades para corpo de aproximadamente 36,5, distinguindo-o do Junior jovem.
- Machado: cabo com amarrações, pomo, núcleo rúnico, duas lâminas com contorno
  escalonado e bordas metálicas claras. Continua parte da malha, sem item equipável.
- Os braços Pleno usam pose de duas articulações para levar os punhos a dois
  pontos do cabo já transformado. Machado, braços, cabeça e peitoral compartilham
  a transformação do torso. Golpes laterais, impacto, preparação, corrida,
  ritual, reação a dano e morte usam os mesmos estados existentes.
- Atlas novos por peça: metal escuro, contornos dourados, olhos espirituais e
  runas ciano localizadas. Sem grade repetida sobre o corpo inteiro. Arte original
  criada por pixels no gerador, sem ler/copiar imagens de outros mods.

Não houve mudança em renderer, RenderState, escala da entidade, atributos,
hitboxes, combate, progressão, talentos, rituais ou persistência. A extração de
`actionProgress` já existente é reutilizada. O sistema continua usando
EntityModel/LayerDefinition/ModelPart da versão instalada, exclusivamente cliente.

## Arquivos

- `client/JuniorCompanionModel.java`, `client/PlenoCompanionModel.java`.
- Novo `client/WarriorArmPose.java`: cálculo da pose visual dos braços.
- `textures/entity/lupus_junior.png` e `lupus_pleno.png`.
- Novo `tools/generate-warrior-visuals.ps1`: geração reproduzível, validação de
  limites e de sobreposição de 14/23 regiões UV, respectivamente.
- Novo `src/test/java/br/com/soulbound/client/WarriorVisualChecks.java`, chamado
  pelo `BabyVisualChecks` existente, e este documento.

## Validação e limites

`./gradlew.bat build --console=plain`: **BUILD SUCCESSFUL**, sem `clean`.
Passaram **1.098 verificações**, incluindo **427 novas**: montagem das duas malhas,
210 poses amostradas com verificação de ambos os punhos, ritual, morte e reset.
O gerador também validou limites/sobreposição dos atlas; os testes anteriores
confirmaram as dimensões dos PNGs e passaram sem alterações em mecânicas.
O primeiro build foi interrompido por BOM UTF-8 inserido pelo PowerShell em dois
arquivos Java; a codificação foi corrigida e a execução repetida passou.

Os testes montam as malhas e amostram poses reais, sem OpenGL. Conferem alcance
e alinhamento dos punhos, existência da hierarquia e reset das transformações.
Isso não valida a qualidade visual, oclusão, iluminação ou toda interseção entre
volumes. Não foi executado Minecraft nesta correção.

Para homologar, rode `./gradlew.bat runClient` e confira frente, costas e perfil
nas duas formas, com escala de GUI e iluminação usuais. Observe idle, andar/correr,
olhar para cima/baixo, três golpes, investida, abalo e R (preparação/impacto).
Confira mãos no cabo, bordas das lâminas, ombreiras, joelhos, botas, reação a dano,
morte, ritual e troca de formas. Repita com dois jogadores para observar a
interpolação. O estilo permanece voxel; não há simulação física de armadura
nem prevenção geral de interseções de malha em tempo real.
