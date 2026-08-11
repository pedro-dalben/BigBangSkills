# mcMMO parity baseline

Repository: `mcMMO-Dev/mcMMO`

Reference commit: `ad8444c2f394ee97c510acdfc7b23623885b071c`

Reference date: `2026-07-30`

Reference branch: `master` at the commit above; the working clone is `/tmp/mcmmo-reference`.

Reference platform: Paper/Bukkit plugin, `api-version: 1.20.5`; the repository contains optional/future Paper API references up to 1.21.11. BigBangSkills targets Fabric and NeoForge 1.21.1, so later content is not silently treated as vanilla content.

This commit is the only mcMMO source baseline for the current parity work. The clone must not be fast-forwarded during implementation without updating this file and the parity tables.

Implementation rule: BigBangSkills re-derives behavior from inputs, conditions, formulas, outputs and side effects. It does not use mcMMO classes, source, tests, configuration files or a runtime dependency.
