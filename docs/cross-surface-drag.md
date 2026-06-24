# Design: cross-surface drag (dock ⇄ home)

Date: 2026-06-24 · Status: approved, in implementation

## Goal
Drag an app icon **out of the dock onto the home screen**, and a **home icon into the dock** —
in both directions, the way real launchers (Launcher3) do it.

## Approach (Launcher3 model, adapted to Compose)
Launcher3 uses one full-screen **drag layer** + a central **drag controller** with registered drop
targets; the finger position routes the drop. Compose constraint: a pointer gesture is owned by the
node that captured the DOWN and can't be transferred mid-gesture. So:

- **Pickup + gesture detection stay per surface** (Workspace icon, Dock icon) — the delicate
  long-press / tap / swipe logic in each is preserved (see memory `home-drag-gesture-architecture`).
- **The floating icon and drop routing are hoisted to `HomeScreen`** via a shared
  `HomeDragController` — this is the drag layer / controller equivalent.

## Components
- **`HomeDragController`** (remembered in `HomeScreen`): `draggedApp`, `source` (Home/Dock),
  `rootPosition` (finger, root coords), grid metrics (cellW/cellH/columns/rows/currentPage,
  workspace origin), and bounds `workspaceBounds` / `dockBounds` (root coords).
- **`HomeScreen`**: owns the controller, captures Workspace/Dock bounds via `onGloballyPositioned`
  (`boundsInRoot`), and draws the **single floating icon** above both surfaces while dragging.
- **`Workspace`**: keeps internal pickup/drag; updates the controller (root position + grid metrics);
  its own floating overlay is removed (HomeScreen draws it). On drop: over dock → move to dock; else
  existing home move/swap.
- **`Dock`**: keeps internal long-press; horizontal travel = reorder (unchanged); updates the
  controller for vertical/out travel. On drop: over workspace → move to home at the finger's cell;
  else existing reorder.

## Drop semantics (move semantics, like real launchers)
- **Dock → home**: place at the cell under the finger, swap if occupied (`placeAt`); **remove from dock**.
- **Home → dock**: insert at the slot under the finger (`addToDockAt`); **remove from home**. If the
  dock is already full (`dockColumns` visible), **block** the drop (haptic tick, no change).
- **Same surface**: unchanged (home move/swap; dock reorder).
- **Dropped on neither** (gap): cancel — icon returns to origin.

## Repository changes
- `HomeLayoutRepository.placeAt(app, page, cellX, cellY): Boolean` — insert-or-swap at a specific
  cell, in one transaction (reuses the `moveItem` swap mechanics via the off-grid temp slot).
- `SettingsRepository.addToDockAt(key, index)` — positional insert into favorites, de-duplicated.

## Testing
- Gestures aren't meaningfully unit-testable → manual verification on emulator + Pixel
  (build → install → screenshots) for the cross-surface flow.
- `placeAt` gets an instrumented Room test (like `moveItem`); `addToDockAt` a JVM test (FakeDataStore).

## Risk
The riskiest step is removing Workspace's internal floating overlay and wiring it to the shared
controller without regressing the tuned drag. Implement in phases, verify on device after each.
