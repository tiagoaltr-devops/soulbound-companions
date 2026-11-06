# Ignited_Sword_Entity

Classe: `com/github/L_Ender/cataclysm/entity/Pet/Summoned_Entity/Ignited_Sword_Entity` (com/github/L_Ender/cataclysm/entity/Pet/Summoned_Entity/Ignited_Sword_Entity.class). Mob concreto por herança; registro/invocação não comprovados.

Inventário técnico: [JSON](Ignited_Sword_Entity-f67c288c858f.json). Plano: [proposta](../proposals/Ignited_Sword_Entity-f67c288c858f.json).

Viabilidade: BAIXA, conservadora; port, permissões e fluxo de comportamento pendentes.

Análise estática parcial: referências não provam execução. Não interpreta fluxo de controle, reflection, mixins ou configuração em runtime. Valores, prioridades, triggers, fases e cooldowns sem vínculo comprovado permanecem desconhecidos. Classes abstratas e projéteis não são mobs catalogados. Relações por nome são inferências; recursos compartilhados ficam no índice global. Compatibilidade binária e funcionamento no servidor não foram testados.
