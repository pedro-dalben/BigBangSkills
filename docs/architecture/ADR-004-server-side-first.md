# ADR-004 — server-side first e segurança authoritative

* Status: aceito para a Fase 0
* Data: 2026-08-10

## Decisão

A progressão funciona em dedicated server sem mod cliente. Chat, action bar, title, boss bar, scoreboard, som e partículas são os primeiros canais. Um cliente opcional poderá renderizar HUD/árvore, mas nunca será fonte de XP, level, cooldown ou reward.

## Razão

Isso mantém compatibilidade com rede e modpacks diferentes, permite rollout progressivo e reduz o custo do primeiro vertical slice. O servidor calcula, valida, persiste e notifica.

## Limites

Menus e HUD customizados ficam fora da primeira fase. Networking futuro será somente informação derivada e terá checagem de permissão/estado no servidor. Abilities com mutação sempre passam por evento/proteção do loader.
