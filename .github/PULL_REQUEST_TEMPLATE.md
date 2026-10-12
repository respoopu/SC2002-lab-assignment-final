## Linked work

Closes #
<!-- Add the issue number above so merging this PR closes the issue. -->

- Stage / workstream:
- Contract sections and approved decisions used:

## What changed

- Goal delivered:
- Main files and responsibilities changed:
- Shared inputs, outputs, states, or method signatures changed (or `None`):
- Existing classes changed for a later stage, and why (or `N/A`):

## Verification

Give commands and observed results. Include a reproducible input sequence or transcript when behavior depends on manager choices.

| Check | Command or scenario | Observed result |
| --- | --- | --- |
| Build and fixture replay (`scripts/replay.sh`) |  |  |
| Required success and edge cases from the issue |  |  |
| Earlier-stage regression, if applicable |  |  |

## Design note for the final report

Keep this brief. Record only principles and trade-offs that the code actually demonstrates; use `N/A` where appropriate.

- **Responsibility and cohesion:** What behavior belongs to this component, and why?
- **Extensibility and principles:** Which design decision supports later features or a relevant OO/SOLID principle?
- **Trade-off or alternative:** What meaningful alternative was considered, and why was this choice made?
- **Diagram / stage comparison impact:** What must change in the class or sequence diagram, README, or stage change log?

## Review checklist

- [ ] The linked issue's acceptance criteria are met or exceptions are explained above.
- [ ] The relevant contract and assignment rules are followed; any changed contract is included in this PR.
- [ ] Invalid choices leave game state and staff capacity unchanged, if this PR handles choices.
- [ ] Build and checks were run from the documented commands, or the reason they could not run is stated above.
- [ ] Fixtures under `fixtures/` and documentation were updated where this PR changes observable behavior, and any regenerated `.expected` file was reviewed.
- [ ] I reviewed and understand the submitted changes, including any AI-assisted work, and can explain them in the group report or video.
