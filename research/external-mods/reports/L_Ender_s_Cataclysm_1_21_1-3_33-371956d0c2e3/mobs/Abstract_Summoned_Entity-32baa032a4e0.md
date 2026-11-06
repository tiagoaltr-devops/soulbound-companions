# Abstract_Summoned_Entity

Classe: `com/github/L_Ender/cataclysm/entity/Pet/Summoned_Entity/Abstract_Summoned_Entity` (com/github/L_Ender/cataclysm/entity/Pet/Summoned_Entity/Abstract_Summoned_Entity.class). Mob concreto por herança; registro/invocação não comprovados.

Inventário técnico: [JSON](Abstract_Summoned_Entity-32baa032a4e0.json). Plano: [proposta](../proposals/Abstract_Summoned_Entity-32baa032a4e0.json).

Viabilidade: BAIXA, conservadora; port, permissões e fluxo de comportamento pendentes.

Análise estática parcial: referências não provam execução. Não interpreta fluxo de controle, reflection, mixins ou configuração em runtime. Valores, prioridades, triggers, fases e cooldowns sem vínculo comprovado permanecem desconhecidos. Classes abstratas e projéteis não são mobs catalogados. Relações por nome são inferências; recursos compartilhados ficam no índice global. Compatibilidade binária e funcionamento no servidor não foram testados.
