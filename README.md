# SC2002 Restaurant Rush

Java command-line restaurant management game for the SC2002 group assignment.

The [assignment brief](SC2002_Restaurant_Rush_Group_Assignment.docx.pdf) is the source of truth for the game rules and submission requirements.

The [component contract](docs/component-contract.md) records the team's agreed rules, responsibilities, turn timing, implementation decisions, and the shared Java API that every workstream issue builds against.

## Project status

The repository currently contains the assignment brief and project setup. The game has not been implemented yet.

## Planned stages

| Tag | Required milestone |
| --- | --- |
| `stage-1` | Complete base game with Regular customers and both outcomes |
| `stage-2` | Add VIP and Critic customer behavior |
| `stage-3` | Add Host, ComboMeal, and Happy Hour |

Each tag will identify a tested, runnable commit. Build, run, and fixture replay commands are in the contract (Sections 5 and 6). Stage change summaries, reproducible run notes, and video timestamps will be added as the corresponding work is completed.

## Team workflow

Track a coherent piece of work in each GitHub issue using the work-item form. Work on a branch, run `scripts/replay.sh`, open a pull request using the repository template and link it to the issue, then review and merge it. When every issue for a stage is merged, run the stage gate in the contract (replay plus a manual play-through), open fix issues for anything outstanding, and tag the passing commit. Next-stage PRs are not merged until that tag exists. Specify the shared Java API in the contract before assigning dependent implementation work.
