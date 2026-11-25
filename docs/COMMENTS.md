# Comment and Naming Rules

- Functional signals use descriptive names without simulation prefixes.
- Simulation-only signals use the `obs` prefix.
- Comments describe current behavior only.
- Do not keep historical comments, migration notes, TODO markers, or difftest references in RTL.
- Do not describe unimplemented behavior as implemented.
- Device limitations belong in `docs/DEVICES.md`, not in RTL comments.
- Use one language consistently within a file.
- Keep comments short and place them next to the signal or block they describe.
- Do not add comments to generated files or scripts.
