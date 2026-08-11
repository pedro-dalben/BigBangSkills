# BigBangSkills

Server-side Fabric/NeoForge skill progression for Minecraft 1.21.1, implemented independently with mcMMO used only as a fixed behavioral reference.

## Development

- Branch: `feat/bigbangskills-base`
- Baseline: [mcMMO parity baseline](docs/reference/MCMO_PARITY_BASELINE.md)
- Skill matrix: [MCMO_SKILL_PARITY.md](docs/reference/MCMO_SKILL_PARITY.md)
- Configuration mapping: [MCMO_CONFIG_MAPPING.md](docs/reference/MCMO_CONFIG_MAPPING.md)
- Known differences: [MCMO_KNOWN_DIFFERENCES.md](docs/reference/MCMO_KNOWN_DIFFERENCES.md)
- Final audit: [FULL_SKILL_PARITY_AUDIT.md](docs/audits/FULL_SKILL_PARITY_AUDIT.md)

Every feature change should be committed in a small thematic commit, preferably one module or skill at a time. XP values, difficulty, cooldowns, unlocks and restrictions must remain traceable to the fixed mcMMO baseline or be documented as an explicit incompatibility.

Minecraft runtime smoke tests are manual-only. Automated validation is limited to common/API/persistence tests, compilation, static checks and documentation audits.
