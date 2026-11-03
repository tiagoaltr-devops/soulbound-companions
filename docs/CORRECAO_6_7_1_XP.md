# Correção 6.7.1 — XP e homologação administrativa

## Diagnóstico

A lista de origens permitidas em `ProgressionService.eligibleSpawn` excluía
`COMMAND` e `SPAWN_ITEM_USE`. Assim, dano efetivo a mobs de `/summon` e ovos não
registrava participação para XP. Na fonte instalada, `/summon` com NBT pode pular
`finalizeSpawn` e deixar `Mob.getSpawnType()` nulo, também recusado pela política.
Essa é uma causa verificável no código para os testes relatados; não houve
reprodução na partida do usuário nem inspeção dos NBTs desses mobs.

Não existe trava no nível 3: são necessários 110 XP para 3 → 4. A fórmula,
o limite 30 e os valores de combate (8/25/200 por classe de hostil) foram mantidos.
Não existe cooldown entre recompensas de mobs distintos: há deduplicação por UUID
do mob e uma janela de participação de 300 ticks. O dano precisa ser aceito pelo
servidor e reduzir vida/absorção. O dono não precisa dar o golpe final: a política
existente premia cada vínculo com participação recente. Junior básico/ultimate e
os ataques Pleno passam pelo mesmo `JuniorCombat.hit` e registro de participação.
Mob natural com IA permanece elegível; dano recusado e mero unload não dão XP.

## Opção de desenvolvimento

Em `<mundo>/serverconfig/soulbound-server.toml`, com o mundo/servidor desligado:

```toml
developmentAllowArtificialCombatXp = true
```

O padrão é **false**. Ative apenas no mundo de homologação. Permite origens
COMMAND, SPAWN_ITEM_USE e origem ausente (necessária para certos `/summon` com NBT).
Uma origem ausente não permite distinguir comando, entidade antiga ou outro mod;
essa ampliação é deliberadamente limitada ao modo de desenvolvimento. Origens
conhecidas de spawner/trial spawner/dispenser continuam bloqueadas, assim como
NoAI, guardiões da provação, jogadores, aliados e duplicação de recompensa.
Não há XP retroativo por mortes anteriores ou golpes anteriores à ativação.
A elegibilidade também é conferida na remoção definitiva por morte.

## Comandos

Exigem operador nível 2 (permissão vanilla de gamemaster) e jogador executor.
Não recebem seletor ou UUID de outro proprietário e não criam vínculo:

```text
/soulbound debug setlevel 3
/soulbound debug addxp 8
/soulbound debug addxp 102
/soulbound debug setlevel 15
```

`setlevel` aceita 1–30, zera o XP dentro do nível e define os pontos adquiridos
como nível menos um. Conserva investimentos e rejeita orçamento/pré-requisitos
inválidos antes de alterar dados. Repetir o comando não acumula pontos.
`addxp` aceita inteiro positivo e usa `ProgressionState.gain`, sem bônus de
combate/exploração, respeitando level-ups e o teto existente. No nível 30, XP é zero.
Ambos preservam identidade, formas, estado de vida, talentos e cooldowns;
recusam alterações durante ritual, persistem imediatamente e publicam o estado
para entidade, item e HUD. Nenhuma cura foi adicionada.

## Arquivos

- `Config`: opção SERVER de desenvolvimento, desligada por padrão.
- `ProgressionService`: elegibilidade controlada e publicação imediata após XP.
- `ProgressionCommands` (novo): comandos administrativos e validação de nível.
- `CompanionHudSync`: entrada para atualização imediata reutilizando o pacote atual.
- `ProgressionDebugChecks` (novo) e `AltarChecks`: verificações direcionadas.
- Traduções `pt_br.json`/`en_us.json`, manual e este documento.

## Validação

Única execução de `./gradlew.bat build --console=plain`: **BUILD SUCCESSFUL**,
com **671 verificações aprovadas**, sendo **51 novas** de progressão administrativa.
As novas verificações cobrem 3 → 4, nível 15, repetição idempotente de setlevel,
orçamento e pré-requisitos dos talentos, teto 30, matriz de origens/NoAI,
deduplicação de recompensas, Junior/Pleno, projeção de HUD e roundtrip do codec.
As verificações anteriores de identidade, Recall, talentos, altar e Pleno passaram.
`git diff --check` também passou. Nenhum `clean` ou build adicional foi executado.
Os testes automatizados de regras/codec não simulam comandos em rede nem mortes
reais. Minecraft não foi iniciado nesta tarefa.

Homologação manual pendente:

1. Como OP, executar os quatro comandos acima: 3/0 → 3/8 → 4/0 → 15/0.
2. Repetir `setlevel 15`; conferir 14 pontos totais, descontando os investidos.
3. Investir talentos e tentar redução inválida; dados devem permanecer intactos.
4. Em Seguir/Combate, usar `/summon minecraft:zombie ~3 ~ ~`, ovo e mob natural;
   testar opção desligada/ligada, com Junior e Pleno, ataques básicos e especiais.
5. Aguardar a remoção do cadáver: cada mob válido concede uma recompensa, não uma
   por golpe. Dois mobs distintos podem conceder XP sem cooldown entre si.
6. Testar `NoAI:1b` e spawner: continuam sem XP mesmo com a opção ligada.
7. Conferir HUD logo após comando/XP, relogar e reiniciar; verificar persistência.
8. Como jogador sem OP, tentar ambos os comandos: acesso deve ser negado.
9. Conferir que Ascensão ainda recusa nível 14 e admite nível 15 com os demais
   requisitos existentes. Não foram alterados ritual, núcleo, atributos ou cura.
